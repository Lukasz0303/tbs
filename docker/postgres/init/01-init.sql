-- ==============================================================================
-- Database initialization script for Docker environment
-- ==============================================================================
-- This script initializes the database schema for Docker deployment
-- It combines all Supabase migrations in the correct order, adapted for
-- standard PostgreSQL without Supabase Auth dependencies
-- ==============================================================================

-- ==============================================================================
-- 1. CREATE TABLES (from 20251101140303_create_enums_and_tables.sql)
-- ==============================================================================

CREATE TABLE IF NOT EXISTS public.users (
    id bigserial PRIMARY KEY,
    auth_user_id uuid,
    username varchar(50),
    email varchar(255),
    password_hash varchar(255),
    is_guest boolean NOT NULL DEFAULT false,
    ip_address text,
    total_points bigint NOT NULL DEFAULT 0,
    games_played integer NOT NULL DEFAULT 0,
    games_won integer NOT NULL DEFAULT 0,
    last_seen_at timestamp with time zone,
    avatar integer DEFAULT 1 CHECK (avatar >= 1 AND avatar <= 6),
    created_at timestamp with time zone NOT NULL DEFAULT now(),
    updated_at timestamp with time zone NOT NULL DEFAULT now()
);

COMMENT ON TABLE public.users IS 'użytkownicy: goście (is_guest=true, identyfikacja przez ip) lub zarejestrowani';
COMMENT ON COLUMN public.users.auth_user_id IS 'uuid - opcjonalne, dla przyszłej integracji';
COMMENT ON COLUMN public.users.username IS 'nazwa użytkownika - tylko dla zarejestrowanych';
COMMENT ON COLUMN public.users.is_guest IS 'true=dla gości, false=dla zarejestrowanych';
COMMENT ON COLUMN public.users.ip_address IS 'adres ip gościa - tylko dla gości (text dla kompatybilności z Hibernate)';
COMMENT ON COLUMN public.users.total_points IS 'suma punktów gracza (używane do rankingu)';
COMMENT ON COLUMN public.users.last_seen_at IS 'ostatnia aktywność (używane do matchmakingu)';
COMMENT ON COLUMN public.users.avatar IS 'Typ avatara gracza (1-6, odpowiada plikom 1_3.png - 6_3.png)';

ALTER TABLE public.users
    ADD CONSTRAINT users_registered_check
    CHECK (
        (is_guest = true AND auth_user_id IS NULL AND username IS NULL AND email IS NULL AND password_hash IS NULL AND ip_address IS NOT NULL)
        OR
        (is_guest = false AND auth_user_id IS NULL AND username IS NOT NULL AND email IS NOT NULL AND password_hash IS NOT NULL AND ip_address IS NULL)
    );

CREATE UNIQUE INDEX IF NOT EXISTS idx_users_auth_user_id ON public.users (auth_user_id) WHERE auth_user_id IS NOT NULL;
CREATE UNIQUE INDEX IF NOT EXISTS idx_users_username ON public.users (username) WHERE username IS NOT NULL;
CREATE UNIQUE INDEX IF NOT EXISTS idx_users_email ON public.users (email) WHERE email IS NOT NULL;
CREATE INDEX IF NOT EXISTS idx_users_is_guest ON public.users (is_guest);
CREATE INDEX IF NOT EXISTS idx_users_ip_address ON public.users (ip_address) WHERE ip_address IS NOT NULL;
CREATE INDEX IF NOT EXISTS idx_users_total_points ON public.users (total_points DESC);
CREATE INDEX IF NOT EXISTS idx_users_last_seen_at ON public.users (last_seen_at DESC) WHERE last_seen_at IS NOT NULL;
CREATE INDEX IF NOT EXISTS idx_users_ip_address_is_guest ON public.users(ip_address, is_guest) WHERE is_guest = true;

CREATE TABLE IF NOT EXISTS public.games (
    id bigserial PRIMARY KEY,
    game_type varchar(20) NOT NULL CHECK (game_type IN ('vs_bot', 'pvp')),
    board_size smallint NOT NULL CHECK (board_size IN (3, 4, 5)),
    player1_id bigint NOT NULL REFERENCES public.users(id) ON DELETE CASCADE,
    player2_id bigint REFERENCES public.users(id) ON DELETE CASCADE,
    bot_difficulty varchar(20) CHECK (bot_difficulty IN ('easy', 'medium', 'hard')),
    status varchar(20) NOT NULL DEFAULT 'waiting',
    current_player_symbol varchar(10) CHECK (current_player_symbol IN ('x', 'o')),
    winner_id bigint REFERENCES public.users(id) ON DELETE SET NULL,
    last_move_at timestamp with time zone,
    created_at timestamp with time zone NOT NULL DEFAULT now(),
    updated_at timestamp with time zone NOT NULL DEFAULT now(),
    finished_at timestamp with time zone
);

