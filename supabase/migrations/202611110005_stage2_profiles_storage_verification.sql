-- TUN-JOBS Stage 2: profile storage and employer verification. Run after migrations 001-004.
alter table public.job_profiles
 add column if not exists bio text not null default '',
 add column if not exists languages text not null default '',
 add column if not exists certifications text not null default '',
 add column if not exists experience_years text not null default '',
 add column if not exists cv_path text,
 add column if not exists profile_completed_at timestamptz;

-- Verification is admin-controlled; users cannot insert or update this table.
create table if not exists public.employer_verifications (
 user_id uuid primary key references auth.users(id) on delete cascade,
 is_verified boolean not null default false,
 verified_at timestamptz,
 verification_note text not null default ''
);
alter table public.employer_verifications enable row level security;
drop policy if exists "Users can view own employer verification" on public.employer_verifications;
create policy "Users can view own employer verification" on public.employer_verifications
 for select to authenticated using (user_id = auth.uid());
drop policy if exists "Authenticated users can view verified employer badges" on public.employer_verifications;
create policy "Authenticated users can view verified employer badges" on public.employer_verifications
 for select to authenticated using (is_verified = true);
grant select on public.employer_verifications to authenticated;

insert into storage.buckets (id,name,public,file_size_limit,allowed_mime_types)
values ('profile-photos','profile-photos',true,5242880,array['image/jpeg','image/png','image/webp'])
on conflict (id) do update set public=true,file_size_limit=5242880,allowed_mime_types=array['image/jpeg','image/png','image/webp'];
insert into storage.buckets (id,name,public,file_size_limit,allowed_mime_types)
values ('private-cvs','private-cvs',false,10485760,array['application/pdf','application/msword','application/vnd.openxmlformats-officedocument.wordprocessingml.document'])
on conflict (id) do update set public=false,file_size_limit=10485760,allowed_mime_types=array['application/pdf','application/msword','application/vnd.openxmlformats-officedocument.wordprocessingml.document'];

drop policy if exists "Users upload own profile photos" on storage.objects;
create policy "Users upload own profile photos" on storage.objects for insert to authenticated
 with check (bucket_id='profile-photos' and (storage.foldername(name))[1]=auth.uid()::text);
drop policy if exists "Users update own profile photos" on storage.objects;
create policy "Users update own profile photos" on storage.objects for update to authenticated
 using (bucket_id='profile-photos' and (storage.foldername(name))[1]=auth.uid()::text)
 with check (bucket_id='profile-photos' and (storage.foldername(name))[1]=auth.uid()::text);
drop policy if exists "Users delete own profile photos" on storage.objects;
create policy "Users delete own profile photos" on storage.objects for delete to authenticated
 using (bucket_id='profile-photos' and (storage.foldername(name))[1]=auth.uid()::text);

drop policy if exists "Users upload own private CVs" on storage.objects;
create policy "Users upload own private CVs" on storage.objects for insert to authenticated
 with check (bucket_id='private-cvs' and (storage.foldername(name))[1]=auth.uid()::text);
drop policy if exists "Users update own private CVs" on storage.objects;
create policy "Users update own private CVs" on storage.objects for update to authenticated
 using (bucket_id='private-cvs' and (storage.foldername(name))[1]=auth.uid()::text)
 with check (bucket_id='private-cvs' and (storage.foldername(name))[1]=auth.uid()::text);
drop policy if exists "Users delete own private CVs" on storage.objects;
create policy "Users delete own private CVs" on storage.objects for delete to authenticated
 using (bucket_id='private-cvs' and (storage.foldername(name))[1]=auth.uid()::text);
drop policy if exists "Verified matched employers can read private CVs" on storage.objects;
create policy "Verified matched employers can read private CVs" on storage.objects for select to authenticated
 using (
  bucket_id='private-cvs'
  and exists (select 1 from public.employer_verifications v where v.user_id=auth.uid() and v.is_verified=true)
  and exists (
   select 1 from public.job_matches m
   where (m.user_a=auth.uid() and m.user_b::text=(storage.foldername(name))[1])
      or (m.user_b=auth.uid() and m.user_a::text=(storage.foldername(name))[1])
  )
 );
