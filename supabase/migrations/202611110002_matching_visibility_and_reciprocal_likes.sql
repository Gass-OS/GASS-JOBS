-- TUN-JOBS: allow limited profile cards for incoming likes and matched users.
-- Run in Supabase SQL Editor after 202611110001_secure_matching_messaging.sql.
drop policy if exists "Authenticated users view opted-in profiles" on public.job_profiles;
create policy "Authenticated users view opted-in profiles" on public.job_profiles
for select to authenticated
using (
  user_id = auth.uid()
  or is_discoverable = true
  or exists (
    select 1 from public.job_swipes s
    where s.actor_id = user_id and s.target_id = auth.uid() and s.decision = 'like'
  )
  or exists (
    select 1 from public.job_matches m
    where (m.user_a = auth.uid() and m.user_b = user_id)
       or (m.user_b = auth.uid() and m.user_a = user_id)
  )
);

-- A reciprocal candidate like must be able to complete a match even though
-- the employer's own profile is not publicly discoverable.
create or replace function public.create_job_match_after_like()
returns trigger
language plpgsql
security definer
set search_path = public
as $$
declare
  first_user uuid;
  second_user uuid;
  new_match_id uuid;
  eligible_pair boolean;
begin
  if new.decision <> 'like' then return new; end if;

  select exists (
    select 1 from public.job_profiles p
    where p.user_id in (new.actor_id, new.target_id)
      and p.account_type = 'job_seeker'
      and p.is_discoverable = true
  ) into eligible_pair;

  if not eligible_pair then return new; end if;

  if exists (
    select 1 from public.job_swipes s
    where s.actor_id = new.target_id
      and s.target_id = new.actor_id
      and s.decision = 'like'
  ) then
    first_user := least(new.actor_id, new.target_id);
    second_user := greatest(new.actor_id, new.target_id);
    insert into public.job_matches(user_a,user_b)
    values(first_user,second_user)
    on conflict(user_a,user_b) do nothing;
    select id into new_match_id from public.job_matches
    where user_a=first_user and user_b=second_user;
    insert into public.job_conversations(match_id,user_a,user_b)
    values(new_match_id,first_user,second_user)
    on conflict(match_id) do nothing;
  end if;
  return new;
end;
$$;
