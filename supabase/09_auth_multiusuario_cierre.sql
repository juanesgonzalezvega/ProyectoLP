-- Ejecutar únicamente después de asignar o eliminar los datos heredados sin user_id.
do $$ declare t text; missing_count bigint; begin
  foreach t in array array['tasks','projects','stages','habits','habit_logs','income','expenses','financial_goals','workout_routines','exercises','workout_sessions','workout_sets','credit_ledger','xp_log','user_stats','user_badges','daily_missions'] loop
    execute format('select count(*) from public.%I where user_id is null',t) into missing_count;
    if missing_count > 0 then
      raise exception 'No se puede cerrar Fase 9: la tabla % tiene % filas sin dueño. Asigna user_id a una cuenta o elimina esas filas y vuelve a ejecutar este script.',t,missing_count;
    end if;
    execute format('alter table public.%I alter column user_id set not null',t);
  end loop;
end $$;
