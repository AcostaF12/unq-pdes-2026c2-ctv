# Images for the Kubernetes cluster, built from the repository root:
#   docker buildx bake -f k8s/docker-bake.hcl --load
# The CI builds the same targets through scripts/ci-build-images.sh (cache and retry).

target "backend" {
  context = "backend"
  tags    = ["ghcr.io/acostaf12/ctv-backend:local"]
}

target "flights-service" {
  context = "flights-service"
  tags    = ["ghcr.io/acostaf12/ctv-flights-service:local"]
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
