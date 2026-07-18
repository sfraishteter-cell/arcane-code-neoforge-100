#!/usr/bin/env bash
set -euo pipefail

timeout_seconds="${ARCANE_SERVER_TIMEOUT_SECONDS:-480}"
log_file="build/reports/dedicated-server.log"

mkdir -p run build/reports
printf 'eula=true\n' > run/eula.txt
printf 'online-mode=false\nserver-port=0\nmotd=Arcane Code CI smoke test\n' > run/server.properties
: > "$log_file"

./gradlew --no-daemon runServer > "$log_file" 2>&1 &
server_pid=$!

cleanup() {
  if kill -0 "$server_pid" 2>/dev/null; then
    kill "$server_pid" 2>/dev/null || true
  fi
  wait "$server_pid" 2>/dev/null || true
}
trap cleanup EXIT INT TERM

deadline=$((SECONDS + timeout_seconds))
while (( SECONDS < deadline )); do
  if grep -Eq 'Done \([0-9.]+s\)! For help' "$log_file"; then
    if grep -Eq '\[[^]]+/ERROR\]|(^|[^A-Za-z])Exception([^A-Za-z]|$)' "$log_file"; then
      echo 'Dedicated server reached ready state but logged an error:' >&2
      cat "$log_file" >&2
      exit 1
    fi
    echo "Dedicated server reached ready state without logged errors. Log: $log_file"
    exit 0
  fi

  if ! kill -0 "$server_pid" 2>/dev/null; then
    echo 'Dedicated server stopped before reaching ready state:' >&2
    cat "$log_file" >&2
    exit 1
  fi

  sleep 5
done

echo "Dedicated server did not become ready within ${timeout_seconds} seconds:" >&2
cat "$log_file" >&2
exit 1
