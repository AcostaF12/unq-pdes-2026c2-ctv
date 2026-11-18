#!/bin/sh
# Creates (or updates) the Kibana data view for the application logs and marks it
# as default. Idempotent: the data view has a fixed id and is created with override.
set -eu

KIBANA_URL="${KIBANA_URL:-http://kibana:5601}"
DATA_VIEW_ID="${DATA_VIEW_ID:-ctv-logs}"
DATA_VIEW_PATTERN="${DATA_VIEW_PATTERN:-ctv-logs-*}"

echo "Waiting for Kibana at ${KIBANA_URL}..."
attempts=0
until curl -fsS "${KIBANA_URL}/api/status" 2>/dev/null | grep -q '"level":"available"'; do
  attempts=$((attempts + 1))
  if [ "${attempts}" -ge 60 ]; then
    echo "Kibana did not become available in time" >&2
    exit 1
  fi
  sleep 5
done

echo "Creating data view ${DATA_VIEW_PATTERN} (id ${DATA_VIEW_ID})..."
curl -fsS -X POST "${KIBANA_URL}/api/data_views/data_view" \
  -H 'kbn-xsrf: true' -H 'Content-Type: application/json' \
  -d "{\"override\":true,\"data_view\":{\"id\":\"${DATA_VIEW_ID}\",\"name\":\"CTV logs\",\"title\":\"${DATA_VIEW_PATTERN}\",\"timeFieldName\":\"@timestamp\"}}" \
  >/dev/null

echo "Setting ${DATA_VIEW_ID} as the default data view..."
curl -fsS -X POST "${KIBANA_URL}/api/data_views/default" \
  -H 'kbn-xsrf: true' -H 'Content-Type: application/json' \
  -d "{\"data_view_id\":\"${DATA_VIEW_ID}\",\"force\":true}" \
  >/dev/null

echo "Kibana data view ready."
