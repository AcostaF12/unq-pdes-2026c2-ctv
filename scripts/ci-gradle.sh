#!/usr/bin/env bash
# Runs the Gradle wrapper of the current directory and retries only when the failure looks like a
# transient repository error (HTTP 408/429/5xx, timeouts, reset connections). Gradle already
# retries each request a few times, but once those retries are exhausted it disables the
# repository for the rest of the build, so a rate limit from Maven Central fails the whole job.
# Compilation errors, failing tests or lint violations fail immediately without retrying.
#
# Usage (from a module directory): ../scripts/ci-gradle.sh <gradle arguments...>
set -euo pipefail

attempts=${GRADLE_ATTEMPTS:-3}
delay=${GRADLE_RETRY_DELAY:-30}
transient='Received status code (408|429|5[0-9][0-9])|Read timed out|Connect(ion)? timed out|Connection reset|Remote host terminated the handshake|No route to host'
log=$(mktemp)
trap 'rm -f "$log"' EXIT

for attempt in $(seq 1 "$attempts"); do
  status=0
  ./gradlew "$@" 2>&1 | tee "$log" || status=$?
  if [ "$status" -eq 0 ]; then
    exit 0
  fi
  if ! grep -Eq "$transient" "$log"; then
    exit "$status"
  fi
  if [ "$attempt" -lt "$attempts" ]; then
    echo "::warning::Gradle hit a transient repository error (attempt $attempt/$attempts). Retrying in $((attempt * delay))s."
    sleep $((attempt * delay))
  fi
done
echo "::error::Gradle kept failing with transient repository errors after $attempts attempts."
exit "$status"
