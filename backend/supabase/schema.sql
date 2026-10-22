-- Backend Onimeku: sinkronisasi akun + komentar.
-- Jalankan SELURUH berkas ini di Supabase SQL editor (aman dijalankan ulang).

-- === user_state: sinkronisasi My List + progres (app/.../data/Sync.kt) ===
create table if not exists public.user_state (
  user_id    uuid primary key references auth.users(id) on delete cascade,
  data       jsonb not null default '{}'::jsonb,
  updated_at timestamptz not null default now()
);

alter table public.user_state enable row level security;

drop policy if exists "own row: select" on public.user_state;
create policy "own row: select" on public.user_state
  for select using (auth.uid() = user_id);
drop policy if exists "own row: insert" on public.user_state;
create policy "own row: insert" on public.user_state
  for insert with check (auth.uid() = user_id);
drop policy if exists "own row: update" on public.user_state;
create policy "own row: update" on public.user_state
  for update using (auth.uid() = user_id) with check (auth.uid() = user_id);
drop policy if exists "own row: delete" on public.user_state;
create policy "own row: delete" on public.user_state
  for delete using (auth.uid() = user_id);

create or replace function public.touch_updated_at() returns trigger language plpgsql as $$
begin new.updated_at = now(); return new; end $$;
drop trigger if exists user_state_touch on public.user_state;
create trigger user_state_touch before update on public.user_state
  for each row execute function public.touch_updated_at();

-- === comments: komentar per judul di halaman Detail (debug.31) ===
create table if not exists public.comments (
  id         bigint generated always as identity primary key,
  anime_id   text not null,
  user_id    uuid not null references auth.users(id) on delete cascade,
  author     text not null default '',
  body       text not null check (char_length(body) between 1 and 1000),
  created_at timestamptz not null default now()
);
create index if not exists comments_anime_idx on public.comments (anime_id, created_at desc);

alter table public.comments enable row level security;

drop policy if exists "comments: read all" on public.comments;
create policy "comments: read all" on public.comments
  for select using (true);
drop policy if exists "comments: insert own" on public.comments;
create policy "comments: insert own" on public.comments
  for insert with check (auth.uid() = user_id);
drop policy if exists "comments: delete own" on public.comments;
create policy "comments: delete own" on public.comments
  for delete using (auth.uid() = user_id);

-- Nama penulis ditetapkan server dari metadata akun; klien tidak bisa memalsukannya.
-- Bila akun tak punya nama, pakai nilai kiriman klien yang sudah dibersihkan (tanpa email).
create or replace function public.comments_set_author() returns trigger
language plpgsql security definer set search_path = public, auth as $$
declare nm text;
begin
  select coalesce(nullif(trim(raw_user_meta_data->>'full_name'), ''),
                  nullif(trim(raw_user_meta_data->>'name'), ''))
    into nm from auth.users where id = new.user_id;
  new.author := left(coalesce(nm, nullif(trim(new.author), ''), 'User'), 40);
  return new;
end $$;

drop trigger if exists comments_author on public.comments;
create trigger comments_author before insert on public.comments
  for each row execute function public.comments_set_author();
