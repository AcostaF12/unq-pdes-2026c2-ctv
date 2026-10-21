<!-- TODO: verificar las pantallas y rutas de este documento una vez mergeadas las funcionalidades de administración de paquetes y vuelos (rutas, textos y capturas). -->

# Sección Administración

Guía de lo que puede hacer el rol administrador en la aplicación.

## Cómo entrar

1. Levantar el stack (`docker compose up --build -d --wait`, o `docker compose -f docker-compose.hub.yml up -d --wait` para usar las imágenes de Docker Hub).
2. Abrir http://localhost:8090 e iniciar sesión con:

| Usuario | Contraseña |
|---------|------------|
| `vferreyra` | `vferreyra` |

`facosta` y `adisanto` también son administradores (contraseña igual al usuario). Los carga `DataBootstrap`.

La navegación muestra las opciones de administración solo al rol administrador. Un comprador o una agencia que intente abrir esas rutas ve "Sin permiso", y la API responde con error de autorización.

## Qué puede hacer un administrador

### Hoteles

- Ver el catálogo (`/hotels`).
- Crear, editar y eliminar hoteles desde la interfaz.

### Paquetes de cualquier agencia

- Cargar paquetes eligiendo la agencia a la que pertenecen.
- Editar y eliminar paquetes de cualquier agencia.

### Vuelos

- Cargar vuelos en `flights-service` para poder usarlos en los paquetes.
- Las agencias y los administradores eligen los vuelos de ida y de vuelta de un paquete desde un selector, en lugar de escribir identificadores.

## Verificación rápida

1. Entrar como `vferreyra` y confirmar que aparece la navegación de administración.
2. Crear un hotel, editarlo y eliminarlo.
3. Cargar un vuelo de ida y uno de vuelta.
4. Crear un paquete para la agencia Despegar usando esos vuelos y comprobar que aparece en el catálogo y en el panel de la agencia (`agency` / `agency123`).
5. Entrar como `buyer` / `buyer123` y confirmar que las rutas de administración no están disponibles.

## Referencias

- Contrato de la API: Swagger en http://localhost:8080/swagger-ui/index.html.
- Guion de demo: [demo.md](demo.md).
