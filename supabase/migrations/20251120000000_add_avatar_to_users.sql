-- ==============================================================================
-- migration: add_avatar_to_users
-- ==============================================================================
-- purpose: dodaje pole avatar (integer) do tabeli users dla personalizacji profilu gracza
-- affected tables: users
-- affected schemas: public
-- 
-- szczegóły:
-- - dodaje kolumnę avatar typu integer (wartości 1-6 odpowiadają plikom 1_3.png - 6_3.png)
-- - ustawia domyślną wartość 1 dla istniejących użytkowników
-- - dodaje constraint sprawdzający zakres wartości (1-6)
-- ==============================================================================

-- Dodaj kolumnę avatar do tabeli users
ALTER TABLE public.users
    ADD COLUMN IF NOT EXISTS avatar INTEGER DEFAULT 1 CHECK (avatar >= 1 AND avatar <= 6);

-- Ustaw domyślną wartość 1 dla istniejących użytkowników
UPDATE public.users
SET avatar = 1
WHERE avatar IS NULL;

-- Dodaj komentarz do kolumny
COMMENT ON COLUMN public.users.avatar IS 'Typ avatara gracza (1-6, odpowiada plikom 1_3.png - 6_3.png)';

