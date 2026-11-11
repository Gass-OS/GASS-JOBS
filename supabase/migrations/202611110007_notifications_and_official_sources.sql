-- TUN-JOBS notification inbox and event notifications.
-- Apply once in Supabase SQL Editor after migrations 001-006.

create table if not exists public.job_notifications (
  id uuid primary key default gen_random_uuid(),
  user_id uuid not null references auth.users(id) on delete cascade,
  category text not null check (category in ('messages','jobs','news')),
  title text not null,
  body text not null default '',
  action_url text,
  source_id uuid,
  created_at timestamptz not null default now(),
  read_at timestamptz
);
create index if not exists job_notifications_user_created_idx
  on public.job_notifications(user_id, created_at desc);
create index if not exists job_notifications_unread_idx
  on public.job_notifications(user_id) where read_at is null;
alter table public.job_notifications enable row level security;
drop policy if exists "Users read own notifications" on public.job_notifications;
create policy "Users read own notifications" on public.job_notifications
  for select to authenticated using (user_id = auth.uid());
drop policy if exists "Users mark own notifications read" on public.job_notifications;
create policy "Users mark own notifications read" on public.job_notifications
  for update to authenticated using (user_id = auth.uid()) with check (user_id = auth.uid());
grant select, update on public.job_notifications to authenticated;

create or replace function public.notify_job_message()
returns trigger language plpgsql security definer set search_path = public
as $$
declare recipient uuid;
begin
  select case when c.user_a = new.sender_id then c.user_b else c.user_a end
    into recipient
  from public.job_conversations c where c.id = new.conversation_id;
  if recipient is not null then
    insert into public.job_notifications(user_id, category, title, body, source_id)
    values (recipient, 'messages', 'Nouveau message', left(new.body, 180), new.conversation_id);
  end if;
  return new;
end;
$$;
drop trigger if exists job_message_notification_trigger on public.job_messages;
create trigger job_message_notification_trigger after insert on public.job_messages
for each row execute function public.notify_job_message();

create or replace function public.notify_published_job()
returns trigger language plpgsql security definer set search_path = public
as $$
begin
  if new.status = 'published' and (tg_op = 'INSERT' or old.status is distinct from 'published') then
    insert into public.job_notifications(user_id, category, title, body, action_url, source_id)
    select p.user_id, 'jobs', 'Nouvelle offre d’emploi : ' || new.title,
      new.company || case when new.governorate <> '' then ' • ' || new.governorate else '' end,
      null, new.id
    from public.job_profiles p
    where p.user_id <> new.owner_id and p.account_type = 'job_seeker'
    on conflict do nothing;
  end if;
  return new;
end;
$$;
drop trigger if exists job_offer_notification_trigger on public.job_offers;
create trigger job_offer_notification_trigger after insert or update of status on public.job_offers
for each row execute function public.notify_published_job();

-- Official source registry: links are visible in the app; automated collection is
-- deliberately not scheduled until each source's permitted access/feed is verified.
create table if not exists public.official_job_sources (
  id text primary key,
  name text not null,
  url text not null,
  enabled boolean not null default true,
  access_status text not null default 'manual_link',
  last_checked_at timestamptz,
  notes text not null default ''
);
insert into public.official_job_sources(id,name,url,notes) values
 ('aneti','ANETI','https://www.emploi.nat.tn/','Official national employment portal; feed/API access to be verified'),
 ('idaraty','Idaraty','https://www.idaraty.tn/','Administrative services portal; recruitment notices must be verified at issuer'),
 ('concours','Concours publics','https://www.concours.gov.tn/','Public recruitment and competition notices'),
 ('emploi_ministry','Ministère de l’Emploi','https://www.emploi.gov.tn/fr/','Official ministry announcements')
on conflict (id) do update set name=excluded.name,url=excluded.url,notes=excluded.notes;
alter table public.official_job_sources enable row level security;
drop policy if exists "Authenticated users read official sources" on public.official_job_sources;
create policy "Authenticated users read official sources" on public.official_job_sources
 for select to authenticated using (true);
grant select on public.official_job_sources to authenticated;
