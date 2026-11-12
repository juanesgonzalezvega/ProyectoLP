-- Este SQL crea el RPC de estado; no controla el contenido del correo.
-- En Supabase Dashboard > Authentication > Email Templates > Reset Password
-- configure la plantilla para mostrar {{ .Token }} y quite {{ .ConfirmationURL }}.
-- Estado mínimo de cuenta para recuperación. Esta función permite enumerar correos; habilitarla es decisión del dueño.
create or replace function public.get_account_status(p_email text)
returns text
language plpgsql
security definer
set search_path = ''
as $$
declare
  normalized_email text := lower(trim(coalesce(p_email, '')));
begin
  if normalized_email = '' then
    return 'not_found';
  end if;

  if exists (
    select 1 from auth.users u
    where lower(trim(u.email)) = normalized_email
      and u.deleted_at is null
      and u.email_confirmed_at is not null
  ) then
    return 'verified';
  end if;

  if exists (
    select 1 from auth.users u
    where lower(trim(u.email)) = normalized_email
      and u.deleted_at is null
  ) then
    return 'unverified';
  end if;

  return 'not_found';
end;
$$;

revoke all on function public.get_account_status(text) from public;
grant execute on function public.get_account_status(text) to anon, authenticated;
comment on function public.get_account_status(text) is
  'Devuelve solo el estado de la cuenta para recuperación; su uso puede revelar si un correo está registrado.';

notify pgrst, 'reload schema';
