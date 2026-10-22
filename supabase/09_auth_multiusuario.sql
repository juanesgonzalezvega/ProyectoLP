-- Fase 9: ejecutar después de 00_schema.sql y antes de 09_auth_multiusuario_cierre.sql.
create table if not exists public.profiles (
  user_id uuid primary key default auth.uid() references auth.users(id) on delete cascade,
  display_name text not null,
  created_at timestamptz not null default now()
);

-- Si hay datos antiguos, descomentar SOLO una alternativa antes de continuar:
-- (A) Asignar los datos sin dueño a un usuario ya registrado:
-- update public.tasks set user_id = '00000000-0000-0000-0000-000000000000' where user_id is null;
-- Repetir para las demás tablas de negocio; reemplazar el UUID por auth.users.id real.
-- (B) Empezar limpio: borrar los datos existentes de las tablas listadas antes de hacer NOT NULL.

do $$ declare t text; begin
  foreach t in array array['tasks','projects','stages','habits','habit_logs','income','expenses','financial_goals','workout_routines','exercises','workout_sessions','workout_sets','credit_ledger','xp_log','user_stats','user_badges','daily_missions'] loop
    execute format('alter table public.%I add column if not exists user_id uuid default auth.uid() references auth.users(id) on delete cascade', t);
    execute format('create index if not exists %I on public.%I(user_id)', t || '_user_id_idx', t);
    execute format('alter table public.%I enable row level security', t);
    execute format('revoke all on public.%I from anon', t);
    execute format('grant select, insert, update, delete on public.%I to authenticated', t);
    execute format('drop policy if exists dev_anon_all on public.%I', t);
    execute format('drop policy if exists user_isolation on public.%I', t);
    execute format('create policy user_isolation on public.%I for all to authenticated using (user_id = (select auth.uid())) with check (user_id = (select auth.uid()))', t);
  end loop;
end $$;
-- Migración opcional A: conservar los registros heredados para una cuenta existente.
-- Descomenta este bloque tras crear columnas user_id y reemplaza UUID por auth.users.id.
-- do $$ declare t text; owner_id uuid := '00000000-0000-0000-0000-000000000000'; begin
--   foreach t in array array['tasks','projects','stages','habits','habit_logs','income','expenses','financial_goals','workout_routines','exercises','workout_sessions','workout_sets','credit_ledger','xp_log','user_stats','user_badges','daily_missions'] loop
--     execute format('update public.%I set user_id = %L where user_id is null',t,owner_id);
--   end loop;
-- end $$;
-- Migración opcional B: empezar limpio (borra los datos de todas las tablas de usuario).
-- do $$ declare t text; begin
--   foreach t in array array['workout_sets','workout_sessions','exercises','workout_routines','habit_logs','habits','stages','tasks','projects','income','expenses','financial_goals','credit_ledger','xp_log','user_badges','daily_missions','user_stats'] loop
--     execute format('delete from public.%I',t);
--   end loop;
-- end $$;
alter table public.user_stats alter column user_id set default auth.uid();

alter table public.profiles enable row level security;
revoke all on public.profiles from anon;
grant select, insert, update, delete on public.profiles to authenticated;
drop policy if exists profile_isolation on public.profiles;
create policy profile_isolation on public.profiles for all to authenticated
  using (user_id = (select auth.uid())) with check (user_id = (select auth.uid()));

-- Claves idempotentes quedan aisladas por usuario.
alter table public.credit_ledger drop constraint if exists credit_ledger_dedupe_key_key;
alter table public.credit_ledger drop constraint if exists credit_ledger_user_dedupe_key;
alter table public.credit_ledger add constraint credit_ledger_user_dedupe_key unique (user_id, dedupe_key);
alter table public.xp_log drop constraint if exists xp_log_dedupe_key_key;
alter table public.xp_log drop constraint if exists xp_log_user_dedupe_key;
alter table public.xp_log add constraint xp_log_user_dedupe_key unique (user_id, dedupe_key);
alter table public.daily_missions drop constraint if exists daily_missions_mission_date_code_key;
alter table public.daily_missions drop constraint if exists daily_missions_user_date_code_key;
alter table public.daily_missions add constraint daily_missions_user_date_code_key unique (user_id, mission_date, code);
alter table public.user_badges drop constraint if exists user_badges_pkey;
delete from public.user_badges where user_id is null;
alter table public.user_badges add constraint user_badges_pkey primary key (user_id, badge_code);
alter table public.user_stats drop constraint if exists user_stats_pkey;
alter table public.user_stats drop constraint if exists user_stats_id_check;
-- La fila global de gamificación no representa a un usuario; se recreará por usuario.
delete from public.user_stats where user_id is null;
alter table public.user_stats drop column if exists id;
alter table public.user_stats add constraint user_stats_pkey primary key (user_id);
drop index if exists public.user_stats_user_id_idx;
create index user_stats_user_id_idx on public.user_stats(user_id);

-- TEMPORALES para compatibilidad con datos antiguos; quitar al completar migración.
drop policy if exists dev_anon_all on public.connection_test;
drop table if exists public.connection_test;
revoke all on all sequences in schema public from anon;
grant usage, select on all sequences in schema public to authenticated;

create or replace function public.handle_new_auth_user()
returns trigger language plpgsql security definer set search_path = '' as $$
begin
  insert into public.profiles(user_id, display_name)
  values (new.id, coalesce(new.raw_user_meta_data ->> 'display_name', split_part(new.email, '@', 1)))
  on conflict (user_id) do nothing;
  insert into public.user_stats(user_id) values (new.id) on conflict (user_id) do nothing;
  return new;
end;
$$;
drop trigger if exists on_auth_user_created_profile on auth.users;
create trigger on_auth_user_created_profile after insert on auth.users
for each row execute function public.handle_new_auth_user();

-- Las policies dev_anon_all de todas las tablas se eliminan además de las enumeradas.
do $$ declare p record; begin
  for p in select schemaname, tablename, policyname from pg_policies where schemaname='public' and policyname ilike '%anon%' loop
    execute format('drop policy if exists %I on %I.%I', p.policyname, p.schemaname, p.tablename);
  end loop;
end $$;