COMMENT ON TABLE public.games IS 'gry: vs_bot (z botem) lub pvp (z innym graczem) w ujednoliconym modelu';
COMMENT ON COLUMN public.games.game_type IS 'typ gry: vs_bot lub pvp';
COMMENT ON COLUMN public.games.board_size IS 'rozmiar planszy: 3, 4 lub 5';
COMMENT ON COLUMN public.games.bot_difficulty IS 'poziom trudności bota - tylko dla vs_bot';
COMMENT ON COLUMN public.games.status IS 'status gry';
COMMENT ON COLUMN public.games.last_move_at IS 'timestamp ostatniego ruchu (używane do timeout pvp)';

ALTER TABLE public.games
    ADD CONSTRAINT games_vs_bot_check
    CHECK (
        (game_type = 'vs_bot' AND player2_id IS NULL AND bot_difficulty IS NOT NULL)
        OR
        (game_type = 'pvp' AND bot_difficulty IS NULL)
    );

ALTER TABLE public.games
    ADD CONSTRAINT games_status_check
    CHECK (
        (status = 'waiting' AND current_player_symbol IS NULL AND winner_id IS NULL)
        OR
        status IN ('in_progress', 'finished', 'abandoned', 'draw')
    );

ALTER TABLE public.games
    ADD CONSTRAINT games_finished_check
    CHECK (
        (status IN ('finished', 'abandoned', 'draw') AND finished_at IS NOT NULL)
        OR
        status NOT IN ('finished', 'abandoned', 'draw')
    );

CREATE INDEX IF NOT EXISTS idx_games_game_type ON public.games (game_type);
CREATE INDEX IF NOT EXISTS idx_games_status ON public.games (status);
CREATE INDEX IF NOT EXISTS idx_games_player1_id ON public.games (player1_id);
CREATE INDEX IF NOT EXISTS idx_games_player2_id ON public.games (player2_id) WHERE player2_id IS NOT NULL;
CREATE INDEX IF NOT EXISTS idx_games_status_type ON public.games (status, game_type);
CREATE INDEX IF NOT EXISTS idx_games_last_move_at ON public.games (last_move_at) WHERE last_move_at IS NOT NULL;
CREATE INDEX IF NOT EXISTS idx_games_created_at ON public.games (created_at DESC);

CREATE TABLE IF NOT EXISTS public.moves (
    id bigserial PRIMARY KEY,
    game_id bigint NOT NULL REFERENCES public.games(id) ON DELETE CASCADE,
    player_id bigint REFERENCES public.users(id) ON DELETE SET NULL,
    row smallint NOT NULL CHECK (row >= 0),
    col smallint NOT NULL CHECK (col >= 0),
    player_symbol varchar(10) NOT NULL CHECK (player_symbol IN ('x', 'o')),
    move_order smallint NOT NULL CHECK (move_order > 0),
    created_at timestamp with time zone NOT NULL DEFAULT now()
);

COMMENT ON TABLE public.moves IS 'historia ruchów - stan planszy generowany dynamicznie z tej tabeli';
COMMENT ON COLUMN public.moves.player_id IS 'id gracza wykonującego ruch (null dla ruchów bota)';
COMMENT ON COLUMN public.moves.row IS 'wiersz planszy (0-indexed)';
COMMENT ON COLUMN public.moves.col IS 'kolumna planszy (0-indexed)';
COMMENT ON COLUMN public.moves.move_order IS 'kolejność ruchu w grze (pozwala odtworzyć sekwencję)';

CREATE UNIQUE INDEX IF NOT EXISTS idx_moves_game_id_position_unique ON public.moves (game_id, row, col);
CREATE INDEX IF NOT EXISTS idx_moves_game_id ON public.moves (game_id);
CREATE INDEX IF NOT EXISTS idx_moves_game_id_move_order ON public.moves (game_id, move_order);
CREATE INDEX IF NOT EXISTS idx_moves_player_id ON public.moves (player_id) WHERE player_id IS NOT NULL;

-- ==============================================================================
-- 2. DISABLE RLS (from 20251101141720_disable_rls_for_local_dev.sql)
-- ==============================================================================

ALTER TABLE public.users DISABLE ROW LEVEL SECURITY;
ALTER TABLE public.games DISABLE ROW LEVEL SECURITY;
ALTER TABLE public.moves DISABLE ROW LEVEL SECURITY;

-- ==============================================================================
-- 3. CREATE FUNCTIONS AND TRIGGERS (from 20251101140403_create_functions_and_triggers.sql)
-- ==============================================================================

