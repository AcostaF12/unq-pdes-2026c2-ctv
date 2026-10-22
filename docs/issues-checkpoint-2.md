# Issues del Checkpoint 2

Lista lista para pegar en GitHub con los puntos de la rúbrica que siguen abiertos: administración de paquetes y vuelos, selectores de vuelos para agencias, entrega continua a Docker Hub, e2e de administración y documentación. Incluye los comandos `gh issue create` exactos (no se ejecutaron). Repositorio: `AcostaF12/unq-pdes-2026c2-ctv`.

Antes de crear los issues hay que tener las etiquetas. Los comandos de la sección "Etiquetas" las crean y son idempotentes con `--force`.

## 1. feat(admin): gestionar paquetes de cualquier agencia

**Etiquetas:** `enhancement`, `backend`, `frontend`, `admin`

## Contexto
Hoy solo una agencia puede crear, editar y eliminar sus propios paquetes. El rol administrador tiene que poder cargar paquetes en nombre de cualquier agencia.

## Alcance
- Backend: el administrador puede crear, editar y eliminar paquetes indicando la agencia propietaria; la agencia sigue limitada a sus paquetes.
- Frontend: pantalla de administración con listado de paquetes de todas las agencias, formulario con selector de agencia y confirmación de baja.
- Contrato documentado en Swagger.

## Criterios de aceptación
- [ ] Un administrador crea un paquete para una agencia elegida y aparece en el catálogo y en el panel de esa agencia.
- [ ] Un administrador edita y elimina paquetes de cualquier agencia.
- [ ] Una agencia no puede crear ni modificar paquetes de otra agencia (403).
- [ ] Un comprador no ve ni puede usar estas rutas (403 en la API, "Sin permiso" en la interfaz).
- [ ] Tests unitarios/de integración del backend y tests del frontend para los tres roles.
- [ ] Pasa el Quality Gate de Sonar.

## 2. feat(admin): cargar y gestionar vuelos

**Etiquetas:** `enhancement`, `backend`, `frontend`, `flights-service`, `admin`

## Contexto
Los vuelos solo existen como datos de ejemplo. El administrador tiene que poder cargarlos para poder armar paquetes nuevos.

## Alcance
- flights-service: alta (y baja o edición si corresponde) de vuelos con origen, destino, fecha, precio y asientos disponibles; validaciones de datos.
- Backend: endpoint restringido a administradores que delega en flights-service con la protección de Resilience4j existente.
- Frontend: formulario y listado de vuelos en la sección de administración.

## Criterios de aceptación
- [ ] Un administrador carga un vuelo desde la interfaz y queda persistido en la base de flights-service.
- [ ] Los datos inválidos (fechas incoherentes, asientos negativos, origen igual a destino) se rechazan con un error claro.
- [ ] Solo el administrador accede a la carga de vuelos (403 para otros roles).
- [ ] Tests en flights-service, backend y frontend.
- [ ] Documentado en Swagger y en `docs/admin.md`.

## 3. feat(agency): selector de vuelos al armar un paquete

**Etiquetas:** `enhancement`, `frontend`, `backend`

## Contexto
Al crear un paquete hay que indicar el vuelo de ida y el de vuelta. Ingresar identificadores a mano es propenso a errores.

## Alcance
- Selector de vuelos en el formulario de paquetes de la agencia y del administrador, filtrado por origen/destino y fecha.
- Si falta, un endpoint de consulta de vuelos disponibles en el backend.

## Criterios de aceptación
- [ ] El formulario permite buscar y elegir vuelo de ida y de vuelta sin escribir identificadores.
- [ ] El selector solo ofrece vuelos coherentes (el de vuelta sale después de la ida y el origen/destino se corresponden con el paquete).
- [ ] Sin resultados, muestra un estado vacío; ante un error de red, permite reintentar.
- [ ] Tests del componente y del hook de consulta.

## 4. ci(cd): publicar imágenes en Docker Hub

**Etiquetas:** `ci`, `devops`, `documentation`

## Contexto
La rúbrica del Checkpoint 2 pide entrega continua con imágenes en Docker Hub. Hoy solo se publica en GHCR.

## Alcance
- Los workflows de backend, flights-service y frontend publican también `<namespace>/ctv-<servicio>` en Docker Hub con las etiquetas `latest` y el SHA del commit.
- Misma condición que hoy: solo en push a `main`, después de que pasan los controles del módulo (incluido el Quality Gate de Sonar); nunca en PR.
- Secrets `DOCKERHUB_USERNAME` y `DOCKERHUB_TOKEN`; variable opcional `DOCKERHUB_NAMESPACE`.
- `docker-compose.hub.yml` para levantar todo el stack desde Docker Hub.
- `actionlint` y `scripts/check-ci.rb` siguen pasando.

