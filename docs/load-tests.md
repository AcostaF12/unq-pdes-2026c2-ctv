# Tests de carga

Tests de carga y stress de la API del backend, escritos con [k6](https://grafana.com/docs/k6/latest/). Los scripts están en [`load-tests/`](../load-tests) y se corren desde GitHub Actions con el workflow **Load Tests**.

## Herramienta

Usamos k6 porque es liviano: es un único binario, los escenarios se escriben en JavaScript y los umbrales (`thresholds`) hacen fallar la corrida si no se cumplen, así que el resultado es un sí o un no y no hace falta interpretar gráficos. Los scripts solo usan módulos propios de k6, sin dependencias externas.

## De dónde sale la carga

La carga se genera desde un runner de GitHub Actions (`ubuntu-24.04`), no desde una máquina del equipo. El workflow tiene dos destinos (`target`):

| Destino (`target`) | Qué se mide | Cuidado con |
|--------|-------------|-------------|
| `runner` | La aplicación levantada con `docker compose up` en el mismo runner | Sirve para validar que los escenarios funcionan. Los números no son representativos: k6 y la aplicación comparten la misma máquina |
| `vm` | La VM de Azure de la demo (`http://<VM_HOST>:8080`) | Los números incluyen la latencia de internet entre GitHub y Azure, y la VM es de la serie B: si se quedó sin créditos de CPU, rinde menos |

La carga es moderada a propósito: la VM tiene 2 vCPU y cada corrida dura entre 1 y 3 minutos.

## Cómo correrlos

### Desde GitHub Actions

Actions → **Load Tests** → Run workflow, con dos inputs:

- `scenario`: `all` o uno de los cuatro escenarios.
- `target`: `runner` o `vm`.

Con `target: vm`, la VM tiene que estar prendida: primero correr **Deploy Demo (Azure VM)** con `action: deploy`. Si el backend no responde en `/actuator/health`, el workflow falla de entrada con ese aviso.

Los escenarios corren uno detrás de otro. Si uno no cumple sus umbrales, los demás corren igual y el job termina en rojo. El resumen de cada escenario (latencias, errores, umbrales y checks) queda en el resumen del job.

### En local

Con la aplicación levantada (`docker compose up --build -d --wait`) y [k6 instalado](https://grafana.com/docs/k6/latest/set-up/install-k6/):

```bash
k6 run load-tests/catalog-load.js
```

Sin instalar k6, con Docker:

```bash
docker run --rm -v "$PWD/load-tests:/scripts" -w /scripts \
  -e BASE_URL=http://host.docker.internal:8080 grafana/k6:2.4.0 run catalog-load.js
```

`BASE_URL` define el destino (por defecto `http://localhost:8080`). `catalog-load` y `login-load` aceptan `VUS` y `DURATION` para una prueba corta, por ejemplo `-e VUS=1 -e DURATION=10s`; `catalog-spike` acepta `BASE_VUS`, `PEAK_VUS` y `TIME_SCALE` (factor sobre las duraciones), por ejemplo `-e BASE_VUS=1 -e PEAK_VUS=4 -e TIME_SCALE=0.125` para unos 20 s. Las credenciales de demo se pueden cambiar con `BUYER_USER`, `BUYER_PASSWORD`, `ADMIN_USER` y `ADMIN_PASSWORD`.

## Escenarios

| Escenario | Objetivo | Carga | Umbrales | Qué prueba |
|-----------|----------|-------|----------|------------|
| `catalog-load` | Búsqueda del catálogo (`GET /packages`) con filtros, páginas y orden variados | 20 VUs constantes durante 2 min, con 0,5 a 1,5 s entre búsquedas | Errores < 1 %, p95 < 1000 ms, checks > 99 % | Que el buscador aguanta el uso normal de varios compradores a la vez |
| `login-load` | Inicio de sesión (`POST /auth/login`) | 10 VUs constantes durante 1 min, con 0,5 a 1 s entre intentos | Errores < 1 %, p95 < 1500 ms, checks > 99 % | Que BCrypt, caro en CPU a propósito, no satura el backend |
| `last-seat-stress` | Compra simultánea de un paquete con pocos lugares (`POST /purchases`) | 30 compradores nuevos compran a la vez, una compra cada uno, contra vuelos de 5 lugares | Exactamente 5 compras 201, 25 compras 400, 0 respuestas inesperadas, 0 asientos libres al final | Que el lock pesimista (sobre el paquete en el backend y sobre el vuelo en flights-service) no sobrevende el último asiento |
| `catalog-spike` | Pico de tráfico sobre el catálogo | `ramping-vus`: 5 VUs 30 s, sube a 50 en 20 s, 50 durante 1 min, baja a 5 en 20 s y 5 durante 30 s | Errores < 5 %, checks > 95 %. Se informa el p95 | Que un pico brusco degrada la latencia pero no tira errores |

### Datos que crea `last-seat-stress`

En `setup()`, como administrador, crea un vuelo de ida `BUE → PAR` y uno de vuelta `PAR → BUE` con capacidad y disponibilidad 5 y fechas futuras distintas en cada corrida, un paquete `k6 último asiento <id>` para la primera agencia activa y el primer hotel de París, y 30 compradores `k6-<id>-<n>`. Después de las compras, consulta los dos vuelos y verifica que no quedó ningún lugar libre.

En `teardown()` deja la demo como estaba: busca las compras de la corrida en `GET /admin/sales?packageName=...`, cancela cada una con `DELETE /purchases/{id}` (tienen que responder 204), verifica que los dos vuelos vuelven a tener sus 5 lugares libres y archiva el paquete para que no aparezca en el catálogo. Esos checks también cuentan para el umbral `checks`.

En la VM solo quedan, de cada corrida, los 30 compradores de prueba y los dos vuelos. Las compras canceladas siguen en los historiales con estado `CANCELLED`, pero no cuentan en los reportes Top 5 ni ocupan lugares, y el paquete archivado no se ve en el catálogo.

## Resultados

Completar después de cada corrida contra la VM.

| Fecha | Destino | Escenario | Pedidos | p95 | Errores | Resultado | Corrida |
|-------|--------|-----------|----------|-----|---------|-----------|---------|
| 26/11/2026 | `vm` | `catalog-load` | 2199 (17,9/s) | 176,9 ms | 0 % | Pasó | [45822461627](https://github.com/AcostaF12/unq-pdes-2026c2-ctv/actions/runs/45822461627) |
| 26/11/2026 | `vm` | `login-load` | 639 (10,5/s) | 345,2 ms | 0 % | Pasó | [45822461627](https://github.com/AcostaF12/unq-pdes-2026c2-ctv/actions/runs/45822461627) |
| 26/11/2026 | `vm` | `last-seat-stress` | 77 | 1235,3 ms | 0 % | Pasó: 5 compras 201, 25 rechazadas 400, 0 inesperadas, 0 lugares libres | [45822461627](https://github.com/AcostaF12/unq-pdes-2026c2-ctv/actions/runs/45822461627) |
| 26/11/2026 | `vm` | `catalog-spike` | 4130 (25,6/s) | 75,1 ms | 0 % | Pasó | [45822461627](https://github.com/AcostaF12/unq-pdes-2026c2-ctv/actions/runs/45822461627) |

La corrida fue contra la versión de `main` `b89de23`, desplegada con "Deploy Demo" ([45820571474](https://github.com/AcostaF12/unq-pdes-2026c2-ctv/actions/runs/45820571474)), con la carga generada desde un runner de GitHub Actions. Las latencias incluyen el viaje por internet hasta la VM.

Lectura de los resultados:

- **Catálogo y pico:** el p95 se mantuvo por debajo de 180 ms, y en el pico de 50 usuarios simultáneos (unos 47 requests/s en Grafana) fue de 75 ms. La búsqueda paginada en PostgreSQL no se degradó con el pico.
- **Login:** es el endpoint más caro por request (p95 345 ms) por el costo de BCrypt, y aun así quedó muy lejos del umbral de 1500 ms.
- **Último asiento:** con 30 compras simultáneas sobre 5 lugares se vendieron exactamente 5 y no hubo sobreventa. El p95 más alto (1235 ms) es esperable, porque las compras esperan el bloqueo pesimista de la fila del vuelo. Al terminar, las 5 compras se cancelaron y los vuelos volvieron a 5 lugares.
