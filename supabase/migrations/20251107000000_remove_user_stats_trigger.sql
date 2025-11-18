-- ==============================================================================
-- migration: remove_user_stats_trigger
-- ==============================================================================
-- purpose: usuwa trigger i funkcję aktualizującą statystyki użytkownika,
--          ponieważ logika biznesowa jest obsługiwana w kodzie Java (PointsService)
--          Trigger powodował podwójne liczenie statystyk (games_played, games_won, total_points)
-- ==============================================================================

DROP TRIGGER IF EXISTS update_user_stats_on_game_finished ON public.games;

DROP FUNCTION IF EXISTS public.update_user_stats_on_game_completion();

