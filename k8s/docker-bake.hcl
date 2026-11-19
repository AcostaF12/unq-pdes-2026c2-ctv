# Images for the Kubernetes cluster, built from the repository root:
#   docker buildx bake -f k8s/docker-bake.hcl --load
# Backend and flights-service are tagged :latest, the tag k8s/kustomization.yaml uses, so a local
# build replaces the GHCR image in the cluster. The CI builds the same targets through scripts/ci-build-images.sh (cache and retry).

target "backend" {
  context = "backend"
  tags    = ["ghcr.io/acostaf12/ctv-backend:latest"]
}

target "flights-service" {
  context = "flights-service"
  tags    = ["ghcr.io/acostaf12/ctv-flights-service:latest"]
}

# The browser runs outside the cluster and reaches the backend through the host port 8080.
target "frontend" {
  context = "frontend"
  args = {
    VITE_API_BASE_URL = "http://localhost:8080"
  }
  tags = ["ctv-frontend:local"]
}

group "default" {
  targets = ["backend", "flights-service", "frontend"]
}
