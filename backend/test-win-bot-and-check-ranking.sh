#!/usr/bin/env bash
set -euo pipefail

BASE_URL="${BASE_URL:-http://localhost:8080}"
BASE_URL="${BASE_URL%/}"
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
source "$SCRIPT_DIR/test-common.sh"
require_commands curl jq

color "36" "========================================"
color "36" "Test wygranej z botem i rankingu"
color "36" "========================================"

timestamp=$(date +%Y%m%d%H%M%S)
email="test${timestamp}@test.com"
username="testuser${timestamp}"
password="test123!!"

token=""
user_id=""
register_or_login_user "$email" "$password" "$username" "TestUser" token user_id

section "KROK 2: Pobranie poczatkowego profilu..."
api_request GET "/api/v1/auth/me" "" "$token"
ensure_ok "Blad pobierania profilu"
initial_points=$(jq -r '.totalPoints' <<<"$API_BODY")
initial_games_played=$(jq -r '.gamesPlayed' <<<"$API_BODY")
initial_games_won=$(jq -r '.gamesWon' <<<"$API_BODY")
success "Stan poczatkowy: Punkty=$initial_points, Gry=$initial_games_played, Wygrane=$initial_games_won"

section "KROK 3: Tworzenie gry vs_bot..."
create_game_body=$(jq -n '{gameType:"vs_bot",boardSize:3,botDifficulty:"easy"}')
api_request POST "/api/v1/games" "$create_game_body" "$token"
ensure_ok "Blad tworzenia gry"
game_id=$(jq -r '.gameId' <<<"$API_BODY")
success "Gra utworzona: ID $game_id"

declare -a BOARD=()
board_size=3
NEXT_MOVE_ROW=-1
NEXT_MOVE_COL=-1

board_index() {
  local row=$1
  local col=$2
  printf '%d\n' $(( row * board_size + col ))
}

board_set() {
  local row=$1
  local col=$2
  local value=$3
  local idx
  idx=$(board_index "$row" "$col")
  BOARD[$idx]="$value"
}

board_get() {
  local row=$1
  local col=$2
  local idx
  idx=$(board_index "$row" "$col")
  if [[ -z "${BOARD[$idx]:-}" ]]; then
    printf "."
  else
    printf "%s" "${BOARD[$idx]}"
  fi
}

init_empty_board() {
  local size=$1
  board_size=$size
  local total=$((size * size))
  BOARD=()
  local idx
  for ((idx=0; idx<total; idx++)); do
    BOARD[$idx]="."
  done
}

load_board_from_state() {
  local state=$1
  if jq -e '.boardState.state == null' >/dev/null <<<"$state"; then
    init_empty_board "$(jq -r '.boardSize' <<<"$state")"
    return 1
  fi
  local rows=()
  local row
  while IFS= read -r row; do
    rows+=("$row")
  done < <(jq -r '.boardState.state[] | [ .[] | (. // ".") ] | @tsv' <<<"$state") || true
  init_empty_board "${#rows[@]}"
  local row_idx=0
  for row in "${rows[@]}"; do
    local IFS=$'\t'
    read -r -a cells <<<"$row"
    local col_idx=0
    local cell
    for cell in "${cells[@]}"; do
      [[ -z "$cell" || "$cell" == "null" ]] && cell="."
      board_set "$row_idx" "$col_idx" "$cell"
      ((col_idx++))
    done
    ((row_idx++))
  done
  return 0
}

apply_moves_from_history() {
  local gid=$1
  api_request GET "/api/v1/games/$gid/moves" "" "$token"
  if ! http_success; then
    return
  fi
  local moves_json
  moves_json=$(jq -c 'if type=="array" then . else (.content // []) end' <<<"$API_BODY")
  while IFS= read -r move; do
    local r c sym
    r=$(jq -r '.row' <<<"$move")
    c=$(jq -r '.col' <<<"$move")
    sym=$(jq -r '.playerSymbol // "."' <<<"$move")
    [[ "$r" == "null" || "$c" == "null" ]] && continue
    board_set "$r" "$c" "$sym"
  done < <(jq -c '.[]' <<<"$moves_json") || true
}

