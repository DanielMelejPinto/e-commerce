# frontend-app

Tienda web del [e-commerce](../README.md), construida con **React 19**, **TypeScript** y **Vite**. Consume los microservicios del backend a través de un **API Gateway** centralizado.

## Funcionalidades

- **Catálogo** de productos con paginación desde el servidor (Server-Side Pagination).
- **Registro e inicio de sesión** contra `usuario-api`; el token JWT se guarda en `localStorage` y se envía en cada petición.
- **Carrito** de compras global y ultra-rápido manejado con Zustand, persistido en el navegador.
- **Perfil** con los datos del usuario y su historial de pedidos.
- **Panel de administración** (`/admin`, solo rol `ADMIN`): crear productos, ver el inventario y agregar stock.

## Rutas

| Ruta | Acceso | Contenido |
|---|---|---|
| `/` | Público | Catálogo |
| `/login` y `/register` | Público | Inicio de sesión y registro |
| `/cart` | Público | Carrito (para confirmar el pedido hay que iniciar sesión) |
| `/profile` | Autenticado | Perfil e historial de pedidos |
| `/admin` | Rol `ADMIN` | Administración de productos e inventario |

> La restricción de `/admin` **solo existe en el frontend**: los endpoints de producto e inventario no validan el rol en el backend (ver los límites en el [README raíz](../README.md#límites-actuales-y-mejoras-propuestas)).

## Stack

| Tecnología | Uso |
|---|---|
| React 19 + TypeScript | Interfaz y tipado estricto |
| Vite | Servidor de desarrollo y build optimizado |
| TanStack Query (React Query) | Fetching, caché y sincronización de datos de servidor |
| Zustand | Manejo del estado global del cliente (Carrito) |
| React Router 7 | Navegación y rutas protegidas |
| Axios | Cliente HTTP con interceptor que agrega `Authorization: Bearer` |
| Vitest | Framework de pruebas unitarias |
| CSS Modules | Estilos por componente |
| Oxlint | Linter |

## Requisitos

- **Node.js** y npm.
- El backend levantado (ver el [README raíz](../README.md#cómo-ejecutar-el-proyecto-en-local)): Particularmente el **Gateway API (Puerto 8000)** por donde pasa todo el tráfico, junto al resto de microservicios.

## Cómo levantar el proyecto

```bash
npm install
npm run dev
```

La app queda en `http://localhost:5173`.

### Scripts

| Comando | Descripción |
|---|---|
| `npm run dev` | Servidor de desarrollo con HMR |
| `npm run build` | Comprueba tipos (`tsc -b`) y genera el build de producción en `dist/` |
| `npm run preview` | Sirve localmente el build de producción |
| `npm run test` | Ejecuta las pruebas unitarias con Vitest |
| `npm run lint` | Ejecuta Oxlint |

## Comunicación con el backend

En desarrollo, `vite.config.ts` redirige las rutas de API al **API Gateway**, así que el navegador solo habla con `localhost:5173` y evita problemas de CORS:

| Prefijo | Destino |
|---|---|
| `/api/*` | `http://localhost:8000` (Gateway API) |

## Estructura del proyecto

```
src/
├── api/          # Cliente Axios con el interceptor del token
├── components/   # Navbar, ProtectedRoute y AdminRoute
├── context/      # AuthContext
├── features/
│   ├── admin/    # Administración de productos e inventario
│   ├── auth/     # Login, registro y perfil
│   ├── cart/     # Carrito y confirmación del pedido
│   └── products/ # Catálogo
├── layouts/      # MainLayout
├── store/        # Zustand Stores (useCartStore.ts)
├── types/        # Interfaces TypeScript globales
├── App.tsx       # Definición de rutas y Providers
└── main.tsx      # Punto de entrada
```

## Pruebas y CI/CD

El frontend cuenta con integración continua en GitHub Actions. En cada `push` o `Pull Request`, se ejecuta de forma automática:
- Linting con Oxlint.
- Verificación de tipos estáticos con TypeScript.
- Pruebas unitarias de la lógica de estado (Vitest).
- Build de producción.

## Autor

**Daniel Melej Pinto** · [GitHub](https://github.com/DanielMelejPinto)
