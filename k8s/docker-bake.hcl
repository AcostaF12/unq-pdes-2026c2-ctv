# Imágenes para el cluster de Kubernetes, construidas desde la raíz del repositorio:
#   docker buildx bake -f k8s/docker-bake.hcl --load
# Bake construye los tres targets en paralelo con una sola orden.
# Backend y flights-service se etiquetan :latest, el tag que usa k8s/kustomization.yaml, así un
# build local reemplaza la imagen de GHCR en el cluster. El CI construye los mismos targets con
# scripts/ci-build-images.sh (caché y reintentos).

# Cada target es una imagen: context es la carpeta con su Dockerfile.
target "backend" {
  context = "backend"
  tags    = ["ghcr.io/acostaf12/ctv-backend:latest"]
}

target "flights-service" {
  context = "flights-service"
  tags    = ["ghcr.io/acostaf12/ctv-flights-service:latest"]
}

# El navegador corre fuera del cluster y llega al backend por el puerto 8080 del host, así que
# la URL de la API se fija en tiempo de build (Vite la incrusta en el JavaScript).
target "frontend" {
  context = "frontend"
  args = {
    VITE_API_BASE_URL = "http://localhost:8080"
  }
  tags = ["ctv-frontend:local"]
}

# Lo que se construye con `docker buildx bake` sin nombrar un target.
group "default" {
  targets = ["backend", "flights-service", "frontend"]
}