CREATE OR REPLACE FUNCTION public.generate_board_state(p_game_id bigint)
RETURNS text[][]
LANGUAGE plpgsql
STABLE
AS $$
DECLARE
    v_board_size smallint;
    v_board text[][];
    v_move record;
BEGIN
    SELECT board_size INTO v_board_size
    FROM public.games
    WHERE id = p_game_id;
    
    IF v_board_size IS NULL THEN
        RAISE EXCEPTION 'game % does not exist', p_game_id;
    END IF;
    
    v_board := array_fill(''::text, ARRAY[v_board_size, v_board_size]);
    
    FOR v_move IN
        SELECT row, col, player_symbol::text
        FROM public.moves
        WHERE game_id = p_game_id
        ORDER BY move_order
    LOOP
        v_board[v_move.row + 1][v_move.col + 1] := v_move.player_symbol;
    END LOOP;
    
    RETURN v_board;
END;
$$;

COMMENT ON FUNCTION public.generate_board_state(bigint) IS 'generuje stan planszy jako tablicę 2d na podstawie historii ruchów';

CREATE OR REPLACE FUNCTION public.is_move_valid(p_game_id bigint, p_row smallint, p_col smallint)
RETURNS boolean
LANGUAGE plpgsql
STABLE
AS $$
DECLARE
    v_board_size smallint;
BEGIN
    SELECT board_size INTO v_board_size
    FROM public.games
    WHERE id = p_game_id;
    
    IF v_board_size IS NULL THEN
        RETURN false;
    END IF;
    
    IF p_row < 0 OR p_col < 0 OR p_row >= v_board_size OR p_col >= v_board_size THEN
        RETURN false;
    END IF;
    
    IF EXISTS (
        SELECT 1 FROM public.moves
        WHERE game_id = p_game_id AND row = p_row AND col = p_col
    ) THEN
        RETURN false;
    END IF;
    
    RETURN true;
END;
$$;

COMMENT ON FUNCTION public.is_move_valid(bigint, smallint, smallint) IS 'waliduje czy ruch jest poprawny (granice planszy i czy pozycja nie jest zajęta)';

CREATE OR REPLACE FUNCTION public.calculate_game_points(p_game_type varchar, p_bot_difficulty varchar)
RETURNS bigint
LANGUAGE plpgsql
STABLE
AS $$
BEGIN
    IF p_game_type = 'pvp' THEN
        RETURN 1000;
    END IF;
    
    IF p_game_type = 'vs_bot' THEN
        IF p_bot_difficulty = 'easy' THEN
            RETURN 100;
        ELSIF p_bot_difficulty = 'medium' THEN
            RETURN 500;
        ELSIF p_bot_difficulty = 'hard' THEN
            RETURN 1000;
        END IF;
    END IF;
    
    RETURN 0;
END;
$$;

COMMENT ON FUNCTION public.calculate_game_points(varchar, varchar) IS 'oblicza punkty za wygraną: pvp=1000, vs_bot easy=100, medium=500, hard=1000';

CREATE OR REPLACE FUNCTION public.check_pvp_timeout()
RETURNS integer
LANGUAGE plpgsql
AS $$
DECLARE
    v_updated_count integer := 0;
    v_game record;
    v_last_player_id bigint;
BEGIN
    FOR v_game IN
        SELECT id, player1_id, player2_id
        FROM public.games
        WHERE game_type = 'pvp'
        AND status = 'in_progress'
        AND last_move_at < NOW() - INTERVAL '20 seconds'
    LOOP
        SELECT player_id INTO v_last_player_id
        FROM public.moves
        WHERE game_id = v_game.id
        ORDER BY move_order DESC
        LIMIT 1;
        
        UPDATE public.games
        SET status = 'finished',
            winner_id = CASE
                WHEN v_last_player_id = v_game.player1_id THEN v_game.player2_id
                ELSE v_game.player1_id
            END,
            finished_at = NOW()
        WHERE id = v_game.id;
        
        v_updated_count := v_updated_count + 1;
    END LOOP;
    
    RETURN v_updated_count;
END;
$$;

COMMENT ON FUNCTION public.check_pvp_timeout() IS 'sprawdza gry pvp z timeout 20s i automatycznie kończy je - powinna być wywoływana co 5-10s przez spring scheduled job';

CREATE OR REPLACE FUNCTION public.get_user_ranking_position(p_user_id bigint)
RETURNS bigint
LANGUAGE plpgsql
STABLE
AS $$
DECLARE
    v_position bigint;
BEGIN
    WITH ranked_users AS (
        SELECT 
            id,
            ROW_NUMBER() OVER (
                ORDER BY total_points DESC, created_at ASC
            ) AS rank_position
        FROM public.users
        WHERE is_guest = false
    )
    SELECT rank_position INTO v_position
    FROM ranked_users
    WHERE id = p_user_id;
    
    RETURN COALESCE(v_position, 0);
