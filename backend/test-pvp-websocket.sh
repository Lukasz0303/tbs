#!/usr/bin/env bash
set -euo pipefail

BASE_URL="${BASE_URL:-http://localhost:8080}"
BASE_URL="${BASE_URL%/}"
WS_PATH="${WS_PATH:-/api/ws/game}"
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
source "$SCRIPT_DIR/test-common.sh"
require_commands curl jq python3

color "36" "========================================"
color "36" "Test scenariusza gry PvP z WebSocket"
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

register_or_login_user "$user1_email" "$user1_password" "$user1_username" "Gracz A" user1_token user1_id
register_or_login_user "$user2_email" "$user2_password" "$user2_username" "Gracz B" user2_token user2_id

ws_scheme="ws"
ws_host="${BASE_URL#http://}"
if [[ "$BASE_URL" == https://* ]]; then
  ws_scheme="wss"
  ws_host="${BASE_URL#https://}"
fi

cleanup() {
  [[ -n "${ws1_pid:-}" ]] && kill "$ws1_pid" >/dev/null 2>&1 || true
  [[ -n "${ws2_pid:-}" ]] && kill "$ws2_pid" >/dev/null 2>&1 || true
  [[ -n "${ws1_log:-}" ]] && rm -f "$ws1_log"
  [[ -n "${ws2_log:-}" ]] && rm -f "$ws2_log"
  [[ -n "${ws1_stdout:-}" ]] && rm -f "$ws1_stdout"
  [[ -n "${ws2_stdout:-}" ]] && rm -f "$ws2_stdout"
}
trap cleanup EXIT

remove_from_queue() {
  local token=$1
  local label=$2
  api_request DELETE "/api/v1/matching/queue" "" "$token"
  if http_success; then
    info "$label usuniety z kolejki matchmaking"
    return
  fi
  if [[ $API_STATUS -eq 404 ]]; then
    info "$label nie byl w kolejce"
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
api_request POST "/api/v1/matching/challenge/$user2_id" "$challenge_body" "$user1_token"
ensure_ok "Blad tworzenia gry PvP"
game_id=$(jq -r '.gameId' <<<"$API_BODY")
success "Gra PvP utworzona: ID $game_id"

start_ws_listener() {
  local token=$1
  local label=$2
  local log_var=$3
  local pid_var=$4
  local stdout_var=$5
  local url="$ws_scheme://$ws_host$WS_PATH/$game_id?token=$token"
  local logfile
  logfile=$(mktemp)
  info "Laczenie WebSocket dla $label ($url)"
  local stdout_file="${logfile}.stdout"
  python3 "$SCRIPT_DIR/ws_client.py" --url "$url" --output "$logfile" --duration 120 --cookie-token "$token" >"$stdout_file" 2>&1 &
  local pid=$!
  sleep 1
  if ! kill -0 "$pid" >/dev/null 2>&1; then
    fail "Nie udalo sie uruchomic klienta WebSocket dla $label. Szczegoly: $(cat "$stdout_file")"
  fi
  printf -v "$log_var" "%s" "$logfile"
  printf -v "$pid_var" "%s" "$pid"
  printf -v "$stdout_var" "%s" "$stdout_file"
}

start_ws_listener "$user1_token" "Gracz A" ws1_log ws1_pid ws1_stdout
start_ws_listener "$user2_token" "Gracz B" ws2_log ws2_pid ws2_stdout

sleep 3

get_latest_ws_message() {
  local file=$1
  local type=$2
  python3 - "$file" "$type" <<'PY'
import json, sys
path, expected = sys.argv[1], sys.argv[2]
last = None
try:
    with open(path, encoding="utf-8") as handle:
        for line in handle:
            line = line.strip()
            if not line:
                continue
            try:
                data = json.loads(line)
            except json.JSONDecodeError:
                continue
            if data.get("type") == expected:
                last = data
except FileNotFoundError:
    last = None
if last is not None:
    print(json.dumps(last))
PY
}

assert_initial_ws_state() {
  local log=$1
  local label=$2
  if ws_msg=$(get_latest_ws_message "$log" "GAME_UPDATE"); then
    success "$label otrzymal GAME_UPDATE"
  else
    warn "$label nie otrzymal GAME_UPDATE"
  fi
}

assert_initial_ws_state "$ws1_log" "Gracz A"
assert_initial_ws_state "$ws2_log" "Gracz B"

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
  LAST_MOVE_BODY="$API_BODY"
  return 0
}

compare_states() {
  local rest=$1
  local ws=$2
  local source=$3
  [[ -z "$ws" ]] && return
  local rest_status ws_status rest_board ws_board rest_symbol ws_symbol
  rest_status=$(jq -r '.status // ""' <<<"$rest")
  ws_status=$(jq -r '.payload.status // ""' <<<"$ws")
  rest_board=$(jq -c '.boardState.state' <<<"$rest")
  ws_board=$(jq -c '.payload.boardState.state' <<<"$ws")
  rest_symbol=$(jq -r '.currentPlayerSymbol // ""' <<<"$rest")
  ws_symbol=$(jq -r '.payload.currentPlayerSymbol // ""' <<<"$ws")
  if [[ "$rest_status" == "$ws_status" ]]; then
    info "$source: status zgodny ($rest_status)"
  else
    warn "$source: status REST=$rest_status WS=$ws_status"
  fi
  if [[ "$rest_board" == "$ws_board" && "$rest_board" != "null" ]]; then
    info "$source: plansza zgodna"
  else
    warn "$source: plansza rozna"
  fi
  if [[ "$rest_symbol" == "$ws_symbol" ]]; then
    info "$source: currentPlayerSymbol zgodny ($rest_symbol)"
  else
    warn "$source: currentPlayerSymbol REST=$rest_symbol WS=$ws_symbol"
  fi
}

section "KROK: Rozpoczecie rozgrywki PvP..."
move_rows=(1 0 0 0 1 1 2 2 2)
move_cols=(1 0 1 2 0 2 0 1 2)
move_index=0
attempts=0
max_attempts=20

while (( attempts < max_attempts && move_index < ${#move_rows[@]} )); do
  api_request GET "/api/v1/games/$game_id" "" "$user1_token"
  ensure_ok "Blad pobierania stanu gry"
  game_state="$API_BODY"
  status=$(jq -r '.status // ""' <<<"$game_state")
  if [[ "$status" == "FINISHED" || "$status" == "DRAW" ]]; then
    color "36" "Gra zakonczona ze statusem: $status"
    break
  fi
  player1_id=$(jq -r '.player1.userId // empty' <<<"$game_state")
  player2_id=$(jq -r '.player2.userId // empty' <<<"$game_state")
  current_symbol=$(jq -r '.currentPlayerSymbol // ""' <<<"$game_state")
  if [[ -z "$current_symbol" ]]; then
    current_symbol="x"
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
  else
    if [[ "$player2_id" == "$user2_id" ]]; then
      token_to_use="$user2_token"
      label="Gracz B"
    else
      token_to_use="$user1_token"
      label="Gracz A"
    fi
  fi
  if make_move "$token_to_use" "$game_id" "$row" "$col" "$current_symbol" "$label"; then
    sleep 3
    api_request GET "/api/v1/games/$game_id" "" "$user1_token"
    ensure_ok "Blad pobierania stanu gry po ruchu"
    current_rest="$API_BODY"
    if ws_msg=$(get_latest_ws_message "$ws1_log" "MOVE_ACCEPTED"); then
      compare_states "$current_rest" "$ws_msg" "MOVE_ACCEPTED (Gracz A)"
    fi
    if ws_msg=$(get_latest_ws_message "$ws2_log" "OPPONENT_MOVE"); then
      compare_states "$current_rest" "$ws_msg" "OPPONENT_MOVE (Gracz B)"
    fi
    game_status=$(jq -r '.status // ""' <<<"$current_rest")
    if [[ "$game_status" == "FINISHED" || "$game_status" == "DRAW" ]]; then
      color "36" "Gra zakonczona ze statusem: $game_status"
      break
    fi
    ((move_index+=1))
  else
    warn "Nieudany ruch ($row,$col) (HTTP $API_STATUS)"
    ((move_index+=1))
  fi
  ((attempts+=1))
  sleep 0.5
done

printf "\n"
color "36" "========================================"
color "36" "Test zakonczony"
color "36" "========================================"