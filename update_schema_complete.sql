-- Ensure profiles table exists
create table if not exists public.profiles (
  id uuid references auth.users not null primary key,
  display_name text,
  role smallint check (role in (1, 2)) default 2, -- 1=Admin, 2=Therapist
  status text check (status in ('pending', 'verified', 'rejected')) default 'pending',
  created_at timestamp with time zone default timezone('utc'::text, now()) not null
);

-- Add clinic column if it doesn't exist
alter table public.profiles 
add column if not exists clinic text;

-- Enable RLS just in case
alter table public.profiles enable row level security;

-- Re-apply policies (drop first to avoid error)
drop policy if exists "Allow read access for all authenticated users" on public.profiles;
create policy "Allow read access for all authenticated users"
  on public.profiles for select
  to authenticated
  using (true);

drop policy if exists "Users can update their own profile" on public.profiles;
create policy "Users can update their own profile"
  on public.profiles for update
  to authenticated
  using (auth.uid() = id);
