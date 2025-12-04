#!/usr/bin/env bash
set -euo pipefail

BASE_URL="${BASE_URL:-http://localhost:8080}"
BASE_URL="${BASE_URL%/}"
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
source "$SCRIPT_DIR/test-common.sh"
require_commands curl jq

color "36" "========================================"
color "36" "Test scenariusza gry PvP"
color "36" "========================================"

user1_email="lukasz.zielinski0303@gmail.com"
user1_password="u331ty!!"
user1_username="lukasz.zielinski"

user2_email="l.zzzielinski@gmail.com"
user2_password="u331ty!!"
user2_username="karol.zielinski"

user1_token=""
user1_id=""
user2_token=""
user2_id=""
game_id=""
LAST_MOVE_BODY=""
GAME_STATE=""

register_or_login_user "$user1_email" "$user1_password" "$user1_username" "Gracz A" user1_token user1_id
register_or_login_user "$user2_email" "$user2_password" "$user2_username" "Gracz B" user2_token user2_id

remove_from_queue() {
  local token=$1
  local label=$2
  info "Usuwanie $label z kolejki matchmaking..."
  api_request DELETE "/api/v1/matching/queue" "" "$token"
  if http_success; then
    success "$label usuniety z kolejki matchmaking"
    return
  fi
  if [[ $API_STATUS -eq 404 ]]; then
    info "$label nie byl w kolejce matchmaking"
    return
  fi
  warn "Nie udalo sie usunac $label z kolejki (HTTP $API_STATUS)"
}

end_active_pvp_games() {
  local token=$1
  local label=$2
  section "KROK: Sprawdzanie aktywnych gier PvP $label..."
  local combined="[]"
  local status
  for status in IN_PROGRESS WAITING; do
    api_request GET "/api/v1/games?status=$status&gameType=PVP" "" "$token"
    if http_success; then
      combined=$(jq -s '.[0] + (.[1].content // [])' <(printf '%s' "$combined") <(printf '%s' "$API_BODY"))
    fi
  done
  local total
  total=$(jq 'length' <<<"$combined")
  if (( total == 0 )); then
    info "Brak aktywnych gier PvP dla $label"
    return
  fi
  warn "Znaleziono $total aktywnych gier PvP dla $label"
  local game_json
  while IFS= read -r game_json; do
    local gid
    gid=$(jq -r '.gameId' <<<"$game_json")
    info "Konczenie gry ID $gid"
    local ended=0
    local end_status
    for end_status in abandoned finished; do
      local body
      body=$(jq -n --arg status "$end_status" '{status:$status}')
      api_request PUT "/api/v1/games/$gid/status" "$body" "$token"
      if http_success; then
        success "Gra $gid zakonczona statusem $end_status"
        ended=1
        break
      fi
    done
    if [[ $ended -eq 0 ]]; then
      warn "Nie udalo sie zakonczyc gry $gid"
    fi
  done < <(jq -c '.[]' <<<"$combined")
}

remove_from_queue "$user1_token" "Gracz A"
remove_from_queue "$user2_token" "Gracz B"
end_active_pvp_games "$user1_token" "Gracz A"
end_active_pvp_games "$user2_token" "Gracz B"

challenge_body=$(jq -n '{boardSize:"THREE"}')
max_retries=2
retry=0
game_id=""

while (( retry < max_retries )); do
  section "KROK: Tworzenie gry PvP (proba $((retry+1)))..."
  api_request POST "/api/v1/matching/challenge/$user2_id" "$challenge_body" "$user1_token"
  if http_success; then
    game_id=$(jq -r '.gameId' <<<"$API_BODY")
    success "Gra PvP utworzona: ID $game_id"
    break
  fi
  if [[ $API_STATUS -eq 409 ]]; then
    msg=$(error_message)
    if [[ "$msg" == *"unavailable"* ]]; then
      warn "Gracz B niedostepny: $msg"
      end_active_pvp_games "$user2_token" "Gracz B"
      ((retry+=1))
      sleep 1
      continue
    fi
    if [[ "$msg" == *"active PvP game"* ]]; then
      warn "Gracz A ma aktywna gre: $msg"
      end_active_pvp_games "$user1_token" "Gracz A"
      ((retry+=1))
      sleep 1
      continue
    fi
  fi
  fail "Blad tworzenia gry PvP (HTTP $API_STATUS)\n$API_BODY"
done

if [[ -z "$game_id" ]]; then
  fail "Nie udalo sie utworzyc gry PvP"
fi

