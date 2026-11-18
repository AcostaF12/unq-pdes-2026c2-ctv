#!/usr/bin/env bash
# Corre un escenario de k6 y agrega su resumen en Markdown al resumen del job de GitHub Actions.
# Uso: bash load-tests/run.sh <escenario>   (BASE_URL define el target, por defecto http://localhost:8080)
set -u

scenario="$1"
summary_file="${RUNNER_TEMP:-/tmp}/k6-${scenario}.md"
script_dir="$(cd "$(dirname "$0")" && pwd)"

status=0
k6 run --quiet -e BASE_URL="${BASE_URL:-http://localhost:8080}" -e SUMMARY_FILE="$summary_file" \
  "$script_dir/${scenario}.js" || status=$?

if [ -n "${GITHUB_STEP_SUMMARY:-}" ]; then
  if [ -f "$summary_file" ]; then
    cat "$summary_file" >> "$GITHUB_STEP_SUMMARY"
  else
    printf '### %s: sin resumen (k6 terminó con código %s)\n' "$scenario" "$status" >> "$GITHUB_STEP_SUMMARY"
  fi
fi

exit "$status"
