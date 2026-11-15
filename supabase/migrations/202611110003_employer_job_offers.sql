-- TUN-JOBS: employer-published job offers visible to authenticated job seekers.
-- Run once in Supabase Dashboard -> SQL Editor.

create table if not exists public.job_offers (
  id uuid primary key default gen_random_uuid(),
  owner_id uuid not null references auth.users(id) on delete cascade,
  title text not null check (length(trim(title)) between 2 and 120),
  company text not null check (length(trim(company)) between 2 and 120),
  governorate text not null default '',
  sector text not null default '',
  contract text not null default '',
  salary text not null default '',
  description text not null check (length(trim(description)) between 10 and 5000),
  status text not null default 'draft' check (status in ('draft','published','closed')),
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now()
);

create index if not exists job_offers_status_created_idx
  on public.job_offers(status, created_at desc);
create index if not exists job_offers_owner_created_idx
  on public.job_offers(owner_id, created_at desc);

alter table public.job_offers enable row level security;

drop policy if exists "Authenticated users view published job offers" on public.job_offers;
create policy "Authenticated users view published job offers"
  on public.job_offers for select to authenticated
  using (status = 'published' or owner_id = auth.uid());

drop policy if exists "Employers create own job offers" on public.job_offers;
create policy "Employers create own job offers"
  on public.job_offers for insert to authenticated
  with check (
    owner_id = auth.uid()
    and exists (
      select 1 from public.job_profiles p
      where p.user_id = auth.uid() and p.account_type = 'employer'
    )
  );

drop policy if exists "Employers update own job offers" on public.job_offers;
create policy "Employers update own job offers"
  on public.job_offers for update to authenticated
  using (owner_id = auth.uid())
  with check (owner_id = auth.uid());

drop policy if exists "Employers delete own job offers" on public.job_offers;
create policy "Employers delete own job offers"
  on public.job_offers for delete to authenticated
  using (owner_id = auth.uid());

grant select, insert, update, delete on public.job_offers to authenticated;
