# Entorno para pruebas e2e

Desde la raíz del repositorio, con Docker Desktop abierto:

```bash
docker compose -f docker-compose.e2e.yml up --build -d --wait --wait-timeout 180
```

Abrir http://localhost:18090. Comprador: `buyer` / `buyer123`.
Administrador: `vferreyra` / `vferreyra`. El backend está en http://localhost:18080.
La API de vuelos se publica en http://localhost:18081 solo para preparar datos e2e.

Este entorno usa el proyecto Docker `ctv-e2e` y bases PostgreSQL temporales,
separadas del entorno de desarrollo y del módulo Cucumber. Reutiliza sus
servicios mediante `extends`; no ejecuta los escenarios Cucumber.
Los datos iniciales incluyen paquetes, hoteles y vuelos con fechas relativas
al día de arranque. Los tests deberán preparar sus datos antes de modificar el estado.

Para apagarlo y eliminar los datos de prueba:

```bash
docker compose -f docker-compose.e2e.yml down
```

Para empezar otra ejecución con los datos iniciales, ejecutar `down` y luego
el comando `up` anterior.

## Cypress

Con Node.js instalado, desde `frontend/`:

```bash
npm ci
npm run e2e:open
```

`e2e:open` abre la interfaz de Cypress para ver las acciones en el navegador.
`npm run e2e:run` ejecuta los tests sin abrir la interfaz, como se hará en CI.
El entorno Docker anterior debe estar levantado. Los escenarios se escribirán
en `frontend/cypress/e2e/` con nombres terminados en `.cy.js` o `.cy.ts`;
La suite verifica login/búsqueda/detalle, compra/historial, creación/edición/eliminación
de hoteles, contraseña incorrecta y permisos del comprador. Se usan servicios reales.
La compra prepara un comprador, dos vuelos y un paquete propios mediante APIs;
la operación de compra se hace desde la interfaz. El hotel usa un nombre único
y se elimina al finalizar el recorrido. Los datos restantes se eliminan con `down`.
La configuración está en `frontend/cypress.config.js` y apunta a http://localhost:18090.
Los videos y capturas generados no se incluyen en Git.

Para ejecutar toda la suite en Chrome: `npm run e2e:run -- --browser chrome`.

# React + TypeScript + Vite

This template provides a minimal setup to get React working in Vite with HMR and some ESLint rules.

Currently, two official plugins are available:

- [@vitejs/plugin-react](https://github.com/vitejs/vite-plugin-react/blob/main/packages/plugin-react) uses [Oxc](https://oxc.rs)
- [@vitejs/plugin-react-swc](https://github.com/vitejs/vite-plugin-react/blob/main/packages/plugin-react-swc) uses [SWC](https://swc.rs/)

## React Compiler

The React Compiler is not enabled on this template because of its impact on dev & build performances. To add it, see [this documentation](https://react.dev/learn/react-compiler/installation).

## Expanding the ESLint configuration

If you are developing a production application, we recommend updating the configuration to enable type-aware lint rules:

```js
export default defineConfig([
  globalIgnores(['dist']),
  {
    files: ['**/*.{ts,tsx}'],
    extends: [
      // Other configs...

      // Remove tseslint.configs.recommended and replace with this
      tseslint.configs.recommendedTypeChecked,
      // Alternatively, use this for stricter rules
      tseslint.configs.strictTypeChecked,
      // Optionally, add this for stylistic rules
      tseslint.configs.stylisticTypeChecked,

      // Other configs...
    ],
    languageOptions: {
      parserOptions: {
        project: ['./tsconfig.node.json', './tsconfig.app.json'],
        tsconfigRootDir: import.meta.dirname,
      },
      // other options...
    },
  },
])

```

You can also install [eslint-plugin-react-x](https://github.com/Rel1cx/eslint-react/tree/main/packages/plugins/eslint-plugin-react-x) and [eslint-plugin-react-dom](https://github.com/Rel1cx/eslint-react/tree/main/packages/plugins/eslint-plugin-react-dom) for React-specific lint rules:

```js
// eslint.config.js
import reactX from 'eslint-plugin-react-x'
import reactDom from 'eslint-plugin-react-dom'

export default defineConfig([
  globalIgnores(['dist']),
  {
    files: ['**/*.{ts,tsx}'],
    extends: [
      // Other configs...
      // Enable lint rules for React
      reactX.configs['recommended-typescript'],
      // Enable lint rules for React DOM
      reactDom.configs.recommended,
    ],
    languageOptions: {
      parserOptions: {
        project: ['./tsconfig.node.json', './tsconfig.app.json'],
        tsconfigRootDir: import.meta.dirname,
      },
      // other options...
    },
  },
])

```
