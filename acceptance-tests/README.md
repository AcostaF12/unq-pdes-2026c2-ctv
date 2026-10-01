# Acceptance tests

Este módulo ejecuta escenarios Cucumber contra el backend y el servicio de vuelos reales. No reemplaza los tests unitarios ni los tests de integración propios de cada aplicación.

## Requisito

- Docker Desktop

## Primera ejecución

```bash
cd acceptance-tests
docker compose run --rm --build acceptance-tests
```

El Compose de este módulo utiliza bases PostgreSQL temporales en memoria. Cada `docker compose down` elimina los datos de la ejecución y no toca los volúmenes del entorno de desarrollo principal.

El runner también vive en Docker y utiliza Node.js 24, por lo que no es necesario instalar Node o npm en la máquina.

Para apagar el entorno:

```bash
docker compose down
```

El reporte HTML se genera en `acceptance-tests/reports/cucumber-report.html`.

## CI

`Acceptance and E2E CI` ejecuta ambas suites en PRs y pushes a `dev` y `main`.
Cucumber usa el perfil `ci`: solo logs, sin reporte HTML ni artifacts.
Cada job tiene bases temporales y elimina sus contenedores al finalizar.
El deploy manual reutiliza estas pruebas y solo publica/despliega si ambas pasan;
las tres imágenes usan el SHA del commit seleccionado. `stop` no ejecuta pruebas.
La configuración se puede comprobar localmente con `ruby scripts/check-ci.rb`
desde la raíz del repositorio. Esto no sustituye una ejecución real en Actions.

## Variables opcionales

Por defecto se utilizan:

```text
BACKEND_URL=http://localhost:8080
FLIGHTS_URL=http://localhost:8081
```

Se pueden cambiar mediante variables de entorno para ejecutar los escenarios contra otro ambiente controlado.
