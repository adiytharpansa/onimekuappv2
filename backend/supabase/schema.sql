-- Sinkronisasi My List + progres (dipakai app/.../data/Sync.kt).
-- Jalankan di Supabase SQL editor.
create table if not exists public.user_state (
  user_id    uuid primary key references auth.users(id) on delete cascade,
  data       jsonb not null default '{}'::jsonb,
  updated_at timestamptz not null default now()
);

alter table public.user_state enable row level security;

create policy "own row: select" on public.user_state
  for select using (auth.uid() = user_id);
create policy "own row: insert" on public.user_state
  for insert with check (auth.uid() = user_id);
create policy "own row: update" on public.user_state
  for update using (auth.uid() = user_id) with check (auth.uid() = user_id);
create policy "own row: delete" on public.user_state
  for delete using (auth.uid() = user_id);

create or replace function public.touch_updated_at() returns trigger language plpgsql as $$
begin new.updated_at = now(); return new; end $$;
create trigger user_state_touch before update on public.user_state
  for each row execute function public.touch_updated_at();
