-- TUN-JOBS Stage 2 privacy hardening: move phone, email and CV path out of public profile rows.
create table if not exists public.job_profile_private (
 user_id uuid primary key references auth.users(id) on delete cascade,
 phone text not null default '',
 email text not null default '',
 cv_path text,
 updated_at timestamptz not null default now()
);
insert into public.job_profile_private(user_id,phone)
select user_id,coalesce(phone,'') from public.job_profiles
on conflict(user_id) do update set phone=excluded.phone,updated_at=now();

-- Private contact/CV metadata: owner always; verified employer only after a mutual match.
alter table public.job_profile_private enable row level security;
drop policy if exists "Users manage own private profile data" on public.job_profile_private;
create policy "Users manage own private profile data" on public.job_profile_private
 for all to authenticated using (user_id=auth.uid()) with check (user_id=auth.uid());
drop policy if exists "Verified matched employers read private profile data" on public.job_profile_private;
create policy "Verified matched employers read private profile data" on public.job_profile_private
 for select to authenticated using (
  exists (select 1 from public.employer_verifications v where v.user_id=auth.uid() and v.is_verified=true)
  and exists (
   select 1 from public.job_matches m
   where (m.user_a=auth.uid() and m.user_b=user_id)
      or (m.user_b=auth.uid() and m.user_a=user_id)
  )
 );
grant select,insert,update on public.job_profile_private to authenticated;

-- Do not expose contact or CV metadata through the discoverable profile endpoint.
revoke select on public.job_profiles from authenticated;
grant select (user_id,full_name,city,profession,education,skills,availability,photo_url,account_type,is_discoverable,updated_at,bio,languages,certifications,experience_years,profile_completed_at)
 on public.job_profiles to authenticated;
