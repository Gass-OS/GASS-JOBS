-- TUN-JOBS Supabase schema — run once in Supabase SQL Editor.
-- Keep candidate CVs and contact details private. Never ship the service_role key in Android.

create table if not exists public.employer_accounts (
  user_id uuid primary key references auth.users(id) on delete cascade,
  company_name text not null,
  registration_number text,
  verified boolean not null default false,
  created_at timestamptz not null default now()
);

create table if not exists public.candidate_cards (
  user_id uuid primary key references auth.users(id) on delete cascade,
  full_name text not null,
  photo_path text,
  city text,
  governorate text,
  profession text,
  education text,
  experience text,
  skills text,
  availability text,
  discoverable boolean not null default true,
  updated_at timestamptz not null default now()
);

create table if not exists public.candidate_private (
  user_id uuid primary key references auth.users(id) on delete cascade,
  phone text,
  updated_at timestamptz not null default now()
);

create table if not exists public.candidate_documents (
  user_id uuid primary key references auth.users(id) on delete cascade,
  cv_path text,
  updated_at timestamptz not null default now()
);

create table if not exists public.jobs (
  id uuid primary key default gen_random_uuid(),
  employer_id uuid not null references auth.users(id) on delete cascade,
  title text not null,
  company_name text not null,
  description text not null,
  governorate text,
  city text,
  latitude double precision,
  longitude double precision,
  sector text,
  contract_type text,
  salary_text text,
  status text not null default 'draft' check (status in ('draft','published','closed')),
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now(),
  constraint jobs_latitude_range check (latitude is null or latitude between -90 and 90),
  constraint jobs_longitude_range check (longitude is null or longitude between -180 and 180)
);

create table if not exists public.applications (
  id uuid primary key default gen_random_uuid(),
  job_id uuid not null references public.jobs(id) on delete cascade,
  candidate_id uuid not null references auth.users(id) on delete cascade,
  status text not null default 'received' check (status in ('received','viewed','shortlisted','rejected','hired')),
  created_at timestamptz not null default now(),
  unique(job_id, candidate_id)
);

-- Candidate explicitly chooses what to share with the employer for each application.
create table if not exists public.application_consents (
  application_id uuid primary key references public.applications(id) on delete cascade,
  candidate_id uuid not null references auth.users(id) on delete cascade,
  share_phone boolean not null default false,
  share_cv boolean not null default false,
  updated_at timestamptz not null default now()
);

create index if not exists jobs_status_created_idx on public.jobs(status, created_at desc);
create index if not exists jobs_governorate_idx on public.jobs(governorate);
create index if not exists applications_candidate_idx on public.applications(candidate_id, created_at desc);
create index if not exists applications_job_idx on public.applications(job_id, created_at desc);

alter table public.employer_accounts enable row level security;
alter table public.candidate_cards enable row level security;
alter table public.candidate_private enable row level security;
alter table public.candidate_documents enable row level security;
alter table public.jobs enable row level security;
alter table public.applications enable row level security;
alter table public.application_consents enable row level security;

drop policy if exists "employer reads own account" on public.employer_accounts;
create policy "employer reads own account" on public.employer_accounts for select to authenticated using (user_id = auth.uid());
drop policy if exists "employer creates own account" on public.employer_accounts;
create policy "employer creates own account" on public.employer_accounts for insert to authenticated with check (user_id = auth.uid() and verified = false);
drop policy if exists "employer updates own unverified account" on public.employer_accounts;
create policy "employer updates own unverified account" on public.employer_accounts for update to authenticated using (user_id = auth.uid() and verified = false) with check (user_id = auth.uid() and verified = false);

drop policy if exists "candidate card visible to owner or applicant employer" on public.candidate_cards;
create policy "candidate card visible to owner or applicant employer" on public.candidate_cards for select to authenticated
using (
  user_id = auth.uid()
  or (discoverable = true and exists (
    select 1 from public.applications a join public.jobs j on j.id = a.job_id
    where a.candidate_id = candidate_cards.user_id and j.employer_id = auth.uid()
  ))
);
drop policy if exists "candidate creates own card" on public.candidate_cards;
create policy "candidate creates own card" on public.candidate_cards for insert to authenticated with check (user_id = auth.uid());
drop policy if exists "candidate updates own card" on public.candidate_cards;
create policy "candidate updates own card" on public.candidate_cards for update to authenticated using (user_id = auth.uid()) with check (user_id = auth.uid());

drop policy if exists "private details visible only to owner or consented employer" on public.candidate_private;
create policy "private details visible only to owner or consented employer" on public.candidate_private for select to authenticated
using (
  user_id = auth.uid()
  or exists (
    select 1 from public.applications a
    join public.jobs j on j.id = a.job_id
    join public.application_consents c on c.application_id = a.id
    where a.candidate_id = candidate_private.user_id
      and j.employer_id = auth.uid()
      and c.share_phone = true
  )
);
drop policy if exists "candidate creates own private details" on public.candidate_private;
create policy "candidate creates own private details" on public.candidate_private for insert to authenticated with check (user_id = auth.uid());
drop policy if exists "candidate updates own private details" on public.candidate_private;
create policy "candidate updates own private details" on public.candidate_private for update to authenticated using (user_id = auth.uid()) with check (user_id = auth.uid());

