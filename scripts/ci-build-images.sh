#!/usr/bin/env bash
# Builds the images of a Compose file with Buildx Bake before `docker compose up --no-build`.
# Layers are shared between runs through the GitHub Actions cache, so Gradle and npm only
# download dependencies when the build scripts change. A bounded retry absorbs transient
# registry errors such as HTTP 429 from Maven Central.
#
# Usage: scripts/ci-build-images.sh <compose file> <compose project name>
# Requires the GitHub Actions runtime variables (crazy-max/ghaction-github-runtime).
set -euo pipefail

file=$1
project=$2
attempts=${BUILD_ATTEMPTS:-3}
# Bake resolves build contexts from the working directory, so callers run this script next
# to the Compose file; reading the whole repository is allowed for contexts like ../backend.
root=$(cd "$(dirname "$0")/.." && pwd)

targets=$(docker buildx bake -f "$file" --print 2>/dev/null | jq -r '.target | keys[]')
args=(--allow "fs.read=$root")
for target in $targets; do
  # Compose looks for <project>-<service> when the service has no image name.
  args+=(--set "$target.tags=$project-$target"
         --set "$target.cache-from=type=gha,scope=$target"
         --set "$target.cache-to=type=gha,scope=$target,mode=max")
done

for attempt in $(seq 1 "$attempts"); do
  if docker buildx bake -f "$file" --load "${args[@]}"; then
    exit 0
  fi
  if [ "$attempt" -lt "$attempts" ]; then
    echo "::warning::Image build failed (attempt $attempt/$attempts). Retrying in $((attempt * 30))s."
    sleep $((attempt * 30))
  fi
done
echo "::error::Image build failed after $attempts attempts."
exit 1
