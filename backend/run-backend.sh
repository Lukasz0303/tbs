#!/usr/bin/env bash
set -euo pipefail

ACTION="${1:-start}"
RESET=$'\033[0m'
COLOR_CYAN=$'\033[36m'
COLOR_GREEN=$'\033[32m'
COLOR_YELLOW=$'\033[33m'
COLOR_RED=$'\033[31m'
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
BACKEND_DIR="$SCRIPT_DIR"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"
LOG_FILE="$BACKEND_DIR/application.log"
PID_FILE="$BACKEND_DIR/backend.pid"
JAVA_SEARCH_PATHS=("/Library/Java/JavaVirtualMachines" "/Users/$(whoami)/.sdkman/candidates/java" "/opt/homebrew/opt")

log_line() {
  local color="$1"
  shift
  printf "%b%s%b\n" "$color" "$*" "$RESET"
}

info() { log_line "$COLOR_CYAN" "$*"; }
ok() { log_line "$COLOR_GREEN" "$*"; }
warn() { log_line "$COLOR_YELLOW" "$*"; }
error() { log_line "$COLOR_RED" "$*"; }

command_exists() {
  command -v "$1" >/dev/null 2>&1
}

find_java_home() {
  if [ -n "${JAVA_HOME:-}" ] && [ -x "$JAVA_HOME/bin/java" ]; then
    if "$JAVA_HOME/bin/java" -version 2>&1 | grep -q '"21'; then
      echo "$JAVA_HOME"
      return 0
    fi
  fi
  if command_exists /usr/libexec/java_home; then
    local candidate
    candidate=$(/usr/libexec/java_home -v 21 2>/dev/null || true)
    if [ -n "$candidate" ] && [ -x "$candidate/bin/java" ]; then
      echo "$candidate"
      return 0
    fi
  fi
  for base in "${JAVA_SEARCH_PATHS[@]}"; do
    if [ -d "$base" ]; then
      local match
      match=$(find "$base" -maxdepth 2 -type d -name "*21*" 2>/dev/null | head -n1 || true)
      if [ -n "$match" ] && [ -x "$match/bin/java" ]; then
        if "$match/bin/java" -version 2>&1 | grep -q '"21'; then
          echo "$match"
          return 0
        fi
      fi
    fi
  done
  return 1
}

set_java_environment() {
  local home
  home=$(find_java_home || true)
  if [ -z "$home" ]; then
    error "Nie znaleziono Java 21"
    exit 1
  fi
  export JAVA_HOME="$home"
  export PATH="$JAVA_HOME/bin:$PATH"
  ok "JAVA_HOME: $JAVA_HOME"
}

test_port() {
  nc -z localhost "$1" >/dev/null 2>&1
}

wait_for_port() {
  local port="$1"
  local name="$2"
  local timeout="${3:-30}"
  info "Oczekiwanie na $name na porcie $port"
  local elapsed=0
  while [ $elapsed -lt $timeout ]; do
    if test_port "$port"; then
      ok "$name dziala na porcie $port"
      return 0
    fi
    sleep 2
    elapsed=$((elapsed + 2))
  done
  error "Timeout oczekiwania na $name"
  return 1
}

start_supabase() {
  info "Sprawdzanie Supabase"
  if ! command_exists npx; then
    error "npx nie jest dostepne"
    return 1
  fi
  local status_output
  pushd "$PROJECT_ROOT" >/dev/null
  status_output=$(npx --yes supabase status 2>&1 || true)
  popd >/dev/null
  if echo "$status_output" | grep -qi "running"; then
    ok "Supabase juz dziala"
  else
    info "Uruchamianie Supabase"
    pushd "$PROJECT_ROOT" >/dev/null
    npx --yes supabase start
    popd >/dev/null
  fi
  if ! wait_for_port 54322 "PostgreSQL" 60; then
    warn "PostgreSQL nie odpowiada, ponawiam start Supabase"
    pushd "$PROJECT_ROOT" >/dev/null
    npx --yes supabase stop >/dev/null 2>&1 || true
    npx --yes supabase start
    popd >/dev/null
    wait_for_port 54322 "PostgreSQL" 60
  fi
}

apply_migrations() {
  info "Stosowanie migracji Supabase"
  pushd "$PROJECT_ROOT" >/dev/null
  npx --yes supabase db reset || warn "Brak nowych migracji lub blad CLI"
  popd >/dev/null
}

