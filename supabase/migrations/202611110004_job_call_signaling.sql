-- TUN-JOBS voice/video call signalling for matched users.
-- Run once in Supabase Dashboard -> SQL Editor.
-- Media travels peer-to-peer / via Cloudflare TURN; this table carries signalling only.

create table if not exists public.job_call_signals (
  id uuid primary key default gen_random_uuid(),
  match_id uuid not null references public.job_matches(id) on delete cascade,
  sender_id uuid not null references auth.users(id) on delete cascade,
  recipient_id uuid not null references auth.users(id) on delete cascade,
  signal_type text not null check (signal_type in (
    'offer', 'answer', 'ice_candidate', 'reject', 'hangup'
  )),
  payload jsonb not null default '{}'::jsonb,
  created_at timestamptz not null default now(),
  check (sender_id <> recipient_id)
);

create index if not exists job_call_signals_recipient_created_idx
  on public.job_call_signals(recipient_id, created_at desc);

create index if not exists job_call_signals_match_created_idx
  on public.job_call_signals(match_id, created_at);

alter table public.job_call_signals enable row level security;

drop policy if exists "Matched users read their call signals" on public.job_call_signals;
create policy "Matched users read their call signals"
  on public.job_call_signals
  for select to authenticated
  using (
    (sender_id = auth.uid() or recipient_id = auth.uid())
    and exists (
      select 1
      from public.job_matches m
      where m.id = match_id
        and (m.user_a = auth.uid() or m.user_b = auth.uid())
        and (
          (m.user_a = sender_id and m.user_b = recipient_id)
          or (m.user_b = sender_id and m.user_a = recipient_id)
        )
    )
  );

drop policy if exists "Matched users send call signals" on public.job_call_signals;
create policy "Matched users send call signals"
  on public.job_call_signals
  for insert to authenticated
  with check (
    sender_id = auth.uid()
    and exists (
      select 1
      from public.job_matches m
      where m.id = match_id
        and (
          (m.user_a = auth.uid() and m.user_b = recipient_id)
          or (m.user_b = auth.uid() and m.user_a = recipient_id)
        )
    )
  );

grant select, insert on public.job_call_signals to authenticated;

-- Enable Supabase Realtime for low-latency call signalling.
do $$
begin
  if not exists (
    select 1 from pg_publication_tables
    where pubname = 'supabase_realtime'
      and schemaname = 'public'
      and tablename = 'job_call_signals'
  ) then
    alter publication supabase_realtime add table public.job_call_signals;
  end if;
end $$;
