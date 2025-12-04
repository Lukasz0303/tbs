#!/usr/bin/env bash

RESET=$'\033[0m'
COLOR_CYAN=$'\033[36m'
COLOR_GREEN=$'\033[32m'
COLOR_YELLOW=$'\033[33m'
COLOR_RED=$'\033[31m'

API_BODY=""
API_STATUS=0

color() {
  local code="$1"
  shift
  printf "%b%s%b\n" "\033[${code}m" "$*" "$RESET"
}

section() {
  printf "\n"
  color "36" "--- $* ---"
}

info() {
  color "36" "$*"
}

success() {
  color "32" "$*"
}

warn() {
  color "33" "$*"
}

fail() {
  color "31" "BLAD: $*"
  exit 1
}

require_commands() {
  local missing=()
  for cmd in "$@"; do
    if ! command -v "$cmd" >/dev/null 2>&1; then
      missing+=("$cmd")
    fi
  done
  if [[ ${#missing[@]} -gt 0 ]]; then
    fail "Brakujace komendy: ${missing[*]}"
  fi
}

api_request() {
  local method="$1"
  local path="$2"
  local body="$3"
  local token="$4"
  
  local url="${BASE_URL}${path}"
  local headers=()
  
  if [[ -n "$token" ]]; then
    headers+=("-H" "Authorization: Bearer $token")
  fi
  
  headers+=("-H" "Content-Type: application/json")
  
  local response
  local status
  
  if [[ -n "$body" && "$body" != "" ]]; then
    response=$(curl -s -w "\n%{http_code}" -X "$method" "${headers[@]}" -d "$body" "$url" 2>/dev/null || echo -e "\n000")
  else
    response=$(curl -s -w "\n%{http_code}" -X "$method" "${headers[@]}" "$url" 2>/dev/null || echo -e "\n000")
  fi
  
  API_BODY=$(echo "$response" | head -n -1)
  API_STATUS=$(echo "$response" | tail -n 1)
  
  if [[ "$API_STATUS" == "000" ]]; then
    API_STATUS=500
    API_BODY='{"error":"Connection failed"}'
  fi
}

http_success() {
  [[ $API_STATUS -ge 200 && $API_STATUS -lt 300 ]]
}

ensure_ok() {
  if ! http_success; then
    local msg="$1"
    local error_msg
    error_msg=$(error_message)
    if [[ -n "$error_msg" ]]; then
      fail "$msg (HTTP $API_STATUS): $error_msg"
    else
      fail "$msg (HTTP $API_STATUS): $API_BODY"
    fi
  fi
}

error_message() {
  if [[ -z "$API_BODY" ]]; then
    return
  fi
  local msg
  msg=$(echo "$API_BODY" | jq -r '.message // .error // ""' 2>/dev/null || echo "")
  if [[ -z "$msg" || "$msg" == "null" ]]; then
    echo "$API_BODY" | jq -r '. // ""' 2>/dev/null || echo ""
  else
    echo "$msg"
  fi
}

register_or_login_user() {
  local email="$1"
  local password="$2"
  local username="$3"
  local display_name="$4"
  local token_var="$5"
  local user_id_var="$6"
  
  api_request POST "/api/v1/auth/login" "$(jq -n --arg email "$email" --arg password "$password" '{email:$email,password:$password}')" ""
  
  if http_success; then
    local token
    local user_id
    token=$(jq -r '.token' <<<"$API_BODY")
    user_id=$(jq -r '.userId' <<<"$API_BODY")
    printf -v "$token_var" "%s" "$token"
    printf -v "$user_id_var" "%s" "$user_id"
    success "Zalogowano uzytkownika: $display_name (ID: $user_id)"
    return 0
  fi
  
  if [[ $API_STATUS -eq 401 || $API_STATUS -eq 404 ]]; then
    info "Uzytkownik nie istnieje, rejestracja..."
    api_request POST "/api/v1/auth/register" "$(jq -n --arg email "$email" --arg password "$password" --arg username "$username" '{email:$email,password:$password,username:$username}')" ""
    
    if http_success; then
      local token
      local user_id
      token=$(jq -r '.token' <<<"$API_BODY")
      user_id=$(jq -r '.userId' <<<"$API_BODY")
      printf -v "$token_var" "%s" "$token"
      printf -v "$user_id_var" "%s" "$user_id"
      success "Zarejestrowano uzytkownika: $display_name (ID: $user_id)"
      return 0
    fi
  fi
  
  fail "Nie udalo sie zalogowac ani zarejestrowac uzytkownika (HTTP $API_STATUS): $(error_message)"
}

print_board() {
  local json_input
  json_input=$(cat)
  
  if [[ -z "$json_input" ]]; then
    warn "Brak danych do wyswietlenia planszy"
    return
  fi
  
  local board_state
  board_state=$(echo "$json_input" | jq -r '.boardState.state // empty' 2>/dev/null)
  
  if [[ -z "$board_state" || "$board_state" == "null" ]]; then
    warn "Brak stanu planszy w odpowiedzi"
    return
  fi
    
  local size
  size=$(echo "$board_state" | jq 'length' 2>/dev/null)
  
  if [[ -z "$size" || "$size" == "0" ]]; then
    warn "Nieprawidlowy rozmiar planszy"
    return
  fi
  
  local row col cell
  for ((row=0; row<size; row++)); do
    local row_data
    row_data=$(echo "$board_state" | jq -r ".[$row] | @tsv" 2>/dev/null)
    if [[ -z "$row_data" ]]; then
      continue
    fi
    local cells=()
    IFS=$'\t' read -r -a cells <<<"$row_data"
    for i in "${!cells[@]}"; do
      [[ -z "${cells[$i]}" || "${cells[$i]}" == "null" ]] && cells[$i]="."
    done
    
    local row_str=""
    for ((col=0; col<size; col++)); do
      local cell_val="${cells[$col]:-.}"
      if [[ "$cell_val" == "." ]]; then
        row_str+=" . "
      else
        row_str+=" $cell_val "
      fi
      if [[ $col -lt $((size-1)) ]]; then
        row_str+="|"
      fi
    done
    printf "%s\n" "$row_str"
    if [[ $row -lt $((size-1)) ]]; then
      local separator=""
      for ((col=0; col<size; col++)); do
        separator+="---"
        if [[ $col -lt $((size-1)) ]]; then
          separator+="+"
        fi
      done
      printf "%s\n" "$separator"
    fi
  done
}

