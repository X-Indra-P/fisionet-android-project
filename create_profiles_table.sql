-- 1. CLEANUP: Drop existing objects to ensure a clean slate (Fixes schema mismatch issues)
drop trigger if exists on_auth_user_created on auth.users;
drop function if exists public.handle_new_user();
-- WARNING: This deletes all profile data (roles/status). Auth users remain.
drop table if exists public.profiles cascade;

-- 2. CREATE TABLE
create table public.profiles (
  id uuid references auth.users not null primary key,
  display_name text,
  role smallint check (role in (1, 2)) default 2, -- 1=Admin, 2=Therapist
  status text check (status in ('pending', 'verified', 'rejected')) default 'pending',
  created_at timestamp with time zone default timezone('utc'::text, now()) not null
);

-- 3. ENABLE RLS
alter table public.profiles enable row level security;

-- 4. CREATE POLICIES
create policy "Allow read access for all authenticated users"
  on public.profiles for select
  to authenticated
  using (true);

create policy "Users can update their own profile"
  on public.profiles for update
  to authenticated
  using (auth.uid() = id);

-- 5. CREATE TRIGGER FUNCTION
create or replace function public.handle_new_user()
returns trigger
language plpgsql
security definer set search_path = public
as $$
begin
  insert into public.profiles (id, display_name, role, status)
  values (
    new.id, 
    new.raw_user_meta_data ->> 'display_name',
    2, -- Default role: 2 (Therapist)
    'pending'    -- Default status
  );
  return new;
end;
$$;

-- 6. ATTACH TRIGGER
create trigger on_auth_user_created
  after insert on auth.users
  for each row execute procedure public.handle_new_user();

-- 7. BACKFILL (Optional but recommended): Create profiles for existing users who might be missing one
insert into public.profiles (id, display_name, role, status)
select 
  id, 
  raw_user_meta_data ->> 'display_name',
  2, 
  'pending'
from auth.users
where id not in (select id from public.profiles);