drop policy if exists "candidate reads own document record" on public.candidate_documents;
create policy "candidate reads own document record" on public.candidate_documents for select to authenticated using (user_id = auth.uid());
drop policy if exists "candidate writes own document record" on public.candidate_documents;
create policy "candidate writes own document record" on public.candidate_documents for all to authenticated using (user_id = auth.uid()) with check (user_id = auth.uid());

drop policy if exists "published jobs and own jobs readable" on public.jobs;
create policy "published jobs and own jobs readable" on public.jobs for select to authenticated using (status = 'published' or employer_id = auth.uid());
drop policy if exists "verified employers create own jobs" on public.jobs;
create policy "verified employers create own jobs" on public.jobs for insert to authenticated
with check (employer_id = auth.uid() and status = 'draft' and exists (
  select 1 from public.employer_accounts e where e.user_id = auth.uid() and e.verified = true
));
drop policy if exists "verified employers update own jobs" on public.jobs;
create policy "verified employers update own jobs" on public.jobs for update to authenticated
using (employer_id = auth.uid() and exists (select 1 from public.employer_accounts e where e.user_id = auth.uid() and e.verified = true))
with check (employer_id = auth.uid() and exists (select 1 from public.employer_accounts e where e.user_id = auth.uid() and e.verified = true));

drop policy if exists "candidate or job owner reads application" on public.applications;
create policy "candidate or job owner reads application" on public.applications for select to authenticated
using (candidate_id = auth.uid() or exists (select 1 from public.jobs j where j.id = job_id and j.employer_id = auth.uid()));
drop policy if exists "candidate applies to published job" on public.applications;
create policy "candidate applies to published job" on public.applications for insert to authenticated
with check (candidate_id = auth.uid() and exists (select 1 from public.jobs j where j.id = job_id and j.status = 'published'));
drop policy if exists "employer updates application status" on public.applications;
create policy "employer updates application status" on public.applications for update to authenticated
using (exists (select 1 from public.jobs j where j.id = job_id and j.employer_id = auth.uid()))
with check (exists (select 1 from public.jobs j where j.id = job_id and j.employer_id = auth.uid()));

drop policy if exists "candidate and related employer read consent" on public.application_consents;
create policy "candidate and related employer read consent" on public.application_consents for select to authenticated
using (
  candidate_id = auth.uid()
  or exists (select 1 from public.applications a join public.jobs j on j.id = a.job_id
    where a.id = application_id and a.candidate_id = candidate_id and j.employer_id = auth.uid())
);
drop policy if exists "candidate sets own application consent" on public.application_consents;
create policy "candidate sets own application consent" on public.application_consents for insert to authenticated
with check (candidate_id = auth.uid() and exists (
  select 1 from public.applications a where a.id = application_id and a.candidate_id = auth.uid()
));
drop policy if exists "candidate changes own application consent" on public.application_consents;
create policy "candidate changes own application consent" on public.application_consents for update to authenticated
using (candidate_id = auth.uid()) with check (candidate_id = auth.uid() and exists (select 1 from public.applications a where a.id = application_id and a.candidate_id = auth.uid()));

-- Restrict writable application fields: the candidate identity and job cannot be reassigned by a client.
revoke update on public.applications from authenticated;
grant update (status) on public.applications to authenticated;
revoke update on public.application_consents from authenticated;
grant update (share_phone, share_cv, updated_at) on public.application_consents to authenticated;

-- Private buckets: candidate photos and CVs are never publicly readable.
insert into storage.buckets (id, name, public) values ('candidate-photos','candidate-photos',false)
on conflict (id) do update set public = false;
insert into storage.buckets (id, name, public) values ('candidate-cvs','candidate-cvs',false)
on conflict (id) do update set public = false;

drop policy if exists "candidate uploads own photo" on storage.objects;
create policy "candidate uploads own photo" on storage.objects for insert to authenticated
with check (bucket_id = 'candidate-photos' and (storage.foldername(name))[1] = auth.uid()::text);
drop policy if exists "candidate and applicant employer read photo" on storage.objects;
create policy "candidate and applicant employer read photo" on storage.objects for select to authenticated
using (bucket_id = 'candidate-photos' and (
  (storage.foldername(name))[1] = auth.uid()::text
  or exists (select 1 from public.applications a join public.jobs j on j.id = a.job_id
    join public.candidate_cards cc on cc.user_id = a.candidate_id
    where a.candidate_id::text = (storage.foldername(name))[1] and j.employer_id = auth.uid() and cc.discoverable = true)
));
drop policy if exists "candidate uploads own cv" on storage.objects;
create policy "candidate uploads own cv" on storage.objects for insert to authenticated
with check (bucket_id = 'candidate-cvs' and (storage.foldername(name))[1] = auth.uid()::text);
drop policy if exists "candidate or consented employer reads cv" on storage.objects;
create policy "candidate or consented employer reads cv" on storage.objects for select to authenticated
using (bucket_id = 'candidate-cvs' and (
  (storage.foldername(name))[1] = auth.uid()::text
  or exists (
    select 1 from public.applications a
    join public.jobs j on j.id = a.job_id
    join public.application_consents c on c.application_id = a.id
    where a.candidate_id::text = (storage.foldername(name))[1]
      and j.employer_id = auth.uid() and c.share_cv = true
  )
));
