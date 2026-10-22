# Verificación de la entrega 2

El dataset, credenciales y reglas de persistencia están en [seed y ciclo de vida](seed-y-ciclo-de-vida.md). El recorrido del panel está en [administración](admin.md).

## Chequeos locales

Con Java 25, Node.js 24 y Docker disponibles, ejecutar:

```powershell
# Desde backend/
.\gradlew.bat build

# Desde flights-service/
.\gradlew.bat build

# Desde frontend/ (dependencias: npm ci)
npm run lint
npm test
npm run build
```

En Linux/macOS usar `./gradlew`. Docker permite ejecutar las pruebas PostgreSQL; no deben quedar omitidas para acreditar migraciones y concurrencia.

Desde la raíz, levantar el entorno temporal:

```powershell
docker compose -f docker-compose.e2e.yml up --build -d --wait --wait-timeout 180
```

Desde `frontend/`: `npm run e2e:run -- --browser electron`.

Al terminar: `docker compose -f docker-compose.e2e.yml down`. Las bases temporales se eliminan. Empezar cada ejecución completa con un entorno limpio.

## Recorrido funcional

1. Entrar como administrador y crear una agencia y un vendedor.
2. Crear vuelos y publicar un paquete para esa agencia.
3. Comprar como comprador y verificar su historial, precio pagado y reseña.
4. Consultar la venta como vendedor y desde el historial global administrativo.
5. Archivar el paquete: desaparece de la búsqueda y rechaza compras; conserva detalle, compras y reseñas.
6. Publicarlo otra vez con vuelos vigentes y disponibles.
7. Deshabilitar un usuario: rechazar login y token existente. Cambiar el rol y verificar los permisos en la siguiente petición.
8. Reiniciar backend y vuelos. Comprobar que IDs, perfiles, estados, historial, stock y reservas se conservan y no se duplican.

## Evidencias de entrega

| Requisito | Evidencia |
| --- | --- |
| App Admin y usuario común | Paneles por rol y recorrido anterior; APIs rechazan accesos ajenos |
| Backend, frontend y modelo | Builds, migraciones Flyway y `docs/mer.puml` |
| Documentación y Swagger | README, guía de seed, `/v3/api-docs` y Swagger UI |
| Unitarios e integración | JUnit/Vitest, PostgreSQL y reportes JaCoCo |
| Interfaz/e2e | Las siete suites Cypress contra servicios y bases reales |
| Git, issues y revisión | PR vinculado a los issues y revisión de otra persona |
| CI, Sonar y Quality Gate | Ejecución remota verde asociada al SHA entregado |
| Docker y entrega continua | Imágenes SHA publicadas en Docker Hub y arranque desde Compose Hub |

Los resultados locales no acreditan aprobación remota de Sonar ni publicación de imágenes. Adjuntar esas ejecuciones al PR después de integrar los cambios.

## Resultado local del 22 de octubre de 2026

- Backend: 443 tests aprobados, sin omisiones, build y ktlint correctos.
- Vuelos: 67 tests aprobados, incluidos los de PostgreSQL, idempotencia y concurrencia; build y ktlint correctos.
- Frontend: 222 tests aprobados, build correcto y lint sin errores. Permanece una advertencia previa de dependencias de `useMemo` en `Profile.tsx`.
- Cypress: se valida nuevamente el conjunto integrado; incluye las consultas y cuatro reportes conservados de `dev`.
- Reinicio real de ambos servicios: comparación de datos antes/después sin cambios de usuarios, permisos, paquetes, historial, stock ni reservas.
