-- Restrict private candidate data to verified employers who currently have a published offer.
-- Public candidate identity remains separate in candidate_public_profiles and candidate-photos.
create or replace function public.is_verified_employer_with_published_job()
returns boolean language sql stable security definer set search_path = public
as $$
  select exists (
    select 1 from public.employer_accounts e
    join public.jobs j on j.employer_id = e.user_id
    where e.user_id = auth.uid() and e.verified = true and j.status = 'published'
  );
$$;
revoke all on function public.is_verified_employer_with_published_job() from public;
grant execute on function public.is_verified_employer_with_published_job() to authenticated;

drop policy if exists "private details visible only to owner or verified employer" on public.candidate_private;
create policy "private details visible to owner or active verified employer"
on public.candidate_private for select to authenticated
using (user_id = auth.uid() or public.is_verified_employer_with_published_job());

drop policy if exists "candidate or verified employer reads candidate documents" on public.candidate_documents;
create policy "candidate or active verified employer reads candidate documents"
on public.candidate_documents for select to authenticated
using (user_id = auth.uid() or public.is_verified_employer_with_published_job());

drop policy if exists "candidate or verified employer reads cv" on storage.objects;
create policy "candidate or active verified employer reads cv"
on storage.objects for select to authenticated
using (bucket_id = 'candidate-cvs' and (
  (storage.foldername(name))[1] = auth.uid()::text
  or public.is_verified_employer_with_published_job()
));
