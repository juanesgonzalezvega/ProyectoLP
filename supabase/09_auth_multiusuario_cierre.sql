-- Ejecutar únicamente después de asignar o eliminar los datos heredados sin user_id.
-- Migración opcional: para conservar los datos, reemplaza el UUID por el id
-- de la cuenta que será propietaria y ejecuta este bloque ANTES del cierre.
-- Consulta los UUID con: select id, email from auth.users;
-- do $$ declare t text; owner_id uuid := '00000000-0000-0000-0000-000000000000'; begin
--   foreach t in array array['tasks','projects','stages','habits','habit_logs','income','expenses','financial_goals','workout_routines','exercises','workout_sessions','workout_sets','credit_ledger','xp_log','user_stats','user_badges','daily_missions'] loop
--     execute format('update public.%I set user_id = $1 where user_id is null',t) using owner_id;
--   end loop;
-- end $$;
-- Si prefieres empezar limpio, elimina las filas huérfanas en vez de asignarlas.
do $$ declare t text; missing_count bigint; begin
  foreach t in array array['tasks','projects','stages','habits','habit_logs','income','expenses','financial_goals','workout_routines','exercises','workout_sessions','workout_sets','credit_ledger','xp_log','user_stats','user_badges','daily_missions'] loop
    -- Los inserts de la app omiten user_id; auth.uid() debe suministrarlo.
    execute format('alter table public.%I alter column user_id set default auth.uid()',t);
    execute format('select count(*) from public.%I where user_id is null',t) into missing_count;
    if missing_count > 0 then
      raise exception 'No se puede cerrar Fase 9: la tabla % tiene % filas sin dueño. Asigna user_id a una cuenta o elimina esas filas y vuelve a ejecutar este script.',t,missing_count;
    end if;
    execute format('alter table public.%I alter column user_id set not null',t);
  end loop;
end $$;
