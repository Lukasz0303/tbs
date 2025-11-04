-- ==============================================================================
-- migration: fix_games_vs_bot_check_constraint
-- ==============================================================================
-- purpose: naprawia constraint games_vs_bot_check aby pozwalał na null player2_id dla PVP w statusie waiting
-- affected tables: games
-- 
-- szczegóły:
-- - constraint wymagał player2_id NOT NULL dla PVP, ale podczas tworzenia gry
--   w statusie WAITING, player2_id jest null (dopiero później przypisuje się
--   drugiego gracza przez matchmaking)
-- - nowy constraint: dla PVP bot_difficulty musi być null, ale player2_id
--   może być null (waiting) lub not null (in_progress)
-- ==============================================================================

-- usuń stary constraint
alter table public.games
    drop constraint if exists games_vs_bot_check;

-- dodaj poprawiony constraint
alter table public.games
    add constraint games_vs_bot_check
    check (
        -- vs_bot: game_type='vs_bot' AND player2_id null AND bot_difficulty not null
        (game_type = 'vs_bot' AND player2_id IS NULL AND bot_difficulty IS NOT NULL)
        OR
        -- pvp: game_type='pvp' AND bot_difficulty null (player2_id może być null dla waiting lub not null dla in_progress)
        (game_type = 'pvp' AND bot_difficulty IS NULL)
    );

comment on constraint games_vs_bot_check on public.games is 'zapewnia poprawność typu gry: vs_bot wymaga bot_difficulty, pvp wymaga null bot_difficulty (player2_id może być null dla waiting)';

