-- ==============================================================================
-- migration: add_email_and_password_to_users
-- ==============================================================================
-- purpose: dodaje pola email i password_hash do tabeli users dla tymczasowej
--          implementacji autoryzacji bez Supabase Auth
-- ==============================================================================

alter table public.users
    add column email varchar(255) unique,
    add column password_hash varchar(255);

create index idx_users_email on public.users (email) where email is not null;

alter table public.users
    drop constraint users_registered_check;

alter table public.users
    add constraint users_registered_check
    check (
        (is_guest = true AND auth_user_id IS NULL AND username IS NULL AND email IS NULL AND password_hash IS NULL AND ip_address IS NOT NULL)
        OR
        (is_guest = false AND auth_user_id IS NULL AND username IS NOT NULL AND email IS NOT NULL AND password_hash IS NOT NULL AND ip_address IS NULL)
    );

comment on column public.users.email is 'email użytkownika (temporary dla local development)';
comment on column public.users.password_hash is 'hash hasła (temporary dla local development)';

