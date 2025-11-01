-- ==============================================================================
-- migration: disable_rls_for_local_dev
-- ==============================================================================
-- purpose: tymczasowo wyłącza RLS dla łatwiejszego programowania API lokalnie
-- affected tables: users, games, moves
-- affected schemas: public
-- 
-- uwaga:
-- - ta migracja jest tylko dla środowiska lokalnego (dev)
-- - na produkcji należy przywrócić RLS i włączyć polityki bezpieczeństwa
-- - nie commitować zmian do produkcji bez RLS!
-- ==============================================================================

-- ==============================================================================
-- WYŁĄCZENIE RLS DLA LOKALNEGO DEVELOPEMENTU
-- ==============================================================================

-- wyłącz row level security dla wszystkich tabel
-- to pozwala na swobodne programowanie API bez ograniczeń dostępu
alter table public.users disable row level security;
alter table public.games disable row level security;
alter table public.moves disable row level security;

-- uwaga: polityki RLS pozostają w bazie danych, ale nie są aktywne
-- aby je przywrócić, wystarczy wykonać:
-- ALTER TABLE public.users ENABLE ROW LEVEL SECURITY;
-- ALTER TABLE public.games ENABLE ROW LEVEL SECURITY;
-- ALTER TABLE public.moves ENABLE ROW LEVEL SECURITY;

