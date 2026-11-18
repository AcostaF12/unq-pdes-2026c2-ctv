# Load Tests

Tests de carga y stress de la API del backend, escritos con [k6](https://grafana.com/docs/k6/latest/). Los scripts están en [`load-tests/`](../load-tests) y se corren desde GitHub Actions con el workflow **Load Tests**.

## Tool

Usamos k6 porque es liviano: es un único binario, los escenarios se escriben en JavaScript y los umbrales (`thresholds`) hacen fallar la corrida si no se cumplen, así que el resultado es un sí o un no y no hace falta interpretar gráficos. Los scripts solo usan módulos propios de k6, sin dependencias externas.

## Where the load comes from

La carga se genera desde un runner de GitHub Actions (`ubuntu-24.04`), no desde una máquina del equipo. El workflow tiene dos targets:

| Target | Qué se mide | Cuidado con |
|--------|-------------|-------------|
| `runner` | La aplicación levantada con `docker compose up` en el mismo runner | Sirve para validar que los escenarios funcionan. Los números no son representativos: k6 y la aplicación comparten la misma máquina |
| `vm` | La VM de Azure de la demo (`http://<VM_HOST>:8080`) | Los números incluyen la latencia de internet entre GitHub y Azure, y la VM es de la serie B: si se quedó sin créditos de CPU, rinde menos |

La carga es moderada a propósito: la VM tiene 2 vCPU y cada corrida dura entre 1 y 3 minutos.

## How to run

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
  -e BASE_URL=http://host.docker.internal:8080 grafana/k6:1.0.0 run catalog-load.js
```

`BASE_URL` define el target (por defecto `http://localhost:8080`). `catalog-load` y `login-load` aceptan `VUS` y `DURATION` para una prueba corta, por ejemplo `-e VUS=1 -e DURATION=10s`. Las credenciales de demo se pueden cambiar con `BUYER_USER`, `BUYER_PASSWORD`, `ADMIN_USER` y `ADMIN_PASSWORD`.

## Scenarios

| Escenario | Objetivo | Carga | Umbrales | Qué prueba |
|-----------|----------|-------|----------|------------|
| `catalog-load` | Búsqueda del catálogo (`GET /packages`) con filtros, páginas y orden variados | 20 VUs constantes durante 2 min, con 0,5 a 1,5 s entre búsquedas | Errores < 1 %, p95 < 1000 ms, checks > 99 % | Que el buscador aguanta el uso normal de varios compradores a la vez |
| `login-load` | Inicio de sesión (`POST /auth/login`) | 10 VUs constantes durante 1 min, con 0,5 a 1 s entre intentos | Errores < 1 %, p95 < 1500 ms, checks > 99 % | Que BCrypt, caro en CPU a propósito, no satura el backend |
| `last-seat-stress` | Compra simultánea de un paquete con pocos lugares (`POST /purchases`) | 30 compradores nuevos compran a la vez, una compra cada uno, contra vuelos de 5 lugares | Exactamente 5 compras 201, 25 compras 400, 0 respuestas inesperadas, 0 asientos libres al final | Que el lock pesimista (sobre el paquete en el backend y sobre el vuelo en flights-service) no sobrevende el último asiento |
| `catalog-spike` | Pico de tráfico sobre el catálogo | `ramping-vus`: 5 VUs 30 s, sube a 50 en 20 s, 50 durante 1 min, baja a 5 en 20 s y 5 durante 30 s | Errores < 5 %, checks > 95 %. Se informa el p95 | Que un pico brusco degrada la latencia pero no tira errores |

### Datos que crea `last-seat-stress`

En `setup()`, como administrador, crea un vuelo de ida `BUE → PAR` y uno de vuelta `PAR → BUE` con capacidad y disponibilidad 5 y fechas futuras distintas en cada corrida, un paquete `k6 último asiento <id>` para la primera agencia activa y el primer hotel de París, y 30 compradores `k6-<id>-<n>`. Después de las compras, consulta los dos vuelos y verifica que no quedó ningún lugar libre. En `teardown()` archiva el paquete para que no aparezca en el catálogo de la demo.

Los vuelos, los compradores y las 5 compras quedan guardados, porque la API no permite borrar un paquete con compras. En la VM se acumulan en cada corrida: no se ven en el catálogo, pero sí en los listados del administrador y en los reportes de ventas.

## Results

Completar después de cada corrida contra la VM.

| Fecha | Target | Escenario | p95 | Errores | Resultado | Corrida |
|-------|--------|-----------|-----|---------|-----------|---------|
| _pendiente_ | `vm` | `catalog-load` | | | | |
| _pendiente_ | `vm` | `login-load` | | | | |
| _pendiente_ | `vm` | `last-seat-stress` | | | | |
| _pendiente_ | `vm` | `catalog-spike` | | | | |