start_redis() {
  info "Sprawdzanie Redis"
  if test_port 6379; then
    ok "Redis juz dziala"
    return 0
  fi
  if ! command_exists docker; then
    warn "Docker nie jest dostepny"
    return 1
  fi
  if docker ps -a --format '{{.Names}}' | grep -q '^waw-redis$'; then
    docker start waw-redis >/dev/null
  else
    docker run --name waw-redis -p 6379:6379 -d redis:7 >/dev/null
  fi
  sleep 3
  if test_port 6379; then
    ok "Redis uruchomiony"
    return 0
  fi
  warn "Redis nie odpowiada"
  return 1
}

build_backend() {
  info "Budowanie backendu"
  pushd "$BACKEND_DIR" >/dev/null
  ./gradlew clean
  ./gradlew build -x test
  popd >/dev/null
}

wait_for_application_start() {
  local timeout="${1:-60}"
  local elapsed=0
  info "Monitorowanie logow aplikacji"
  while [ $elapsed -lt $timeout ]; do
    if [ -f "$LOG_FILE" ] && grep -q "Started TbsApplication" "$LOG_FILE"; then
      ok "Backend uruchomiony"
      return 0
    fi
    if [ -f "$PID_FILE" ]; then
      local pid
      pid=$(cat "$PID_FILE")
      if ! ps -p "$pid" >/dev/null 2>&1; then
        error "Proces backendu zakonczyl sie"
        return 1
      fi
    fi
    sleep 2
    elapsed=$((elapsed + 2))
  done
  warn "Nie znaleziono potwierdzenia startu w logach"
  return 0
}

test_application_health() {
  local attempts="${1:-5}"
  local count=1
  while [ $count -le $attempts ]; do
    if curl -sf "http://localhost:8080/v3/api-docs" >/dev/null; then
      ok "Aplikacja odpowiada"
      return 0
    fi
    sleep 2
    count=$((count + 1))
  done
  warn "Endpoint zdrowia nie odpowiada"
  return 1
}

clear_redis_cache() {
  info "Czyszczenie cache rankingu"
  curl -sf -X DELETE "http://localhost:8080/api/v1/rankings/cache" >/dev/null || true
}

start_backend() {
  info "Uruchamianie backendu"
  set_java_environment
  pushd "$BACKEND_DIR" >/dev/null
  : > "$LOG_FILE"
  ./gradlew bootRun >"$LOG_FILE" 2>&1 &
  local pid=$!
  echo "$pid" >"$PID_FILE"
  popd >/dev/null
  wait_for_application_start 60
  wait_for_port 8080 "Backend" 60
  test_application_health 10
}

stop_backend() {
  info "Zatrzymywanie backendu"
  if [ -f "$PID_FILE" ]; then
    local pid
    pid=$(cat "$PID_FILE")
    if ps -p "$pid" >/dev/null 2>&1; then
      kill "$pid" >/dev/null 2>&1 || true
      wait "$pid" 2>/dev/null || true
    fi
    rm -f "$PID_FILE"
  fi
  local pids
  pids=$(lsof -t -i:8080 2>/dev/null || true)
  if [ -n "${pids:-}" ]; then
    kill $pids >/dev/null 2>&1 || true
  fi
}

show_logs() {
  if [ -f "$LOG_FILE" ]; then
    tail -n 50 "$LOG_FILE"
  else
    warn "Plik logow nie istnieje"
  fi
}

show_status() {
  info "Supabase"
  if test_port 54322; then ok "PostgreSQL dziala"; else warn "PostgreSQL zatrzymany"; fi
  info "Redis"
  if test_port 6379; then ok "Redis dziala"; else warn "Redis zatrzymany"; fi
  info "Backend"
  if test_port 8080; then ok "Backend dziala"; else warn "Backend zatrzymany"; fi
  if [ -n "${JAVA_HOME:-}" ]; then ok "JAVA_HOME: $JAVA_HOME"; fi
}

run_start() {
  start_supabase || exit 1
  apply_migrations
  start_redis || true
  build_backend || exit 1
  start_backend || exit 1
  clear_redis_cache
  ok "Backend uruchomiony"
  info "API: http://localhost:8080"
  info "Swagger UI: http://localhost:8080/swagger-ui/index.html"
}

run_restart() {
  stop_backend || true
  run_start
}

case "$ACTION" in
  start)
    run_start
    ;;
  restart)
    run_restart
    ;;
  logs)
    show_logs
    ;;
  stop)
    stop_backend
    ;;
  status)
    show_status
    ;;
  *)
    error "Nieznana akcja: $ACTION"
    exit 1
    ;;
esac

