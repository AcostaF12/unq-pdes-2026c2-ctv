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

# En el runner, Prometheus scrapea los dos servicios. En la VM el puerto no está publicado.
if [ -n "${PROMETHEUS_URL:-}" ] && [ -n "${GITHUB_STEP_SUMMARY:-}" ]; then
  prom_p95() {
    local query="$1"
    local body
    if ! body=$(curl -sfG --max-time 10 "${PROMETHEUS_URL%/}/api/v1/query" --data-urlencode "query=${query}"); then
      printf '%s' "sin datos (Prometheus no respondió)"
      return 0
    fi
    printf '%s' "$body" | python3 -c '
import json, math, sys
try:
    body = json.load(sys.stdin)
    result = body.get("data", {}).get("result") or []
except (json.JSONDecodeError, AttributeError):
    print("sin datos (respuesta invalida)", end="")
    raise SystemExit(0)
if not result:
    print("sin muestras", end="")
    raise SystemExit(0)
raw = result[0]["value"][1]
try:
    value = float(raw)
except (TypeError, ValueError):
    print("sin muestras", end="")
    raise SystemExit(0)
if math.isnan(value):
    print("sin muestras", end="")
else:
    print(f"{value * 1000:.1f} ms", end="")
'
  }

  packages=$(prom_p95 'histogram_quantile(0.95, sum by (le) (rate(http_server_requests_seconds_bucket{job="backend",uri="/packages",method="GET"}[5m])))')
  purchases=$(prom_p95 'histogram_quantile(0.95, sum by (le) (rate(http_server_requests_seconds_bucket{job="backend",uri="/purchases",method="POST"}[5m])))')
  cat >> "$GITHUB_STEP_SUMMARY" <<EOF

#### Prometheus (backend, p95 sobre los últimos 2 minutos)

| Endpoint | p95 |
|----------|-----|
| \`GET /packages\` | ${packages} |
| \`POST /purchases\` | ${purchases} |

EOF
fi

exit "$status"
