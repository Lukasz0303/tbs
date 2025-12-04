#!/usr/bin/env bash
set -euo pipefail

BASE_URL="${BASE_URL:-http://localhost:8080}"
BASE_URL="${BASE_URL%/}"
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
source "$SCRIPT_DIR/test-common.sh"
require_commands curl jq

color "36" "========================================"
color "36" "Test scenariusza ruchu bota"
color "36" "========================================"

timestamp=$(date +%Y%m%d%H%M%S)
email="test${timestamp}@test.com"
username="testuser${timestamp}"
password="test123!!"
token=""
user_id=""

section "KROK 1: Rejestracja uzytkownika..."
register_or_login_user "$email" "$password" "$username" "TestUser" token user_id

section "KROK 2: Tworzenie nowej gry vs_bot..."
create_game_body=$(jq -n '{gameType:"vs_bot",boardSize:3,botDifficulty:"easy"}')
api_request POST "/api/v1/games" "$create_game_body" "$token"
ensure_ok "Blad tworzenia gry"
game_id=$(jq -r '.gameId' <<<"$API_BODY")
success "OK Gra utworzona: ID $game_id, status $(jq -r '.status' <<<"$API_BODY")"

section "KROK 3: Wykonanie ruchu gracza (symbol x na 1,1)..."
player_move_body=$(jq -n '{row:1,col:1,playerSymbol:"x"}')
api_request POST "/api/v1/games/$game_id/moves" "$player_move_body" "$token"
ensure_ok "Blad ruchu gracza"
player_move_id=$(jq -r '.moveId' <<<"$API_BODY")
success "OK Ruch gracza Move ID $player_move_id"
printf "\nStan planszy:\n"
print_board <<<"$API_BODY"

section "KROK 4: Wykonanie ruchu bota..."
api_request POST "/api/v1/games/$game_id/bot-move" "" "$token"
ensure_ok "Blad ruchu bota"
bot_symbol=$(jq -r '.playerSymbol' <<<"$API_BODY")
bot_row=$(jq -r '.row' <<<"$API_BODY")
bot_col=$(jq -r '.col' <<<"$API_BODY")
board_size=$(jq '.boardState.state | length' <<<"$API_BODY")
if [[ "$bot_symbol" != "o" ]]; then
  fail "Bot wykonal symbol $bot_symbol zamiast o"
fi
if (( bot_row < 0 || bot_col < 0 || bot_row >= board_size || bot_col >= board_size )); then
  fail "Bot wykonal ruch poza plansza ($bot_row,$bot_col)"
fi
cell_value=$(jq -r --argjson r "$bot_row" --argjson c "$bot_col" '.boardState.state[$r][$c]' <<<"$API_BODY")
if [[ "$cell_value" != "$bot_symbol" ]]; then
  fail "Symbol na planszy ($cell_value) nie zgadza sie z ruchem ($bot_symbol)"
fi
success "OK Ruch bota Move ID $(jq -r '.moveId' <<<"$API_BODY") na ($bot_row,$bot_col)"
printf "\nStan planszy:\n"
print_board <<<"$API_BODY"

printf "\n"
color "36" "========================================"
color "36" "Test zakonczony"
color "36" "========================================"

