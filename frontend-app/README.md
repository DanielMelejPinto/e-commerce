# frontend-app

Tienda web del [e-commerce](../README.md), construida con **React 19**, **TypeScript** y **Vite**. Consume las cuatro APIs del backend a través del proxy de desarrollo de Vite.

## Funcionalidades

- **Catálogo** de productos en la portada.
- **Registro e inicio de sesión** contra `usuario-api`; el token JWT se guarda en `localStorage` y se envía en cada petición.
- **Carrito** de compras y creación de pedidos en `pedido-api`.
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
| React 19 + TypeScript | Interfaz y tipado |
| Vite | Servidor de desarrollo y build |
| React Router 7 | Navegación y rutas protegidas |
| Axios | Cliente HTTP con interceptor que agrega `Authorization: Bearer` |
| CSS Modules | Estilos por componente |
| lucide-react | Iconos |
| Oxlint | Linter |

## Requisitos

- **Node.js** y npm.
- El backend levantado (ver el [README raíz](../README.md#ejecución-local)): `usuario-api` para login y registro, `producto-api` para el catálogo, `inventario-api` para el panel de administración y `pedido-api` para comprar.

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
| `npm run lint` | Ejecuta Oxlint |

## Comunicación con el backend

En desarrollo, `vite.config.ts` redirige las rutas de API a cada servicio, así que el navegador solo habla con `localhost:5173`:

| Prefijo | Destino |
|---|---|
| `/api/productos` | `http://localhost:8080` |
| `/api/inventarios` | `http://localhost:8081` |
| `/api/usuarios` | `http://localhost:8082` |
| `/api/pedidos` | `http://localhost:8083` |

Si defines `VITE_API_URL`, Axios usará esa URL base en lugar del origen actual (por ejemplo, para apuntar a un backend desplegado). Ese proxy es **solo para desarrollo**: `npm run preview` y un despliegue estático no lo incluyen.

## Estructura del proyecto

```
src/
├── api/          # Cliente Axios con el interceptor del token
├── components/   # Navbar, ProtectedRoute y AdminRoute
├── context/      # AuthContext y CartContext
├── features/
│   ├── admin/    # Administración de productos e inventario
│   ├── auth/     # Login, registro y perfil
│   ├── cart/     # Carrito y confirmación del pedido
│   └── products/ # Catálogo
├── layouts/      # MainLayout
├── App.tsx       # Definición de rutas
└── main.tsx      # Punto de entrada
```

## Limitaciones conocidas

- Sin pruebas automatizadas; el CI del repositorio no ejecuta el frontend.
- El token vive en `localStorage` y el control de acceso a `/admin` es solo visual.
- Sin configuración de despliegue: hoy se usa con el proxy de desarrollo.

## Autor

**Daniel Melej Pinto** · [GitHub](https://github.com/DanielMelejPinto)