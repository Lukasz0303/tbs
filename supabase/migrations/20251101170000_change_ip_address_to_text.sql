-- ==============================================================================
-- migration: change_ip_address_to_text
-- ==============================================================================
-- purpose: zmienia typ kolumny ip_address z inet na text dla kompatybilności
--          z Hibernate (Hibernate nie obsługuje poprawnie null dla typu inet)
-- ==============================================================================

-- Usuń polityki RLS używające kolumny ip_address
DROP POLICY IF EXISTS users_insert_anon ON public.users;
DROP POLICY IF EXISTS users_select_anon ON public.users;

-- Zmień typ kolumny z inet na text
ALTER TABLE public.users
    ALTER COLUMN ip_address TYPE text USING ip_address::text;

-- Przywróć polityki RLS (zmienione, aby działały z typem text)
CREATE POLICY users_insert_anon ON public.users
    FOR INSERT
    WITH CHECK (
        is_guest = true AND auth_user_id IS NULL AND ip_address IS NOT NULL
    );

CREATE POLICY users_select_anon ON public.users
    FOR SELECT
    USING (
        is_guest = false AND username IS NOT NULL
    );

COMMENT ON COLUMN public.users.ip_address IS 'adres ip gościa - tylko dla gości (zmieniony na text dla kompatybilności z Hibernate)';

