# GeoStat System — Dashboard

A production-grade administrative dashboard for the National Statistics Office of Georgia, built with React 19 and a custom data layer. Supports multi-department workflows, role-based access control, real-time file import progress, and a hierarchical CMS for managing navigational structure.

---

## Features

- **Role-Based Access Control** — fine-grained permission system with three tiers: resource, admin, and per-department. Roles and permissions are fully manageable from the UI without code changes.
- **Dynamic CMS** — tree-based page and menu management with drag-and-drop reordering. Leaf nodes auto-generate upload routes; directory nodes group menu sections. Per-node access rules control visibility per role.
- **Real-Time File Uploads** — STOMP over WebSocket delivers live import progress from the Spring Boot backend. Supports multi-year batch selection, mid-upload cancellation, and upload history sidebar.
- **Data Transforms Pipeline** — custom fluent `Mutator` toolkit handles request/response serialization between React Admin's data model and the Spring REST API without coupling features to transport logic.
- **Multi-Environment Docker Deployment** — multi-stage Dockerfile (deps → build → nginx), separate compose files for dev/prod, Nginx reverse proxy with SPA fallback and long-term static asset caching.
- **Smart Query Management** — TanStack Query configured to skip retries on 4xx errors, suppress refetch on window focus for error-state queries, and prevent "looping 404" bugs on deleted resources.

---

## Tech Stack

| Layer | Technology |
|---|---|
| Framework | React 19, React Router 7, TypeScript 5 |
| Admin UI | React Admin 5.8 |
| Styling | Material UI 7, Emotion |
| Data Fetching | TanStack Query 5, Axios |
| Real-Time | STOMP, SockJS, WebSocket |
| Charts | Recharts |
| Auth | JWT (RS256), token refresh, RBAC |
| Build | Vite 6, custom chunk splitting |
| Containerization | Docker, Nginx 1.27, Node 22 Alpine |

---

## Architecture

### Feature-Based Module Structure

```
src/
├── api/            # HTTP client, query client, query builder
├── auth/           # Authentication provider, authorization hooks, guards
├── config/         # Centralized env config, query presets, upload limits
├── features/       # Self-contained feature modules
│   ├── cms/        # Page/menu tree management
│   ├── dashboard/  # Analytics with revenue and usage charts
│   ├── permissions/
│   ├── profile/
│   ├── roles/
│   ├── upload/     # Multi-year file import with WebSocket progress
│   └── users/
├── layout/         # AppLayout, sidebar, app bar, dynamic menu
├── providers/      # Data provider routing, Spring adapter, mutator utils
├── services/       # WebSocket service with exponential backoff reconnect
├── themes/
├── types/
└── workers/
```

### Auth Architecture

Auth is split into four independent layers to enable testing and reuse:

1. **Core types** — `UserAuth`, `JwtPayload`, permission constants with auto-derived union type. Adding a permission constant automatically expands the TypeScript type.
2. **Authentication** — login, logout, token storage, JWT decode, refresh flow. React Admin `AuthProvider` implementation.
3. **Authorization** — route-level `RESOURCE_POLICIES`, hooks (`useHasPermission`, `useHasAnyPermission`, `useHasRole`, `useIsAdmin`, `useCanAccessPage`).
4. **Guards** — `HttpErrorBoundary`, `AccessDenied` fallback UI, `MenuAccessContext`.

### Data Provider Routing

A JavaScript `Proxy` intercepts React Admin data provider calls and dispatches to specialized providers based on resource name:

- `dashboard` → `dashboardDataProvider`
- `users`, `roles`, `permissions` → `userDataProvider`
- `pages` → `pagesDataProvider`
- Everything else → `springDataProvider` (Spring Data `Page<T>` format adapter)

### Mutator Toolkit

A fluent builder for transforming records between API and UI representations. Supports both immediate and deferred (reusable) modes:

```typescript
// Immediate
mutate(data).map("roleIds", "roles", id => ({ id })).rename("displayName", "display_name").get();

// Deferred — reusable transform function
const toRequest = mutator().set("status", "active").build();
```

Supports dot-notation nested paths, conditional transforms (`.ifElse()`, `.switch()`), and typed closures that don't leak mutations to parent scope.

### WebSocket Reconnection

`WebSocketService` wraps `@stomp/stompjs` with:

- Exponential backoff reconnection (up to 15 attempts)
- Configurable heartbeat (default 15s)
- Per-connection token and task ID headers
- `useWebSocket` hook with mounted-ref guard to prevent unmount race conditions and callback refs to prevent unnecessary reconnects on re-render

### Build Optimization

Vite is configured with manual chunk splitting to avoid React initialization order bugs:

```
xlsx           → vendor-xlsx   (no React deps, safe to split)
@stomp, sockjs → vendor-ws     (WebSocket, no React deps)
everything else → vendor-app   (React-dependent, kept together)
```

Chunk filenames mirror `src/` folder structure in `dist/assets/` for easier debugging.

---

## Getting Started

### Local Development

```sh
npm install
npm run dev
```

### Docker (Development)

```sh
docker-compose up
```

The override file is loaded automatically. Source files are volume-mounted with watch enabled on `src/` and `package.json`.

### Docker (Production)

```sh
docker-compose -f docker-compose.prod.yml up -d
```

Builds a production image, serves via Nginx on port 5175, and proxies `/api` and `/sign` to the backend.

### Environment Variables

| Variable | Description |
|---|---|
| `VITE_BASE_URL` | Base URL of the Spring Boot backend (default: `http://localhost:8081`) |
| `VITE_APP_TITLE` | Application title shown in the browser tab |

---

## Project Context

This system is deployed for an internal statistics management workflow. The backend is a Spring Boot application with JWT authentication, Spring Data JPA, and SockJS/STOMP for WebSocket support. The frontend communicates with the backend over a local network with Nginx acting as the reverse proxy in production.

---

## License

Private — all rights reserved.