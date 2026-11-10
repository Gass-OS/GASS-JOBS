-- Public candidate identity vs verified-employer-only contact documents.
-- Apply after 20261110130000_job_matching.sql in Supabase SQL Editor.
create table if not exists public.candidate_public_profiles (
  user_id uuid primary key references auth.users(id) on delete cascade,
  full_name text not null, city text, governorate text, profession text,
  photo_path text, discoverable boolean not null default true,
  updated_at timestamptz not null default now()
);
alter table public.candidate_private add column if not exists email text;
alter table public.candidate_public_profiles enable row level security;
drop policy if exists "public candidate profiles visible to everyone signed in" on public.candidate_public_profiles;
create policy "public candidate profiles visible to everyone signed in"
on public.candidate_public_profiles for select to authenticated
using (discoverable = true or user_id = auth.uid());
drop policy if exists "candidate creates own public profile" on public.candidate_public_profiles;
create policy "candidate creates own public profile" on public.candidate_public_profiles for insert to authenticated with check (user_id = auth.uid());
drop policy if exists "candidate updates own public profile" on public.candidate_public_profiles;
create policy "candidate updates own public profile" on public.candidate_public_profiles for update to authenticated using (user_id = auth.uid()) with check (user_id = auth.uid());

create or replace function public.is_verified_employer()
returns boolean language sql stable security definer set search_path = public
as $$ select exists (select 1 from public.employer_accounts e where e.user_id = auth.uid() and e.verified = true); $$;
revoke all on function public.is_verified_employer() from public;
grant execute on function public.is_verified_employer() to authenticated;

drop policy if exists "private details visible only to owner or consented employer" on public.candidate_private;
drop policy if exists "private details visible only to owner or verified employer" on public.candidate_private;
create policy "private details visible only to owner or verified employer"
on public.candidate_private for select to authenticated using (user_id = auth.uid() or public.is_verified_employer());

drop policy if exists "candidate reads own document record" on public.candidate_documents;
drop policy if exists "candidate or verified employer reads candidate documents" on public.candidate_documents;
create policy "candidate or verified employer reads candidate documents"
on public.candidate_documents for select to authenticated using (user_id = auth.uid() or public.is_verified_employer());

-- Portraits are public after upload; only the owner may upload.
insert into storage.buckets (id, name, public) values ('candidate-photos','candidate-photos',true)
on conflict (id) do update set public = true;
drop policy if exists "candidate and applicant employer read photo" on storage.objects;
drop policy if exists "candidate uploads own photo" on storage.objects;
create policy "candidate uploads own photo" on storage.objects for insert to authenticated
with check (bucket_id = 'candidate-photos' and (storage.foldername(name))[1] = auth.uid()::text);

-- CVs remain private and readable only by the owner or verified employers.
insert into storage.buckets (id, name, public) values ('candidate-cvs','candidate-cvs',false)
on conflict (id) do update set public = false;
drop policy if exists "candidate or consented employer reads cv" on storage.objects;
drop policy if exists "candidate or verified employer reads cv" on storage.objects;
create policy "candidate or verified employer reads cv" on storage.objects for select to authenticated
using (bucket_id = 'candidate-cvs' and ((storage.foldername(name))[1] = auth.uid()::text or public.is_verified_employer()));
