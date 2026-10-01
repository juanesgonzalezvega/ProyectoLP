create table if not exists public.projects (
 id bigint generated always as identity primary key, created_at timestamptz not null default now(),
 name text not null, main_goal text, status text check (status in ('pendiente','en_progreso','completada')), deadline date
);
create table if not exists public.stages (
 id bigint generated always as identity primary key, created_at timestamptz not null default now(),
 project_id bigint not null references public.projects(id) on delete cascade, name text not null, position int not null default 0,
 status text not null default 'pendiente' check (status in ('pendiente','en_progreso','completada'))
);
create table if not exists public.tasks (
 id bigint generated always as identity primary key, created_at timestamptz not null default now(), name text not null,
 description text, priority text not null default 'media' check (priority in ('baja','media','alta')), due_date date,
 status text not null default 'pendiente' check (status in ('pendiente','en_progreso','completada')),
 progress int not null default 0 check (progress between 0 and 100), category text,
 project_id bigint references public.projects(id) on delete set null
);
create table if not exists public.habits (
 id bigint generated always as identity primary key, created_at timestamptz not null default now(), name text not null, description text, active boolean not null default true
);
create table if not exists public.habit_logs (
 id bigint generated always as identity primary key, created_at timestamptz not null default now(),
 habit_id bigint not null references public.habits(id) on delete cascade, log_date date not null, completed boolean not null default true,
 unique(habit_id, log_date)
);
create table if not exists public.connection_test (
 id bigint generated always as identity primary key, created_at timestamptz not null default now(), message text
);
-- Finanzas: flujo de ingresos/gastos y metas. No representa activos ni patrimonio.
create table if not exists public.income (
 id bigint generated always as identity primary key, amount numeric not null check (amount > 0),
 category text not null, description text, date date not null, created_at timestamptz not null default now()
);
create table if not exists public.expenses (
 id bigint generated always as identity primary key, amount numeric not null check (amount > 0),
 category text not null, description text, date date not null, created_at timestamptz not null default now()
);
create table if not exists public.financial_goals (
 id bigint generated always as identity primary key, name text not null,
 target_amount numeric not null check (target_amount > 0), saved_amount numeric not null default 0 check (saved_amount >= 0),
 created_at timestamptz not null default now()
);
create index if not exists income_date_idx on public.income(date);
create index if not exists expenses_date_idx on public.expenses(date);
create index if not exists stages_project_id_idx on public.stages(project_id);
create index if not exists tasks_project_id_idx on public.tasks(project_id);
create index if not exists tasks_due_date_idx on public.tasks(due_date);
create index if not exists habit_logs_habit_id_idx on public.habit_logs(habit_id);
create index if not exists habit_logs_log_date_idx on public.habit_logs(log_date);

-- TEMPORALES: políticas abiertas solo para desarrollo; endurecer al agregar autenticación y user_id.
do $$ declare t text; begin
 foreach t in array array['projects','stages','tasks','habits','habit_logs','connection_test','income','expenses','financial_goals'] loop
  execute format('alter table public.%I enable row level security', t);
  execute format('drop policy if exists dev_anon_all on public.%I', t);
  execute format('create policy dev_anon_all on public.%I for all to anon using (true) with check (true)', t);
  execute format('grant select, insert, update, delete on public.%I to anon', t);
 end loop;
end $$;
grant usage, select on all sequences in schema public to anon;
