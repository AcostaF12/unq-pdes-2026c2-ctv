# Sección Administración

Qué puede hacer el rol administrador, y cómo comprobarlo.

La lectura para la cátedra está en [catedra.html](catedra.html). Este archivo es la guía corta de la sección.

## Cómo entrar

1. Levantar el stack (`docker compose up --build -d --wait`, o `docker compose -f docker-compose.hub.yml up -d --wait` para usar las imágenes de Docker Hub).
2. Abrir http://localhost:8090 e iniciar sesión con `vferreyra` / `vferreyra`.

`facosta` y `adisanto` también son administradores. La contraseña es igual al usuario. Los carga `DataBootstrap`.

La navegación de `/admin` solo aparece para ese rol. Un comprador o una agencia que abre esas rutas ve "Sin permiso". La API responde 403.

## Pantallas

El inicio está en `/admin`. Cada tarjeta lleva a un área.

| Área | Ruta | Qué hace |
|------|------|----------|
| Paquetes | `/admin/packages` | Crea, edita y elimina paquetes de cualquier agencia |
| Vuelos | `/admin/flights` | Carga vuelos en `flights-service` para usarlos en los paquetes |
| Hoteles | `/hotels` | El listado lo ve cualquier usuario logueado. Alta en `/hotels/new`, edición en `/hotels/:id/edit` |

Al armar un paquete, el administrador y la agencia eligen ida y vuelta en un selector. No escriben ids. El selector solo ofrece vuelos coherentes con el origen, el destino y las fechas.

Los vuelos no tienen baja desde la interfaz: quedan hasta que se borra el volumen o, en `prod`, hasta que el servicio reinicia con `create-drop`.

## Qué exige la API

- `POST /flights` y alta, edición y baja de hoteles: solo `ADMIN`.
- Crear, editar y borrar paquetes: `AGENCY` o `ADMIN`. Una agencia solo toca los suyos; el servicio responde 403 si intenta modificar los de otra.
- El listado de hoteles lo ve cualquier usuario logueado. Crear, editar y borrar, no.
- Las agencias se consultan con `GET /agencies`. No hay pantalla para crearlas: salen del seed.

El contrato está en Swagger: http://localhost:8080/swagger-ui/index.html.

## Verificación rápida

1. Entrar como `vferreyra` y confirmar que aparece Administración.
2. Crear un hotel, editarlo y eliminarlo.
3. Cargar un vuelo de ida y uno de vuelta.
4. Crear un paquete para Despegar con esos vuelos. Tiene que verse en el catálogo y en el panel de `agency` / `agency123`.
5. Entrar como `buyer` / `buyer123` y abrir `/admin`. Tiene que decir "Sin permiso".