first_empty_cell() {
  local r c
  for ((r=0; r<board_size; r++)); do
    for ((c=0; c<board_size; c++)); do
      if [[ "$(board_get "$r" "$c")" == "." ]]; then
        NEXT_MOVE_ROW=$r
        NEXT_MOVE_COL=$c
        return 0
      fi
    done
  done
  return 1
}

find_winning_move() {
  local symbol=$1
  local r c
  for ((r=0; r<board_size; r++)); do
    local count=0 empty_r=-1 empty_c=-1 valid=1
    for ((c=0; c<board_size; c++)); do
      local cell
      cell=$(board_get "$r" "$c")
      if [[ "$cell" == "$symbol" ]]; then
        ((count++))
      elif [[ "$cell" == "." ]]; then
        empty_r=$r
        empty_c=$c
      else
        valid=0
        break
      fi
    done
    if (( valid == 1 && count == board_size - 1 && empty_r != -1 )); then
      NEXT_MOVE_ROW=$empty_r
      NEXT_MOVE_COL=$empty_c
      return 0
    fi
  done
  for ((c=0; c<board_size; c++)); do
    local count=0 empty_r=-1 empty_c=-1 valid=1
    for ((r=0; r<board_size; r++)); do
      local cell
      cell=$(board_get "$r" "$c")
      if [[ "$cell" == "$symbol" ]]; then
        ((count++))
      elif [[ "$cell" == "." ]]; then
        empty_r=$r
        empty_c=$c
      else
        valid=0
        break
      fi
    done
    if (( valid == 1 && count == board_size - 1 && empty_r != -1 )); then
      NEXT_MOVE_ROW=$empty_r
      NEXT_MOVE_COL=$empty_c
      return 0
    fi
  done
  local count=0 empty_r=-1 empty_c=-1 valid=1
  for ((r=0; r<board_size; r++)); do
    local cell
    cell=$(board_get "$r" "$r")
    if [[ "$cell" == "$symbol" ]]; then
      ((count++))
    elif [[ "$cell" == "." ]]; then
      empty_r=$r
      empty_c=$r
    else
      valid=0
      break
    fi
  done
  if (( valid == 1 && count == board_size - 1 && empty_r != -1 )); then
    NEXT_MOVE_ROW=$empty_r
    NEXT_MOVE_COL=$empty_c
    return 0
  fi
  count=0 empty_r=-1 empty_c=-1 valid=1
  for ((r=0; r<board_size; r++)); do
    local c_rev=$((board_size - 1 - r))
    local cell
    cell=$(board_get "$r" "$c_rev")
    if [[ "$cell" == "$symbol" ]]; then
      ((count++))
    elif [[ "$cell" == "." ]]; then
      empty_r=$r
      empty_c=$c_rev
    else
      valid=0
      break
    fi
  done
  if (( valid == 1 && count == board_size - 1 && empty_r != -1 )); then
    NEXT_MOVE_ROW=$empty_r
    NEXT_MOVE_COL=$empty_c
    return 0
  fi
  return 1
}

game_finished=0
player_won=0
max_moves=15
move_counter=0

