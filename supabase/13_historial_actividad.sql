-- Historial de actividad por usuario. Ejecutar después de 09_auth_multiusuario.sql
-- y después de instalar los scripts opcionales de adjuntos y canje.
-- El historial no se completa retroactivamente.

create table if not exists public.activity_log (
    id bigint generated always as identity primary key,
    user_id uuid not null references auth.users(id) on delete cascade,
    occurred_at timestamptz not null default now(),
    category text not null check (category in (
        'tarea', 'proyecto', 'habito', 'finanzas', 'entrenamiento',
        'creditos', 'canje', 'insignia', 'mision', 'adjunto', 'cuenta'
    )),
    action text not null,
    entity_type text,
    entity_id text,
    summary text not null,
    metadata jsonb not null default '{}'::jsonb,
    source text not null default 'trigger' check (source in ('trigger', 'app'))
);

create index if not exists activity_log_user_occurred_idx
    on public.activity_log(user_id, occurred_at desc, id desc);
create index if not exists activity_log_user_category_occurred_idx
    on public.activity_log(user_id, category, occurred_at desc);

alter table public.activity_log enable row level security;
drop policy if exists activity_log_read_own on public.activity_log;
drop policy if exists activity_log_user_isolation on public.activity_log;
do $$
declare
    v_policy record;
begin
    for v_policy in
        select policyname from pg_catalog.pg_policies
        where schemaname = 'public' and tablename = 'activity_log'
    loop
        execute pg_catalog.format('drop policy if exists %I on public.activity_log', v_policy.policyname);
    end loop;
end;
$$;
create policy activity_log_read_own on public.activity_log
    for select to authenticated using (user_id = (select auth.uid()));

revoke all on table public.activity_log from public, anon, authenticated;
grant select on table public.activity_log to authenticated;
revoke all on sequence public.activity_log_id_seq from public, anon, authenticated;
create or replace function public.activity_sanitize_metadata(p_value jsonb)
returns jsonb
language plpgsql
immutable
set search_path = ''
as $$
declare
    v_result jsonb;
    v_item record;
