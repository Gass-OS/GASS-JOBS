-- TUN-JOBS secure matching and messaging foundation
-- Run once in Supabase Dashboard -> SQL Editor.
-- Demo profiles stored in the APK are intentionally excluded: only opted-in auth users can match.

create extension if not exists pgcrypto;

create table if not exists public.job_profiles (
  user_id uuid primary key references auth.users(id) on delete cascade,
  full_name text not null default '',
  phone text,
  city text,
  profession text,
  education text,
  skills text,
  availability text,
  photo_url text,
  account_type text not null default 'job_seeker'
    check (account_type in ('job_seeker','employer')),
  is_discoverable boolean not null default false,
  updated_at timestamptz not null default now()
);

create table if not exists public.job_swipes (
  id uuid primary key default gen_random_uuid(),
  actor_id uuid not null references auth.users(id) on delete cascade,
  target_id uuid not null references auth.users(id) on delete cascade,
  decision text not null check (decision in ('like','pass')),
  created_at timestamptz not null default now(),
  unique (actor_id, target_id),
  check (actor_id <> target_id)
);

create table if not exists public.job_matches (
  id uuid primary key default gen_random_uuid(),
  user_a uuid not null references auth.users(id) on delete cascade,
  user_b uuid not null references auth.users(id) on delete cascade,
  created_at timestamptz not null default now(),
  check (user_a < user_b),
  unique (user_a, user_b)
);

create table if not exists public.job_conversations (
  id uuid primary key default gen_random_uuid(),
  match_id uuid not null unique references public.job_matches(id) on delete cascade,
  user_a uuid not null references auth.users(id) on delete cascade,
  user_b uuid not null references auth.users(id) on delete cascade,
  created_at timestamptz not null default now(),
  check (user_a < user_b),
  unique (user_a, user_b)
);

create table if not exists public.job_messages (
  id uuid primary key default gen_random_uuid(),
  conversation_id uuid not null references public.job_conversations(id) on delete cascade,
  sender_id uuid not null references auth.users(id) on delete cascade,
  body text not null check (length(trim(body)) between 1 and 4000),
  created_at timestamptz not null default now(),
  read_at timestamptz
);

create index if not exists job_swipes_target_created_idx on public.job_swipes(target_id, created_at desc);
create index if not exists job_matches_user_a_idx on public.job_matches(user_a);
create index if not exists job_matches_user_b_idx on public.job_matches(user_b);
create index if not exists job_messages_conversation_created_idx on public.job_messages(conversation_id, created_at);

alter table public.job_profiles enable row level security;
alter table public.job_swipes enable row level security;
alter table public.job_matches enable row level security;
alter table public.job_conversations enable row level security;
alter table public.job_messages enable row level security;

drop policy if exists "Users manage own job profile" on public.job_profiles;
create policy "Users manage own job profile" on public.job_profiles
  for all to authenticated
  using (user_id = auth.uid())
  with check (user_id = auth.uid());

drop policy if exists "Authenticated users view opted-in profiles" on public.job_profiles;
create policy "Authenticated users view opted-in profiles" on public.job_profiles
  for select to authenticated
  using (is_discoverable = true);

drop policy if exists "Participants view swipes" on public.job_swipes;
create policy "Participants view swipes" on public.job_swipes
  for select to authenticated
  using (actor_id = auth.uid() or target_id = auth.uid());

drop policy if exists "Users create own swipes" on public.job_swipes;
create policy "Users create own swipes" on public.job_swipes
  for insert to authenticated
  with check (actor_id = auth.uid());

drop policy if exists "Users update own swipes" on public.job_swipes;
create policy "Users update own swipes" on public.job_swipes
  for update to authenticated
  using (actor_id = auth.uid())
  with check (actor_id = auth.uid());

drop policy if exists "Participants view matches" on public.job_matches;
create policy "Participants view matches" on public.job_matches
  for select to authenticated
  using (user_a = auth.uid() or user_b = auth.uid());

drop policy if exists "Participants view conversations" on public.job_conversations;
create policy "Participants view conversations" on public.job_conversations
  for select to authenticated
  using (user_a = auth.uid() or user_b = auth.uid());

drop policy if exists "Participants read messages" on public.job_messages;
create policy "Participants read messages" on public.job_messages
  for select to authenticated
  using (exists (
    select 1 from public.job_conversations c
    where c.id = conversation_id and (c.user_a = auth.uid() or c.user_b = auth.uid())
  ));

drop policy if exists "Matched users send messages" on public.job_messages;
create policy "Matched users send messages" on public.job_messages
  for insert to authenticated
  with check (
    sender_id = auth.uid()
    and exists (
      select 1 from public.job_conversations c
      where c.id = conversation_id and (c.user_a = auth.uid() or c.user_b = auth.uid())
    )
  );

drop policy if exists "Recipients mark messages read" on public.job_messages;
create policy "Recipients mark messages read" on public.job_messages
  for update to authenticated
  using (
    sender_id <> auth.uid()
    and exists (
      select 1 from public.job_conversations c
      where c.id = conversation_id and (c.user_a = auth.uid() or c.user_b = auth.uid())
    )
  )
  with check (
    sender_id <> auth.uid()
    and exists (
      select 1 from public.job_conversations c
      where c.id = conversation_id and (c.user_a = auth.uid() or c.user_b = auth.uid())
    )
  );

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
begin
  if new.decision <> 'like' then
    return new;
  end if;

  if not exists (
    select 1 from public.job_profiles p
    where p.user_id = new.target_id and p.is_discoverable = true
  ) then
    return new;
  end if;

  if exists (
    select 1 from public.job_swipes s
    where s.actor_id = new.target_id
      and s.target_id = new.actor_id
      and s.decision = 'like'
  ) then
    first_user := least(new.actor_id, new.target_id);
    second_user := greatest(new.actor_id, new.target_id);

    insert into public.job_matches(user_a, user_b)
    values (first_user, second_user)
    on conflict (user_a, user_b) do nothing;

    select id into new_match_id
    from public.job_matches
    where user_a = first_user and user_b = second_user;

    insert into public.job_conversations(match_id, user_a, user_b)
    values (new_match_id, first_user, second_user)
    on conflict (match_id) do nothing;
  end if;

  return new;
end;
$$;

drop trigger if exists job_swipe_match_trigger on public.job_swipes;
create trigger job_swipe_match_trigger
after insert or update of decision on public.job_swipes
for each row execute function public.create_job_match_after_like();

-- Enable Realtime for chat delivery (safe to run repeatedly).
do $$
begin
  if not exists (
    select 1 from pg_publication_tables
    where pubname = 'supabase_realtime' and schemaname = 'public' and tablename = 'job_messages'
  ) then
    alter publication supabase_realtime add table public.job_messages;
  end if;
end $$;

grant select, insert, update on public.job_profiles to authenticated;
grant select, insert, update on public.job_swipes to authenticated;
grant select on public.job_matches, public.job_conversations to authenticated;
grant select, insert on public.job_messages to authenticated;
grant update (read_at) on public.job_messages to authenticated;
