-- ==============================================================================
-- migration: fix_users_registered_check_constraint
-- ==============================================================================
-- purpose: wymusza aktualizację constraint users_registered_check dla 
--          rejestracji bez Supabase Auth (auth_user_id IS NULL)
-- ==============================================================================

-- Usuń constraint jeśli istnieje (niezależnie od wersji)
DO $$
BEGIN
    IF EXISTS (
        SELECT 1 FROM pg_constraint 
        WHERE conname = 'users_registered_check' 
        AND conrelid = 'public.users'::regclass
    ) THEN
        ALTER TABLE public.users DROP CONSTRAINT users_registered_check;
        RAISE NOTICE 'Constraint users_registered_check dropped';
    ELSE
        RAISE NOTICE 'Constraint users_registered_check does not exist';
    END IF;
END $$;

-- Dodaj nowy constraint zgodnie z aktualną wersją
ALTER TABLE public.users
    ADD CONSTRAINT users_registered_check
    CHECK (
        -- Goście: is_guest=true AND auth_user_id IS NULL AND username IS NULL 
        --         AND email IS NULL AND password_hash IS NULL AND ip_address IS NOT NULL
        (is_guest = true AND auth_user_id IS NULL AND username IS NULL AND email IS NULL AND password_hash IS NULL AND ip_address IS NOT NULL)
        OR
        -- Zarejestrowani: is_guest=false AND auth_user_id IS NULL AND username IS NOT NULL 
        --                 AND email IS NOT NULL AND password_hash IS NOT NULL AND ip_address IS NULL
        (is_guest = false AND auth_user_id IS NULL AND username IS NOT NULL AND email IS NOT NULL AND password_hash IS NOT NULL AND ip_address IS NULL)
    );

COMMENT ON CONSTRAINT users_registered_check ON public.users IS 
    'Constraint zapewniający poprawność danych dla gości i zarejestrowanych użytkowników (bez Supabase Auth)';

