create table public.data_sources (
 id text primary key,
 name text not null,
 url text not null check (url like 'https://%'),
 purpose text not null,
 refresh_seconds integer not null check (refresh_seconds >= 60)
);
alter table public.data_sources enable row level security;
revoke all on public.data_sources from anon, authenticated;
grant select on public.data_sources to anon, authenticated;
create policy "Public source catalogue" on public.data_sources for select to anon, authenticated using (true);
insert into public.data_sources(id,name,url,purpose,refresh_seconds) values
 ('cert_pl','CERT Polska / NASK — Lista Ostrzeżeń','https://hole.cert.pl/domains/v2/domains.txt','Domeny phishingowe. Nie jest to baza reputacji numerów telefonu.',300);

create table public.private_notes (
 user_id uuid not null references auth.users(id) on delete cascade default auth.uid(),
 phone text not null check (phone ~ '^\+[1-9][0-9]{6,14}$'),
 label text not null default '' check (char_length(label) <= 80),
 note text not null default '' check (char_length(note) <= 500),
 primary key (user_id, phone)
);
alter table public.private_notes enable row level security;
revoke all on public.private_notes from anon, authenticated;
grant select, insert, update, delete on public.private_notes to authenticated;
create policy "Read own notes" on public.private_notes for select to authenticated using ((select auth.uid()) = user_id);
create policy "Insert own notes" on public.private_notes for insert to authenticated with check ((select auth.uid()) = user_id);
create policy "Update own notes" on public.private_notes for update to authenticated using ((select auth.uid()) = user_id) with check ((select auth.uid()) = user_id);
create policy "Delete own notes" on public.private_notes for delete to authenticated using ((select auth.uid()) = user_id);