END;
$$;

COMMENT ON FUNCTION public.get_user_ranking_position(bigint) IS 'zwraca pozycję użytkownika w rankingu (tylko zarejestrowani)';

CREATE OR REPLACE FUNCTION public.update_updated_at_column()
RETURNS trigger
LANGUAGE plpgsql
AS $$
BEGIN
    NEW.updated_at = NOW();
    RETURN NEW;
END;
$$;

COMMENT ON FUNCTION public.update_updated_at_column() IS 'automatycznie aktualizuje updated_at do bieżącego czasu';

CREATE OR REPLACE FUNCTION public.update_game_last_move_at()
RETURNS trigger
LANGUAGE plpgsql
AS $$
BEGIN
    UPDATE public.games
    SET last_move_at = NOW()
    WHERE id = NEW.game_id;
    RETURN NEW;
END;
$$;

COMMENT ON FUNCTION public.update_game_last_move_at() IS 'aktualizuje last_move_at w tabeli games przy każdym nowym ruchu';

DROP TRIGGER IF EXISTS update_users_updated_at ON public.users;
CREATE TRIGGER update_users_updated_at
    BEFORE UPDATE ON public.users
    FOR EACH ROW
    EXECUTE FUNCTION public.update_updated_at_column();

DROP TRIGGER IF EXISTS update_games_updated_at ON public.games;
CREATE TRIGGER update_games_updated_at
    BEFORE UPDATE ON public.games
    FOR EACH ROW
    EXECUTE FUNCTION public.update_updated_at_column();

DROP TRIGGER IF EXISTS update_game_last_move_timestamp ON public.moves;
CREATE TRIGGER update_game_last_move_timestamp
    AFTER INSERT ON public.moves
    FOR EACH ROW
    EXECUTE FUNCTION public.update_game_last_move_at();

-- ==============================================================================
-- 4. CREATE VIEWS (from 20251101140403_create_functions_and_triggers.sql)
-- ==============================================================================

CREATE OR REPLACE VIEW public.game_summary AS
SELECT 
    g.*,
    p1.id as player1_user_id,
    p1.username as player1_username,
    p1.is_guest as player1_is_guest,
    p1.auth_user_id as player1_auth_user_id,
    p2.id as player2_user_id,
    p2.username as player2_username,
    p2.is_guest as player2_is_guest,
    p2.auth_user_id as player2_auth_user_id,
    w.id as winner_user_id,
    w.username as winner_username,
    w.auth_user_id as winner_auth_user_id,
    (SELECT COUNT(*) FROM public.moves WHERE game_id = g.id) as total_moves
FROM public.games g
LEFT JOIN public.users p1 ON g.player1_id = p1.id
LEFT JOIN public.users p2 ON g.player2_id = p2.id
LEFT JOIN public.users w ON g.winner_id = w.id;

COMMENT ON VIEW public.game_summary IS 'widok łączący tabele games i users - kompleksowe informacje o grze';

-- ==============================================================================
-- 5. CREATE MATERIALIZED VIEW - player_rankings
-- ==============================================================================

CREATE MATERIALIZED VIEW IF NOT EXISTS public.player_rankings AS
SELECT 
    id,
    username,
    total_points,
    games_played,
    games_won,
    ROW_NUMBER() OVER (ORDER BY total_points DESC, created_at ASC) as rank_position,
    created_at
FROM public.users
WHERE is_guest = false;

COMMENT ON MATERIALIZED VIEW public.player_rankings IS 'ranking graczy (tylko zarejestrowani) - pre-obliczona pozycja';

CREATE UNIQUE INDEX IF NOT EXISTS idx_player_rankings_id ON public.player_rankings (id);
CREATE INDEX IF NOT EXISTS idx_player_rankings_points ON public.player_rankings (total_points DESC);
CREATE INDEX IF NOT EXISTS idx_player_rankings_rank ON public.player_rankings (rank_position);

REFRESH MATERIALIZED VIEW public.player_rankings;

CREATE OR REPLACE FUNCTION public.refresh_player_rankings()
RETURNS void
LANGUAGE plpgsql
AS $$
BEGIN
    REFRESH MATERIALIZED VIEW CONCURRENTLY public.player_rankings;
EXCEPTION WHEN OTHERS THEN
    REFRESH MATERIALIZED VIEW public.player_rankings;
END;
$$;

COMMENT ON FUNCTION public.refresh_player_rankings() IS 'odświeża materialized view player_rankings (concurrently) - powinna być wywoływana co 5-15min przez spring scheduled job';