## Criterios de aceptación
- [ ] Un push a `main` deja las tres imágenes en Docker Hub con `latest` y el SHA.
- [ ] Un PR no publica ni necesita los secrets.
- [ ] Sin los secrets, el job de publicación en `main` falla con un mensaje claro.
- [ ] `docker compose -f docker-compose.hub.yml up -d --wait` levanta la aplicación sin compilar.
- [ ] El README documenta las imágenes y la configuración necesaria.

## 5. test(e2e): cubrir la sección de administración

**Etiquetas:** `test`, `frontend`

## Contexto
Los tests de Cypress cubren login, búsqueda, hoteles y compras. Falta cubrir la administración de paquetes y vuelos.

## Alcance
- Cypress: el administrador carga un vuelo, crea un paquete para una agencia, lo edita y lo elimina.
- Cypress: un comprador y una agencia no acceden a las rutas de administración.
- Cucumber (opcional): reglas de autorización de las APIs de administración.

## Criterios de aceptación
- [ ] Los escenarios corren en el CI (`e2e.yml`) sobre el entorno Docker aislado.
- [ ] Los tests no dependen del orden de ejecución ni dejan datos que rompan otros escenarios.
- [ ] `scripts/check-ci.rb` sigue pasando.

## 6. docs: documentar la sección de administración

**Etiquetas:** `documentation`

## Contexto
El README y la demo deben explicar lo que puede hacer un administrador.

## Alcance
- `docs/admin.md` con el login de administrador y las funciones disponibles.
- Enlaces desde el README y desde la sección de administrador de `docs/demo.md`.
- Verificar pantallas y rutas una vez mergeadas las funcionalidades y quitar el TODO del documento.

## Criterios de aceptación
- [ ] `docs/admin.md` coincide con lo que muestra la aplicación.
- [ ] El README y `docs/demo.md` lo enlazan.
- [ ] Swagger refleja los endpoints de administración.

## 7. docs: completar el README para la entrega (Docker Hub, release notes, Swagger)

**Etiquetas:** `documentation`

## Contexto
La rúbrica pide README, Swagger y documentación de Docker.

## Criterios de aceptación
- [ ] El README tiene la sección "Imágenes en Docker Hub" con el nombre real de las imágenes y el comando de `docker-compose.hub.yml`.
- [ ] Las Release Notes de la Entrega 2 resumen lo que contiene el repositorio.
- [ ] Hay una sola forma documentada de levantar, probar y apagar el stack.
- [ ] Los enlaces del README funcionan.

## Etiquetas

```bash
gh label create enhancement --color a2eeef --force
gh label create backend --color 1d76db --force
gh label create frontend --color 5319e7 --force
gh label create flights-service --color 0e8a16 --force
gh label create admin --color d93f0b --force
gh label create ci --color fbca04 --force
gh label create devops --color c5def5 --force
gh label create documentation --color 0075ca --force
gh label create test --color bfd4f2 --force
```

## Comandos `gh issue create`

Ejecutar desde la raíz del repositorio con `gh auth status` correcto.

