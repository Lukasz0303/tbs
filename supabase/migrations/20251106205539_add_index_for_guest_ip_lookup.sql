-- ==============================================================================
-- migration: add_index_for_guest_ip_lookup
-- ==============================================================================
-- purpose: dodaje indeks dla szybkiego wyszukiwania gości po adresie IP
--          optymalizuje zapytanie findByIpAddressAndIsGuest
-- ==============================================================================

CREATE INDEX IF NOT EXISTS idx_users_ip_address_is_guest 
ON public.users(ip_address, is_guest) 
WHERE is_guest = true;

COMMENT ON INDEX idx_users_ip_address_is_guest IS 'Indeks dla szybkiego wyszukiwania gości po adresie IP - używany przez GuestService.findOrCreateGuest()';

