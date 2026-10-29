    -- Metadatos de archivos privados asociados a una tarea, proyecto o sesión.
create table if not exists public.attachments (
    id bigint generated always as identity primary key,
    user_id uuid not null default auth.uid() references auth.users(id) on delete cascade,
    task_id bigint references public.tasks(id) on delete cascade,
    project_id bigint references public.projects(id) on delete cascade,
    session_id bigint references public.workout_sessions(id) on delete cascade,
    file_name text not null,
    mime_type text not null,
    kind text not null check (kind in ('imagen', 'audio', 'video')),
    size_bytes bigint not null check (size_bytes > 0 and size_bytes <= 52428800),
    storage_path text not null unique,
    created_at timestamptz not null default now(),
    constraint attachments_exactly_one_owner check (num_nonnulls(task_id, project_id, session_id) = 1)
);

create index if not exists attachments_user_task_idx on public.attachments(user_id, task_id);
create index if not exists attachments_user_project_idx on public.attachments(user_id, project_id);
create index if not exists attachments_user_session_idx on public.attachments(user_id, session_id);

alter table public.attachments enable row level security;
drop policy if exists attachments_user_isolation on public.attachments;
create policy attachments_user_isolation on public.attachments
    for all to authenticated
    using (user_id = (select auth.uid()))
    with check (user_id = (select auth.uid()));

-- Bucket privado; el cliente aplica límites por tipo más estrictos.
insert into storage.buckets (id, name, public, file_size_limit, allowed_mime_types)
values ('attachments', 'attachments', false, 52428800, null)
on conflict (id) do update set public = false, file_size_limit = 52428800, allowed_mime_types = null;

drop policy if exists attachments_storage_insert_own on storage.objects;
create policy attachments_storage_insert_own on storage.objects
    for insert to authenticated
    with check (bucket_id = 'attachments' and (storage.foldername(name))[1] = (select auth.uid())::text);

drop policy if exists attachments_storage_select_own on storage.objects;
create policy attachments_storage_select_own on storage.objects
    for select to authenticated
    using (bucket_id = 'attachments' and (storage.foldername(name))[1] = (select auth.uid())::text);

drop policy if exists attachments_storage_delete_own on storage.objects;
create policy attachments_storage_delete_own on storage.objects
    for delete to authenticated
    using (bucket_id = 'attachments' and (storage.foldername(name))[1] = (select auth.uid())::text);

-- No se concede UPDATE ni acceso al rol anon. Verifica el aislamiento iniciando sesión
-- con dos cuentas: la segunda no debe listar metadatos ni descargar rutas de la primera.