```bash
gh issue create --title "feat(admin): gestionar paquetes de cualquier agencia" --label "enhancement,backend,frontend,admin" --body "$(cat <<'EOF'
## Contexto
Hoy solo una agencia puede crear, editar y eliminar sus propios paquetes. El rol administrador tiene que poder cargar paquetes en nombre de cualquier agencia.

## Alcance
- Backend: el administrador puede crear, editar y eliminar paquetes indicando la agencia propietaria; la agencia sigue limitada a sus paquetes.
- Frontend: pantalla de administración con listado de paquetes de todas las agencias, formulario con selector de agencia y confirmación de baja.
- Contrato documentado en Swagger.

## Criterios de aceptación
- [ ] Un administrador crea un paquete para una agencia elegida y aparece en el catálogo y en el panel de esa agencia.
- [ ] Un administrador edita y elimina paquetes de cualquier agencia.
- [ ] Una agencia no puede crear ni modificar paquetes de otra agencia (403).
- [ ] Un comprador no ve ni puede usar estas rutas (403 en la API, "Sin permiso" en la interfaz).
- [ ] Tests unitarios/de integración del backend y tests del frontend para los tres roles.
- [ ] Pasa el Quality Gate de Sonar.
EOF
)"

gh issue create --title "feat(admin): cargar y gestionar vuelos" --label "enhancement,backend,frontend,flights-service,admin" --body "$(cat <<'EOF'
## Contexto
Los vuelos solo existen como datos de ejemplo. El administrador tiene que poder cargarlos para poder armar paquetes nuevos.

## Alcance
- flights-service: alta (y baja o edición si corresponde) de vuelos con origen, destino, fecha, precio y asientos disponibles; validaciones de datos.
- Backend: endpoint restringido a administradores que delega en flights-service con la protección de Resilience4j existente.
- Frontend: formulario y listado de vuelos en la sección de administración.

## Criterios de aceptación
- [ ] Un administrador carga un vuelo desde la interfaz y queda persistido en la base de flights-service.
- [ ] Los datos inválidos (fechas incoherentes, asientos negativos, origen igual a destino) se rechazan con un error claro.
- [ ] Solo el administrador accede a la carga de vuelos (403 para otros roles).
- [ ] Tests en flights-service, backend y frontend.
- [ ] Documentado en Swagger y en `docs/admin.md`.
EOF
)"

gh issue create --title "feat(agency): selector de vuelos al armar un paquete" --label "enhancement,frontend,backend" --body "$(cat <<'EOF'
## Contexto
Al crear un paquete hay que indicar el vuelo de ida y el de vuelta. Ingresar identificadores a mano es propenso a errores.

## Alcance
- Selector de vuelos en el formulario de paquetes de la agencia y del administrador, filtrado por origen/destino y fecha.
- Si falta, un endpoint de consulta de vuelos disponibles en el backend.

## Criterios de aceptación
- [ ] El formulario permite buscar y elegir vuelo de ida y de vuelta sin escribir identificadores.
- [ ] El selector solo ofrece vuelos coherentes (el de vuelta sale después de la ida y el origen/destino se corresponden con el paquete).
- [ ] Sin resultados, muestra un estado vacío; ante un error de red, permite reintentar.
- [ ] Tests del componente y del hook de consulta.
EOF
)"

gh issue create --title "ci(cd): publicar imágenes en Docker Hub" --label "ci,devops,documentation" --body "$(cat <<'EOF'
## Contexto
La rúbrica del Checkpoint 2 pide entrega continua con imágenes en Docker Hub. Hoy solo se publica en GHCR.

## Alcance
- Los workflows de backend, flights-service y frontend publican también `<namespace>/ctv-<servicio>` en Docker Hub con las etiquetas `latest` y el SHA del commit.
- Misma condición que hoy: solo en push a `main`, después de que pasan los controles del módulo (incluido el Quality Gate de Sonar); nunca en PR.
- Secrets `DOCKERHUB_USERNAME` y `DOCKERHUB_TOKEN`; variable opcional `DOCKERHUB_NAMESPACE`.
- `docker-compose.hub.yml` para levantar todo el stack desde Docker Hub.
- `actionlint` y `scripts/check-ci.rb` siguen pasando.

## Criterios de aceptación
- [ ] Un push a `main` deja las tres imágenes en Docker Hub con `latest` y el SHA.
- [ ] Un PR no publica ni necesita los secrets.
- [ ] Sin los secrets, el job de publicación en `main` falla con un mensaje claro.
- [ ] `docker compose -f docker-compose.hub.yml up -d --wait` levanta la aplicación sin compilar.
- [ ] El README documenta las imágenes y la configuración necesaria.
EOF
)"

gh issue create --title "test(e2e): cubrir la sección de administración" --label "test,frontend" --body "$(cat <<'EOF'
## Contexto
Los tests de Cypress cubren login, búsqueda, hoteles y compras. Falta cubrir la administración de paquetes y vuelos.

## Alcance
- Cypress: el administrador carga un vuelo, crea un paquete para una agencia, lo edita y lo elimina.
- Cypress: un comprador y una agencia no acceden a las rutas de administración.
- Cucumber (opcional): reglas de autorización de las APIs de administración.

## Criterios de aceptación
- [ ] Los escenarios corren en el CI (`e2e.yml`) sobre el entorno Docker aislado.
- [ ] Los tests no dependen del orden de ejecución ni dejan datos que rompan otros escenarios.
- [ ] `scripts/check-ci.rb` sigue pasando.
EOF
)"

gh issue create --title "docs: documentar la sección de administración" --label "documentation" --body "$(cat <<'EOF'
## Contexto
El README y la demo deben explicar lo que puede hacer un administrador.

## Alcance
- `docs/admin.md` con el login de administrador y las funciones disponibles.
- Enlaces desde el README y desde la sección de administrador de `docs/demo.md`.
- Verificar pantallas y rutas una vez mergeadas las funcionalidades y quitar el TODO del documento.

## Criterios de aceptación
- [ ] `docs/admin.md` coincide con lo que muestra la aplicación.
- [ ] El README y `docs/demo.md` lo enlazan.
- [ ] Swagger refleja los endpoints de administración.
EOF
)"

gh issue create --title "docs: completar el README para la entrega (Docker Hub, release notes, Swagger)" --label "documentation" --body "$(cat <<'EOF'
## Contexto
La rúbrica pide README, Swagger y documentación de Docker.

## Criterios de aceptación
- [ ] El README tiene la sección "Imágenes en Docker Hub" con el nombre real de las imágenes y el comando de `docker-compose.hub.yml`.
- [ ] Las Release Notes de la Entrega 2 resumen lo que contiene el repositorio.
- [ ] Hay una sola forma documentada de levantar, probar y apagar el stack.
- [ ] Los enlaces del README funcionan.
EOF
)"

```