while (( game_finished == 0 && move_counter < max_moves )); do
  ((++move_counter))
  api_request GET "/api/v1/games/$game_id" "" "$token"
  ensure_ok "Blad pobierania stanu gry"
  game_state="$API_BODY"
  status=$(jq -r '.status // ""' <<<"$game_state")
  board_size=$(jq -r '.boardSize' <<<"$game_state")
  load_board_from_state "$game_state" || apply_moves_from_history "$game_id"
  if [[ "$status" == "finished" ]]; then
    game_finished=1
    winner_id=$(jq -r '.winnerId // empty' <<<"$game_state")
    if [[ "$winner_id" == "$user_id" ]]; then
      player_won=1
      success "Gra zakonczona wygrana gracza"
    else
      warn "Gra zakonczona bez zwyciestwa gracza"
    fi
    break
  fi
  current_symbol=$(jq -r '.currentPlayerSymbol // ""' <<<"$game_state")
  if [[ "$current_symbol" != "x" && "$current_symbol" != "" ]]; then
    info "Oczekiwanie na ture gracza (aktualny symbol: $current_symbol)"
    sleep 1
    continue
  fi
  NEXT_MOVE_ROW=-1
  NEXT_MOVE_COL=-1
  if ! find_winning_move "x"; then
    first_empty_cell || fail "Brak wolnych pol na planszy"
  fi
  section "Ruch gracza na (${NEXT_MOVE_ROW},${NEXT_MOVE_COL})"
  player_move_body=$(jq -n --argjson row "$NEXT_MOVE_ROW" --argjson col "$NEXT_MOVE_COL" '{row:$row,col:$col,playerSymbol:"x"}')
  api_request POST "/api/v1/games/$game_id/moves" "$player_move_body" "$token"
  ensure_ok "Blad ruchu gracza"
  success "Ruch gracza wykonany: Move ID $(jq -r '.moveId' <<<"$API_BODY")"
  printf "\nStan planszy:\n"
  print_board <<<"$API_BODY"
  load_board_from_state "$API_BODY" || apply_moves_from_history "$game_id"
  game_status_after_move=$(jq -r '.gameStatus // ""' <<<"$API_BODY")
  if [[ "$game_status_after_move" == "finished" ]]; then
    game_finished=1
    if jq -e '.winner.userId == '"$user_id" <<<"$API_BODY" >/dev/null; then
      player_won=1
      success "Gracz wygral gre!"
    else
      warn "Gra zakonczona bez zwyciestwa gracza"
    fi
    break
  fi
  info "Wywolanie ruchu bota..."
  api_request POST "/api/v1/games/$game_id/bot-move" "" "$token"
  ensure_ok "Blad ruchu bota"
  printf "\nStan planszy po ruchu bota:\n"
  print_board <<<"$API_BODY"
  if jq -e '.gameStatus == "finished"' >/dev/null <<<"$API_BODY"; then
    game_finished=1
    if jq -e '.winner.userId == '"$user_id" <<<"$API_BODY" >/dev/null; then
      player_won=1
      success "Gracz wygral gre!"
    else
      warn "Gra zakonczona bez zwyciestwa gracza"
    fi
    break
  fi
  sleep 0.5
done

if (( player_won == 0 )); then
  fail "Gracz nie wygral gry. Test przerwany."
fi

section "KROK 5: Profil po wygranej"
api_request GET "/api/v1/auth/me" "" "$token"
ensure_ok "Blad pobierania profilu"
new_points=$(jq -r '.totalPoints' <<<"$API_BODY")
new_games_played=$(jq -r '.gamesPlayed' <<<"$API_BODY")
new_games_won=$(jq -r '.gamesWon' <<<"$API_BODY")
success "Nowy stan: Punkty=$new_points (was $initial_points), Gry=$new_games_played (was $initial_games_played), Wygrane=$new_games_won (was $initial_games_won)"

section "KROK 6: Globalny ranking"
api_request GET "/api/v1/rankings?page=0&size=100" "" "$token"
ensure_ok "Blad pobierania rankingu"
player_entry=$(jq -c --arg uid "$user_id" '.content[]? | select((.userId|tostring)==$uid)' <<<"$API_BODY")
if [[ -n "$player_entry" ]]; then
  success "Gracz znaleziony w rankingu na pozycji $(jq -r '.rankPosition' <<<"$player_entry")"
else
  warn "Gracz nie wystepuje w pierwszej setce rankingu"
fi

section "KROK 7: Pozycja gracza w rankingu"
api_request GET "/api/v1/rankings/$user_id" "" "$token"
if http_success; then
  success "Pozycja gracza: $(jq -r '.rankPosition' <<<"$API_BODY"), Punkty: $(jq -r '.totalPoints' <<<"$API_BODY")"
  if [[ "$(jq -r '.totalPoints' <<<"$API_BODY")" != "$new_points" ]]; then
    warn "Punkty w rankingu roznia sie od profilu"
  fi
  if [[ "$(jq -r '.gamesPlayed' <<<"$API_BODY")" != "$new_games_played" ]]; then
    warn "Gry rozegrane w rankingu roznia sie od profilu"
  fi
  if [[ "$(jq -r '.gamesWon' <<<"$API_BODY")" != "$new_games_won" ]]; then
    warn "Wygrane w rankingu roznia sie od profilu"
  fi
else
  warn "Nie udalo sie pobrac pozycji gracza w rankingu (HTTP $API_STATUS)"
fi

printf "\n"
color "36" "========================================"
color "36" "Test zakonczony"
color "36" "========================================"