begin
    if p_value is null then
        return '{}'::jsonb;
    end if;
    case pg_catalog.jsonb_typeof(p_value)
        when 'object' then
            v_result := '{}'::jsonb;
            for v_item in select key, value from pg_catalog.jsonb_each(p_value)
            loop
                v_result := v_result || pg_catalog.jsonb_build_object(
                    v_item.key,
                    public.activity_sanitize_metadata(v_item.value)
                );
            end loop;
            return v_result;
        when 'array' then
            select coalesce(pg_catalog.jsonb_agg(public.activity_sanitize_metadata(value)), '[]'::jsonb)
                into v_result
                from pg_catalog.jsonb_array_elements(p_value);
            return v_result;
        when 'string' then
            return pg_catalog.to_jsonb(pg_catalog.left(p_value #>> '{}', 200));
        else
            return p_value;
    end case;
end;
$$;

revoke all on function public.activity_sanitize_metadata(jsonb) from public, anon, authenticated;

create or replace function public.activity_insert(
    p_user_id uuid,
    p_category text,
    p_action text,
    p_entity_type text,
    p_entity_id text,
    p_summary text,
    p_metadata jsonb default '{}'::jsonb
)
returns void
language plpgsql
security definer
set search_path = ''
as $$
begin
    if p_user_id is null or p_summary is null or pg_catalog.btrim(p_summary) = '' then
        return;
    end if;
    insert into public.activity_log(
        user_id, category, action, entity_type, entity_id, summary, metadata, source
    ) values (
        p_user_id, p_category, p_action, p_entity_type, p_entity_id,
        pg_catalog.left(p_summary, 1000),
        public.activity_sanitize_metadata(coalesce(p_metadata, '{}'::jsonb)),
        'trigger'
    );
end;
$$;

revoke all on function public.activity_insert(uuid, text, text, text, text, text, jsonb)
    from public, anon, authenticated;

create or replace function public.activity_money(p_amount numeric)
returns text
language sql
immutable
set search_path = ''
as $$
    select pg_catalog.replace(
        pg_catalog.to_char(pg_catalog.trunc(coalesce(p_amount, 0)), 'FM999,999,999,999,999,990'),
        ',', '.'
    );
$$;

create or replace function public.activity_capture_change()
returns trigger
language plpgsql
security definer
set search_path = ''
as $$
declare
    v_old jsonb := case when tg_op = 'INSERT' then '{}'::jsonb else pg_catalog.to_jsonb(old) end;
    v_new jsonb := case when tg_op = 'DELETE' then '{}'::jsonb else pg_catalog.to_jsonb(new) end;
    v_row jsonb;
    v_user_id uuid;
    v_id text;
    v_name text;
    v_parent text;
    v_category text;
    v_entity text;
    v_action text;
    v_summary text;
    v_fields text;
    v_changed jsonb := '{}'::jsonb;
    v_key text;
    v_amount numeric;
    v_stage_count integer := 0;
    v_before numeric;
    v_after numeric;
    v_target numeric;
    v_completed boolean := false;
    v_divisor text;
    v_badge text;
begin
    if tg_table_name = 'projects' and tg_op = 'DELETE' and tg_when = 'AFTER' then
        return old;
    end if;

    -- Las filas eliminadas por FK no generan entradas adicionales.
    if pg_catalog.pg_trigger_depth() > 1 then
        if tg_op = 'DELETE' then return old; else return new; end if;
    end if;

    if tg_op = 'UPDATE' then
        if v_old = v_new then return new; end if;
        for v_key in
            select keys.key
            from (
                select jsonb_object_keys(v_old) as key
                union
                select jsonb_object_keys(v_new) as key
            ) as keys
            where keys.key <> 'created_at'
              and v_old -> keys.key is distinct from v_new -> keys.key
            order by keys.key
        loop
            v_changed := v_changed || pg_catalog.jsonb_build_object(
                v_key, pg_catalog.jsonb_build_object('antes', v_old -> v_key, 'despues', v_new -> v_key)
            );
        end loop;
        if v_changed = '{}'::jsonb then return new; end if;
        select pg_catalog.string_agg(key, ', ' order by key)
            into v_fields from pg_catalog.jsonb_object_keys(v_changed) as key;
    end if;

    v_row := case when tg_op = 'DELETE' then v_old else v_new end;
    v_user_id := (v_row ->> 'user_id')::uuid;
    v_id := v_row ->> 'id';
    v_name := pg_catalog.left(coalesce(v_row ->> 'name', ''), 80);

    if tg_table_name = 'tasks' then
        v_category := 'tarea'; v_entity := 'tarea';
        if tg_op = 'INSERT' then v_action := 'crear'; v_summary := 'Creó la tarea «' || v_name || '»';
        elsif tg_op = 'DELETE' then v_action := 'eliminar'; v_summary := 'Eliminó la tarea «' || v_name || '»';
        elsif v_old ->> 'status' is distinct from v_new ->> 'status'
              and v_new ->> 'status' = 'completada' then
            v_action := 'completar'; v_summary := 'Completó la tarea «' || v_name || '»';
        elsif v_old ->> 'status' = 'completada'
              and v_new ->> 'status' is distinct from 'completada' then
            v_action := 'reabrir'; v_summary := 'Reabrió la tarea «' || v_name || '»';
        else
            v_action := 'editar'; v_summary := 'Editó la tarea «' || v_name || '» (campos: ' || v_fields || ')';
        end if;

    elsif tg_table_name = 'projects' then
        v_category := 'proyecto'; v_entity := 'proyecto';
        if tg_op = 'INSERT' then
            v_action := 'crear'; v_summary := 'Creó el proyecto «' || v_name || '»';
            select pg_catalog.count(*) into v_stage_count from public.stages where project_id = (v_new ->> 'id')::bigint;
        elsif tg_op = 'DELETE' then
            v_action := 'eliminar'; v_summary := 'Eliminó el proyecto «' || v_name || '»';
            select pg_catalog.count(*) into v_stage_count from public.stages where project_id = (v_old ->> 'id')::bigint;
        else
            v_action := 'editar'; v_summary := 'Editó el proyecto «' || v_name || '» (campos: ' || v_fields || ')';
            select pg_catalog.count(*) into v_stage_count from public.stages where project_id = (v_new ->> 'id')::bigint;
        end if;

    elsif tg_table_name = 'stages' then
        v_category := 'proyecto'; v_entity := 'etapa';
        select pg_catalog.left(name, 80) into v_parent
            from public.projects where id = (v_row ->> 'project_id')::bigint;
        v_parent := coalesce(v_parent, 'proyecto');
        if tg_op = 'INSERT' then
            v_action := 'crear'; v_summary := 'Creó la etapa «' || v_name || '» del proyecto «' || v_parent || '»';
        elsif tg_op = 'DELETE' then
            v_action := 'eliminar'; v_summary := 'Eliminó la etapa «' || v_name || '» del proyecto «' || v_parent || '»';
        elsif v_old ->> 'status' is distinct from v_new ->> 'status' then
            v_action := 'cambiar_estado';
            v_summary := 'La etapa «' || v_name || '» del proyecto «' || v_parent || '» pasó de '
                || coalesce(v_old ->> 'status', 'sin estado') || ' a ' || coalesce(v_new ->> 'status', 'sin estado');
        else
            v_action := 'editar'; v_summary := 'Editó la etapa «' || v_name || '» del proyecto «' || v_parent
                || '» (campos: ' || v_fields || ')';
        end if;

    elsif tg_table_name = 'habits' then
        v_category := 'habito'; v_entity := 'habito';
        if tg_op = 'INSERT' then v_action := 'crear'; v_summary := 'Creó el hábito «' || v_name || '»';
        elsif tg_op = 'DELETE' then v_action := 'eliminar'; v_summary := 'Eliminó el hábito «' || v_name || '»';
        elsif v_old ->> 'active' is distinct from v_new ->> 'active' then
            if coalesce((v_new ->> 'active')::boolean, false) then
                v_action := 'activar'; v_summary := 'Activó el hábito «' || v_name || '»';
            else
                v_action := 'desactivar'; v_summary := 'Desactivó el hábito «' || v_name || '»';
            end if;
        else
            v_action := 'editar'; v_summary := 'Editó el hábito «' || v_name || '» (campos: ' || v_fields || ')';
        end if;

    elsif tg_table_name = 'habit_logs' then
        v_category := 'habito'; v_entity := 'cumplimiento';
        select pg_catalog.left(name, 80) into v_name from public.habits where id = (v_row ->> 'habit_id')::bigint;
        v_name := coalesce(v_name, 'hábito');
        if tg_op = 'DELETE' or coalesce((v_row ->> 'completed')::boolean, false) = false then
            v_action := 'desmarcar';
            v_summary := 'Desmarcó el hábito «' || v_name || '» (' || coalesce(v_row ->> 'log_date', '') || ')';
        else
            v_action := 'marcar';
            v_summary := 'Marcó como cumplido el hábito «' || v_name || '» (' || coalesce(v_row ->> 'log_date', '') || ')';
        end if;

    elsif tg_table_name in ('income', 'expenses') then
        v_category := 'finanzas'; v_entity := case when tg_table_name = 'income' then 'ingreso' else 'gasto' end;
        v_amount := coalesce((v_row ->> 'amount')::numeric, 0);
        if tg_op = 'INSERT' then
            v_action := 'crear';
            v_summary := 'Registró un ' || v_entity || ' de $' || public.activity_money(v_amount)
                || ' (' || pg_catalog.left(coalesce(v_row ->> 'category', 'sin categoría'), 80) || ')';
        elsif tg_op = 'DELETE' then
            v_action := 'eliminar';
            v_summary := 'Eliminó un ' || v_entity || ' de $' || public.activity_money(v_amount)
                || ' (' || pg_catalog.left(coalesce(v_row ->> 'category', 'sin categoría'), 80) || ')';
        else
            v_action := 'editar';
            v_summary := 'Editó un ' || v_entity || ' de $' || public.activity_money(v_amount)
                || ' (' || pg_catalog.left(coalesce(v_row ->> 'category', 'sin categoría'), 80)
                || '; campos: ' || v_fields || ')';
        end if;

    elsif tg_table_name = 'financial_goals' then
        v_category := 'finanzas'; v_entity := 'meta';
        if tg_op = 'INSERT' then
            v_action := 'crear'; v_summary := 'Creó la meta «' || v_name || '»';
        elsif tg_op = 'DELETE' then
            v_action := 'eliminar'; v_summary := 'Eliminó la meta «' || v_name || '»';
        else
            v_before := coalesce((v_old ->> 'saved_amount')::numeric, 0);
            v_after := coalesce((v_new ->> 'saved_amount')::numeric, 0);
            v_target := coalesce((v_new ->> 'target_amount')::numeric, 0);
            v_completed := v_before < coalesce((v_old ->> 'target_amount')::numeric, 0)
                    and v_after >= v_target;
            if v_after > v_before then
                perform public.activity_insert(
                    v_user_id, 'finanzas', 'aportar', 'meta', v_id,
                    'Aportó $' || public.activity_money(v_after - v_before) || ' a la meta «' || v_name || '»',
                    pg_catalog.jsonb_build_object('antes', v_before, 'despues', v_after, 'cambios', v_changed)
                );
            end if;
            if v_completed then
                perform public.activity_insert(v_user_id, 'finanzas', 'completar', 'meta', v_id,
                    'Completó la meta «' || v_name || '»', pg_catalog.jsonb_build_object('objetivo', v_target));
            end if;
            if v_after > v_before or v_completed then
                return new;
            end if;
            v_action := 'editar'; v_summary := 'Editó la meta «' || v_name || '» (campos: ' || v_fields || ')';
        end if;

    elsif tg_table_name = 'workout_routines' then
        v_category := 'entrenamiento'; v_entity := 'rutina';
        if tg_op = 'INSERT' then v_action := 'crear'; v_summary := 'Creó la rutina «' || v_name || '»';
        elsif tg_op = 'DELETE' then v_action := 'eliminar'; v_summary := 'Eliminó la rutina «' || v_name || '»';
        else v_action := 'editar'; v_summary := 'Editó la rutina «' || v_name || '» (campos: ' || v_fields || ')';
        end if;

    elsif tg_table_name = 'workout_sessions' then
        if tg_op <> 'INSERT' then return case when tg_op = 'DELETE' then old else new end; end if;
        v_category := 'entrenamiento'; v_entity := 'sesion'; v_action := 'iniciar';
        select pg_catalog.left(name, 80) into v_parent
            from public.workout_routines where id = nullif(v_new ->> 'routine_id', '')::bigint;
        if v_parent is null then
            v_summary := 'Inició una sesión de entrenamiento (sesión libre)';
        else
            v_summary := 'Inició una sesión de entrenamiento (rutina «' || v_parent || '»)';
        end if;

    elsif tg_table_name = 'workout_sets' then
        v_category := 'entrenamiento'; v_entity := 'serie';
        if tg_op = 'INSERT' then
            v_action := 'registrar';
            v_summary := 'Registró la serie ' || coalesce(v_row ->> 'set_number', '?') || ' de «'
                || pg_catalog.left(coalesce(v_row ->> 'exercise_name', 'ejercicio'), 80) || '» ('
                || coalesce(v_row ->> 'weight', '0') || ' × ' || coalesce(v_row ->> 'reps', '0') || ')';
        elsif tg_op = 'DELETE' then
            v_action := 'eliminar';
            v_summary := 'Eliminó la serie ' || coalesce(v_row ->> 'set_number', '?') || ' de «'
                || pg_catalog.left(coalesce(v_row ->> 'exercise_name', 'ejercicio'), 80) || '»';
        else
            v_action := 'editar';
            v_summary := 'Editó la serie ' || coalesce(v_row ->> 'set_number', '?') || ' de «'
                || pg_catalog.left(coalesce(v_row ->> 'exercise_name', 'ejercicio'), 80) || '» (campos: ' || v_fields || ')';
        end if;

    elsif tg_table_name = 'credit_ledger' then
        if tg_op <> 'INSERT' then return case when tg_op = 'DELETE' then old else new end; end if;
        if v_new ->> 'entry_type' = 'redencion' then return new; end if;
        v_amount := coalesce((v_new ->> 'credits')::numeric, 0);
        if v_new ->> 'entry_type' = 'penalizacion' then
            v_action := 'perder'; v_category := 'creditos'; v_entity := 'credito';
            v_summary := 'Perdió ' || pg_catalog.abs(v_amount)::text || ' créditos por racha perdida';
            v_divisor := pg_catalog.substring(coalesce(v_new ->> 'description', ''), 'divididos por ([0-9]+)');
            v_changed := case when v_divisor is null then '{}'::jsonb else pg_catalog.jsonb_build_object('divisor', v_divisor::integer) end;
        elsif v_amount > 0 then
            v_action := 'ganar'; v_category := 'creditos'; v_entity := 'credito';
            v_summary := 'Ganó ' || v_amount::text || case when v_amount = 1 then ' crédito: ' else ' créditos: ' end
                || pg_catalog.left(coalesce(v_new ->> 'description', 'actividad'), 200);
        else
            return new;
        end if;

    elsif tg_table_name = 'redemptions' then
        if tg_op <> 'INSERT' then return case when tg_op = 'DELETE' then old else new end; end if;
        v_category := 'canje'; v_entity := 'canje'; v_action := 'canjear';
        v_summary := 'Canjeó ' || coalesce(v_new ->> 'credits_spent', '0') || ' créditos por «'
            || pg_catalog.left(coalesce(v_new ->> 'product_name', 'producto'), 80) || '» en «'
            || pg_catalog.left(coalesce(v_new ->> 'merchant_name', 'restaurante'), 80) || '» (cupón '
            || coalesce(v_new ->> 'coupon_code', '') || ')';

    elsif tg_table_name = 'user_badges' then
        if tg_op <> 'INSERT' then return case when tg_op = 'DELETE' then old else new end; end if;
        v_category := 'insignia'; v_entity := 'insignia'; v_action := 'desbloquear';
        v_badge := case v_new ->> 'badge_code'
            when 'PRIMERA_TAREA' then 'Primera tarea'
            when 'PRIMER_HABITO' then 'Primer hábito'
            when 'RACHA_7' then 'Racha de 7 días'
            when 'RACHA_30' then 'Racha de 30 días'
            when 'NIVEL_5' then 'Nivel 5'
            when 'PRIMER_PROYECTO' then 'Primer proyecto'
            when 'PRIMERA_META' then 'Primera meta'
            when 'CREDITOS_50' then '50 créditos'
            else coalesce(v_new ->> 'badge_code', 'insignia')
        end;
        v_name := pg_catalog.left(v_badge, 80);
        v_id := v_new ->> 'badge_code';
        v_summary := 'Desbloqueó la insignia «' || v_name || '»';

    elsif tg_table_name = 'daily_missions' then
        if tg_op = 'DELETE' or coalesce((v_new ->> 'completed')::boolean, false) = false then
            return case when tg_op = 'DELETE' then old else new end;
        end if;
        if tg_op = 'UPDATE' and coalesce((v_old ->> 'completed')::boolean, false) then return new; end if;
        v_category := 'mision'; v_entity := 'mision'; v_action := 'completar';
        v_name := pg_catalog.left(coalesce(v_new ->> 'description', ''), 80);
        v_summary := 'Completó la misión «' || v_name || '»';

    elsif tg_table_name = 'attachments' then
        v_category := 'adjunto'; v_entity := 'adjunto';
        v_parent := null; v_name := pg_catalog.left(coalesce(v_row ->> 'file_name', 'archivo'), 80);
        if v_row ->> 'task_id' is not null then
            v_entity := 'tarea';
            select pg_catalog.left(name, 80) into v_parent from public.tasks where id = (v_row ->> 'task_id')::bigint;
        elsif v_row ->> 'project_id' is not null then
            v_entity := 'proyecto';
            select pg_catalog.left(name, 80) into v_parent from public.projects where id = (v_row ->> 'project_id')::bigint;
        elsif v_row ->> 'session_id' is not null then
            v_entity := 'sesion';
            select pg_catalog.left(coalesce(r.name, 'entrenamiento'), 80) into v_parent
                from public.workout_sessions s left join public.workout_routines r on r.id = s.routine_id
                where s.id = (v_row ->> 'session_id')::bigint;
        end if;
        v_parent := coalesce(v_parent, 'elemento');
        if tg_op = 'DELETE' then
            v_action := 'eliminar';
            v_summary := 'Eliminó el adjunto «' || v_name || '» de ' || v_entity || ' «' || v_parent || '»';
        elsif tg_op = 'INSERT' then
            v_action := 'adjuntar';
            v_summary := 'Adjuntó «' || v_name || '» (' || coalesce(v_row ->> 'kind', 'archivo')
                || ') a ' || v_entity || ' «' || v_parent || '»';
        else
            v_action := 'editar'; v_summary := 'Editó el adjunto «' || v_name || '»';
        end if;

    else
        return case when tg_op = 'DELETE' then old else new end;
    end if;

    if v_user_id is not null then
        if tg_table_name = 'projects' then
            perform public.activity_insert(v_user_id, v_category, v_action, v_entity, v_id, v_summary,
                pg_catalog.jsonb_build_object('stage_count', v_stage_count, 'cambios', v_changed));
        else
            perform public.activity_insert(v_user_id, v_category, v_action, v_entity, v_id, v_summary, v_changed);
        end if;
    end if;
    if tg_op = 'DELETE' then return old; else return new; end if;
end;
$$;

revoke all on function public.activity_capture_change() from public, anon, authenticated;
revoke all on function public.activity_money(numeric) from public, anon, authenticated;

-- Solo instala triggers para tablas presentes. Los catálogos y datos derivados se excluyen.
do $$
declare
    v_table text;
    v_tables text[] := array[
        'tasks', 'projects', 'stages', 'habits', 'habit_logs', 'income', 'expenses',
        'financial_goals', 'workout_routines', 'workout_sessions', 'workout_sets',
        'credit_ledger', 'redemptions', 'user_badges', 'daily_missions', 'attachments'
    ];
begin
    foreach v_table in array v_tables loop
        if pg_catalog.to_regclass('public.' || v_table) is null then
            raise notice 'Historial: se omite public.% (tabla inexistente).', v_table;
        else
            execute pg_catalog.format('drop trigger if exists activity_log_change on public.%I', v_table);
            execute pg_catalog.format(
                'create trigger activity_log_change after insert or update or delete on public.%I '
                'for each row execute function public.activity_capture_change()', v_table
            );
            if v_table = 'projects' then
                execute 'drop trigger if exists activity_log_project_delete on public.projects';
                execute 'create trigger activity_log_project_delete before delete on public.projects '
                    'for each row execute function public.activity_capture_change()';
            end if;
        end if;
    end loop;
end;
$$;

create or replace function public.log_client_event(p_action text, p_metadata jsonb default '{}'::jsonb)
returns void
language plpgsql
security definer
set search_path = ''
as $$
declare
    v_user_id uuid := auth.uid();
    v_category text := 'cuenta';
    v_summary text;
    v_metadata jsonb := '{}'::jsonb;
    v_sets numeric;
    v_volume numeric;
begin
    if v_user_id is null then
        raise exception using errcode = '28000', message = 'Se requiere una sesión activa.';
    end if;
    case p_action
        when 'inicio_sesion' then v_summary := 'Inició sesión';
        when 'cierre_sesion' then v_summary := 'Cerró sesión';
        when 'contrasena_actualizada' then v_summary := 'Actualizó su contraseña';
        when 'entrenamiento_finalizado' then
            v_category := 'entrenamiento';
            v_summary := 'Finalizó una sesión de entrenamiento';
            if pg_catalog.jsonb_typeof(p_metadata) = 'object' then
                if (p_metadata ->> 'series') ~ '^[0-9]+$' then
                    v_sets := (p_metadata ->> 'series')::numeric;
                    if v_sets between 0 and 1000 then v_metadata := v_metadata || pg_catalog.jsonb_build_object('series', v_sets); end if;
                end if;
                if (p_metadata ->> 'volumen') ~ '^[0-9]+([.][0-9]+)?$' then
                    v_volume := (p_metadata ->> 'volumen')::numeric;
                    if v_volume between 0 and 1000000000 then v_metadata := v_metadata || pg_catalog.jsonb_build_object('volumen', v_volume); end if;
                end if;
            end if;
        else
            raise exception using errcode = '22023', message = 'Acción de historial no permitida.';
    end case;
    perform public.activity_insert(v_user_id, v_category, p_action, 'evento', null, v_summary, v_metadata);
    update public.activity_log set source = 'app'
        where id = pg_catalog.currval('public.activity_log_id_seq');
end;
$$;

revoke all on function public.log_client_event(text, jsonb) from public, anon;
revoke all on function public.log_client_event(text, jsonb) from authenticated;
grant execute on function public.log_client_event(text, jsonb) to authenticated;

comment on table public.activity_log is
    'Historial inmutable de actividad propia; triggers y log_client_event son los únicos escritores.';
comment on function public.log_client_event(text, jsonb) is
    'Registra solo los eventos de cuenta permitidos; los resúmenes se generan en la base de datos.';

-- Verificación manual:
-- select category, count(*) from public.activity_log where user_id = auth.uid() group by category order by category;
-- select occurred_at, category, summary from public.activity_log where user_id = auth.uid()
--   order by occurred_at desc, id desc limit 20;
notify pgrst, 'reload schema';
