# Migración de agencias

Flyway aplica `V3__agency_active.sql` al iniciar el backend. Agrega `agencies.active`, obligatorio y con valor inicial `true`, sin borrar ni modificar las agencias existentes ni sus relaciones. Las versiones anteriores de la aplicación pueden seguir insertando agencias porque la columna tiene un valor por defecto.

Las pruebas migran una base H2 desde V2 con una agencia existente, verifican su nombre y estado activo y comprueban que Flyway no repite V3. La suite PostgreSQL con Testcontainers verifica el esquema contra las entidades cuando Docker está disponible. No ejecutar V3 manualmente después de que Flyway la haya aplicado.

Para revertir una versión de la aplicación, se puede volver al código anterior dejando la columna y V3 aplicadas. No se deben cambiar ni borrar migraciones ya ejecutadas. El código anterior no conoce la desactivación, por lo que ese rollback restaura la operación de todas las agencias, incluso las marcadas inactivas. No usarlo si se necesita mantener ese bloqueo; en ese caso corregir hacia adelante o detener temporalmente el servicio.

No se incluye un `DROP COLUMN`: eliminaría el estado registrado y no es necesario para volver a una versión anterior.

La desactivación bloquea solicitudes nuevas. No cancela compras que ya estaban en ejecución. El perfil local `dev` sigue recreando los datos demo al arrancar; para comprobar conservación de datos entre versiones se debe usar una base persistente sin ese reinicio.
