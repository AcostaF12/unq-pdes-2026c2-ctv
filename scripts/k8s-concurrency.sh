#!/usr/bin/env bash
# Corre la suite corta de concurrencia contra el Service del cluster (2 réplicas de backend).
# Misma carga que `npm run test:stress`: no sobrevender, y devolver cada asiento una sola vez.
# Si un escenario falla, los demás corren igual. El job queda en rojo si alguno falló.
# Cada resumen de k6 queda en $SUMMARY_DIR para el resumen del job.
set -u

BASE_URL=${BASE_URL:-http://localhost:8080}
DIR=${SUMMARY_DIR:-k8s-summary}
NAMESPACE=${NAMESPACE:-ctv}
mkdir -p "$DIR"
: > "$DIR/concurrency.tsv"

if command -v kubectl >/dev/null 2>&1; then
  kubectl -n "$NAMESPACE" get deployment backend -o jsonpath='{.status.readyReplicas}' > "$DIR/backend-ready" 2>/dev/null || true
fi

# name, then KEY=VALUE pairs passed to k6. El orden es el de la tabla del resumen.
scenarios=(
  "last-seat-stress|SEATS=5|BUYERS=12"
  "purchase-throughput|SEATS=8|BUYERS=6"
  "cancel-rebuy|SEATS=5"
  "double-cancel|ATTEMPTS=20"
)

failed=0
for entry in "${scenarios[@]}"; do
  IFS='|' read -r name _rest <<<"$entry"
  args=(-e "BASE_URL=${BASE_URL}" -e "SUMMARY_FILE=${DIR}/${name}.md")
  rest=${entry#*|}
  IFS='|' read -r -a pairs <<<"$rest"
  for pair in "${pairs[@]}"; do
    [ -n "$pair" ] || continue
    args+=(-e "$pair")
  done

  echo "::group::${name}"
  status=0
  k6 run --quiet "${args[@]}" "load-tests/${name}.js" || status=$?
  echo "::endgroup::"

  if [ "$status" -eq 0 ]; then
    printf '%s\tok\n' "$name" >> "$DIR/concurrency.tsv"
  else
    printf '%s\tfail\n' "$name" >> "$DIR/concurrency.tsv"
    echo "::error::${name} no cumplió sus umbrales (k6 terminó con código ${status})."
    failed=1
  fi
done

exit "$failed"