make_move() {
  local token=$1
  local gid=$2
  local row=$3
  local col=$4
  local symbol=$5
  local label=$6
  local body
  body=$(jq -n --argjson row "$row" --argjson col "$col" --arg symbol "$symbol" '{row:$row,col:$col,playerSymbol:$symbol}')
  info "Ruch $label: symbol '$symbol' na ($row,$col)"
  api_request POST "/api/v1/games/$gid/moves" "$body" "$token"
  if ! http_success; then
    return 1
  fi
  success "Ruch zaakceptowany: Move ID $(jq -r '.moveId' <<<"$API_BODY")"
  printf "\nStan planszy:\n"
  print_board <<<"$API_BODY"
  if jq -e '.winner != null' >/dev/null <<<"$API_BODY"; then
    info "Zwyciezca: $(jq -r '.winner.username' <<<"$API_BODY") (ID: $(jq -r '.winner.userId' <<<"$API_BODY"))"
  fi
  LAST_MOVE_BODY="$API_BODY"
  return 0
}

get_game_state() {
  local token=$1
  local gid=$2
  api_request GET "/api/v1/games/$gid" "" "$token"
  if http_success; then
    GAME_STATE="$API_BODY"
    return 0
  fi
  GAME_STATE=""
  return 1
}

section "KROK: Rozpoczecie rozgrywki PvP..."
move_rows=(1 0 0 0 1 1 2 2 2)
move_cols=(1 0 1 2 0 2 0 1 2)
move_index=0
attempts=0
max_attempts=20

while (( attempts < max_attempts && move_index < ${#move_rows[@]} )); do
  if ! get_game_state "$user1_token" "$game_id"; then
    fail "Nie mozna pobrac stanu gry (HTTP $API_STATUS)"
  fi
  status=$(jq -r '.status // ""' <<<"$GAME_STATE")
  if [[ "$status" == "FINISHED" || "$status" == "DRAW" ]]; then
    color "36" "Gra zakonczona ze statusem: $status"
    break
  fi
  player1_id=$(jq -r '.player1.userId // empty' <<<"$GAME_STATE")
  player2_id=$(jq -r '.player2.userId // empty' <<<"$GAME_STATE")
  current_symbol=$(jq -r '.currentPlayerSymbol // ""' <<<"$GAME_STATE")
  if [[ -z "$current_symbol" && -n "$LAST_MOVE_BODY" ]]; then
    current_symbol=$(jq -r '.currentPlayerSymbol // ""' <<<"$LAST_MOVE_BODY")
  fi
  if [[ -z "$current_symbol" && "$status" == "WAITING" ]]; then
    current_symbol="x"
  fi
  if [[ -z "$current_symbol" ]]; then
    warn "Nie mozna okreslic aktualnego gracza, pomijam ruch"
    ((move_index+=1))
    ((attempts+=1))
    sleep 0.5
    continue
  fi
  row=${move_rows[$move_index]}
  col=${move_cols[$move_index]}
  token_to_use=""
  label=""
  if [[ "$current_symbol" == "x" ]]; then
    if [[ "$player1_id" == "$user1_id" ]]; then
      token_to_use="$user1_token"
      label="Gracz A"
    else
      token_to_use="$user2_token"
      label="Gracz B"
    fi
  elif [[ "$current_symbol" == "o" ]]; then
    if [[ "$player2_id" == "$user2_id" ]]; then
      token_to_use="$user2_token"
      label="Gracz B"
    else
      token_to_use="$user1_token"
      label="Gracz A"
    fi
  else
    warn "Nieznany symbol $current_symbol"
    ((move_index++))
    ((attempts++))
    sleep 0.5
    continue
  fi
  if make_move "$token_to_use" "$game_id" "$row" "$col" "$current_symbol" "$label"; then
    game_status=$(jq -r '.gameStatus // ""' <<<"$LAST_MOVE_BODY")
    if [[ "$game_status" == "FINISHED" || "$game_status" == "DRAW" ]]; then
      color "36" "Gra zakonczona ze statusem: $game_status"
      break
    fi
    ((move_index+=1))
  else
    if [[ $API_STATUS -eq 403 ]]; then
      warn "Nie kolej gracza (403) dla pozycji ($row,$col)"
    else
      warn "Nieudany ruch ($row,$col) (HTTP $API_STATUS)"
    fi
    ((move_index+=1))
  fi
  ((attempts+=1))
  sleep 0.5
done

printf "\n"
color "36" "========================================"
color "36" "Test zakonczony"
color "36" "========================================"