-- Fix RLS to allow Admins to update other users
-- Current policy only allows "auth.uid() = id", which blocks Admins.

-- 1. Drop existing update policy
drop policy if exists "Users can update their own profile" on public.profiles;

-- 2. Create new comprehensive update policy
create policy "Users can update own profile OR Admins can update any"
  on public.profiles for update
  to authenticated
  using (
    -- User updating themselves
    auth.uid() = id 
    OR 
    -- User is an Admin (role 1)
    (select role from public.profiles where id = auth.uid()) = 1
  );

-- 3. Verify Read Policy is correct (needed for the admin check above)
drop policy if exists "Allow read access for all authenticated users" on public.profiles;
create policy "Allow read access for all authenticated users"
  on public.profiles for select
  to authenticated
  using (true);
