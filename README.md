# GeoStat System

> Multi-module Spring Boot platform for the National Statistics Office of Georgia — automobile statistics, trade analytics, data import/export, and public API services.

[![Java](https://img.shields.io/badge/Java-17-orange?logo=openjdk)](https://openjdk.org/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.4.5-brightgreen?logo=springboot)](https://spring.io/projects/spring-boot)
[![Gradle](https://img.shields.io/badge/Gradle-Multi--Module-blue?logo=gradle)](https://gradle.org/)
[![SQL Server](https://img.shields.io/badge/SQL%20Server-2019+-red?logo=microsoftsqlserver)](https://www.microsoft.com/sql-server)
[![Docker](https://img.shields.io/badge/Docker-Ready-2496ED?logo=docker)](https://www.docker.com/)
[![License](https://img.shields.io/badge/license-Internal-lightgrey)]()

---

## Table of Contents

- [Architecture Overview](#architecture-overview)
- [Module Structure](#module-structure)
- [Prerequisites](#prerequisites)
- [Getting Started](#getting-started)
- [Running the Application](#running-the-application)
- [API Endpoints](#api-endpoints)
- [Configuration](#configuration)
- [Build & Deployment](#build--deployment)
- [Docker & Containerization](#docker--containerization)
- [Scripts](#scripts)
- [Project Conventions](#project-conventions)

---

## Architecture Overview

```
┌─────────────────────────────────────────────────────────┐
│                    gesotat-system                        │
│                   (Gradle root project)                  │
├─────────┬──────────────┬───────────────┬────────────────┤
│  core   │   mobile     │     api       │     web        │
│ (lib)   │ (lib + app)  │   (app)       │    (app)       │
│         │              │               │                │
│ Security│ Stats API    │ Admin/Import  │ Thymeleaf      │
│ Auth    │ 50+ endpoints│ File upload   │ Portal UI      │
│ JPA     │ Facade+Svc   │ WebSocket     │                │
│ JWT     │ Query Strategy│ Access/Excel │                │
│ Entities│ pattern      │ import        │                │
└─────────┴──────┬───────┴───────┬───────┴────────────────┘
                 │               │
          ┌──────▼───────┐ ┌─────▼──────┐
          │  SQL Server  │ │ SQL Server │
          │     (DB)     │ │ geostat-   │
          │              │ │ system (DB)│
          └──────────────┘ └────────────┘
```

### Dependency Graph

```
core  ◄──── mobile  ◄──── api
  ▲                         │
  └─────────────────────────┘
  ▲
  └──── web
```

---

## Module Structure

### `core` — Shared Foundation Library

Provides cross-cutting concerns shared across all modules.

| Package | Responsibility |
|---------|---------------|
| `anotation` | Custom annotations (`@Api`, `@Web`, `@Sign`, `@FolderPrefix`) |
| `entity` | JPA entities (`PageNode`, `User`, `Permission`) |
| `exeption` | Global exception handling, `ApiException`, error codes |
| `model` | Shared models (`ClassificationTable`, `ClassificationTableType`) |
| `repository` | JPA repositories (`PageNodeRepository`, `UserRepository`) |
| `security` | JWT authentication, `JwtTokenUtil`, `UserPrincipal` |
| `service` | `QueryBuilder`, `SignService`, `UserService`, `PageStructureService` |
| `setting` | Security configs (`ApiSecurityConfig`, `WebSecurityConfig`), CORS |

### `mobile` — Automobile Statistics Microservice

Self-contained module for vehicle statistics — runs **standalone** or **embedded** in `api`.

| Package | Responsibility |
|---------|---------------|
| `controller` | `PublicMobileController` (25+ endpoints), `PublicMobileTextController` |
| `config` | `MobileDataSourceConfig`, `MobileSecurityConfig` |
| `filter` | `LanguageFilter` (i18n: `ka`/`en`) |
| `dto` | 50+ response DTOs (Sankey, Treemap, Stacked, Regional, etc.) |
| `params` | 30+ request parameter objects |
| `strategy` | 40+ query strategy implementations (Strategy pattern) |
| `repository` | `GlobalRepository` (dynamic JDBC queries) |
| `PublicMobileFacade` | Facade layer — orchestrates table resolution, params, error handling |
| `MobileService` | Core business logic (1300+ lines) |
| `TableResolver` | Resolves `Boolean table` → `[dbo].[eoyes]` / `[dbo].[auto_main]` |
| `RequestContext` | Extracts `lang`, `langName`, `period`, `title` from request |

### `api` — Admin & Data Import Service

Handles file uploads, data conversions, and administrative operations.

| Package | Responsibility |
|---------|---------------|
| `controller` | Upload controllers (8 domain-specific), `XlsxToCsvController` |
| `service` | `FileUploadService` (shared upload logic), `AccessFileImporter` |
| `socket` | WebSocket progress tracking for file imports |
| `setting/db` | `DataSourceConfig` (primary + secondary datasources) |

### `web` — Web Portal

Server-side rendered portal using Thymeleaf templates.

---

## Prerequisites

| Tool | Version | Required |
|------|---------|----------|
| **JDK** | 17+ | ✅ |
| **Gradle** | 8.x (wrapper included) | ✅ |
| **SQL Server** | 2019+ | ✅ |
| **Git** | 2.x+ | ✅ |
| **Docker** | 24.x+ | Optional (for containerized deployment) |
| **Docker Compose** | 2.x+ | Optional (for containerized deployment) |

---

## Getting Started

### 1. Clone the Repository

```bash
git clone <repository-url>
cd gesotat-system
```

### 2. Configure Database

Edit datasource credentials in the appropriate `application.yml`:

| Module | Config File | Default Port |
|--------|------------|-------------|
| `api` | `api/src/main/resources/application.yml` | `8081` |
| `mobile` | `mobile/src/main/resources/application.yml` | `8082` |
| `web` | `web/src/main/resources/application.yml` | `8080` |

```yaml
spring:
  datasource:
    primary:
      jdbc-url: jdbc:sqlserver://<HOST>;databaseName=geostat-system;encrypt=true;trustServerCertificate=true
      username: <USERNAME>
      password: <PASSWORD>
    secondary:
      jdbc-url: jdbc:sqlserver://<HOST>;databaseName=auto;encrypt=true;trustServerCertificate=true
      username: <USERNAME>
      password: <PASSWORD>
```

### 3. Build

```bash
./gradlew clean build -x test
```

### 4. Configure Active Modules (Optional)

Control which modules are built and deployed via `gradle.properties`:

```properties
# gradle.properties
activeModules=api,mobile        # default — both
activeModules=api               # api only (without mobile)
activeModules=mobile            # mobile standalone only
activeModules=api,mobile,web    # everything
```

Or override from CLI without editing the file:

```bash
# Build only mobile
./gradlew build -x test -PactiveModules=mobile

# Build only api (without mobile embedded)
./gradlew build -x test -PactiveModules=api

# Build all
./gradlew build -x test -PactiveModules=api,mobile,web
```

> **Note:** `core` is always included — it's the shared foundation library.

---

## Running the Application

### Full Stack (api + mobile embedded)

```bash
java -jar api/build/libs/api-0.0.1-SNAPSHOT.jar
```

This starts `api` on port **8081** with `mobile` module's controllers and services embedded. All `/mobile/**` and `/mobile-text/**` endpoints are served from the same process.

### Mobile Standalone

```bash
java -jar mobile/build/libs/mobile-0.0.1-SNAPSHOT-boot.jar
```

Runs the mobile statistics API independently on port **8082** — lightweight, no JPA/Hibernate overhead, no admin functionality.

### Web Portal

```bash
java -jar web/build/libs/web-0.0.1-SNAPSHOT.jar
```

### Development Mode (Gradle)

```bash
# Run api (includes mobile)
./gradlew :api:bootRun

# Run mobile standalone
./gradlew :mobile:bootRun

# Run web
./gradlew :web:bootRun
```

---

## API Endpoints

### Mobile — Vehicle Statistics (`/mobile/*`)

| Method | Endpoint | Description |
|--------|----------|-------------|
| `GET` | `/mobile/sliders-data` | Dashboard slider statistics |
| `GET` | `/mobile/filters` | Dynamic filter options (brand, model, fuel, etc.) |
| `GET` | `/mobile/full-raiting` | Full vehicle ratings with pagination |
| `GET` | `/mobile/top-five` | Top 5 vehicles by quarter/year |
| `GET` | `/mobile/treemap` | Treemap visualization data |
| `GET` | `/mobile/sankey` | Sankey diagram flow data |
| `GET` | `/mobile/colors` | Vehicle color distribution |
| `GET` | `/mobile/fuels` | Fuel type distribution |
| `GET` | `/mobile/engine` | Engine type statistics |
| `GET` | `/mobile/body` | Body type statistics |
| `GET` | `/mobile/vehicle-age` | Vehicle age distribution |
| `GET` | `/mobile/race` | Brand race comparison |
| `GET` | `/mobile/vehicle-dual` | Vehicles per 1000 population |
| `GET` | `/mobile/stacked-area` | Stacked area chart (import/export) |
| `GET` | `/mobile/area-currency` | Trade currency data (area chart) |
| `GET` | `/mobile/area-quantity` | Trade quantity data (area chart) |
| `GET` | `/mobile/line-trade` | Trade line chart |
| `GET` | `/mobile/compare-line` | Two-vehicle comparison |
| `GET` | `/mobile/regional-map` | Regional map data |
| `GET` | `/mobile/regional-bar` | Regional bar chart |
| `GET` | `/mobile/regional-quantity` | Regional quantity overview |
| `GET` | `/mobile/equity` | Brand equity ratio |
| `GET` | `/mobile/fuel-currency` | Fuel import/export currency |
| `GET` | `/mobile/fuel-quantity` | Fuel import/export quantity |
| `GET` | `/mobile/fuel-av-price` | Average fuel price |
| `GET` | `/mobile/fuel-column` | Fuel column chart by country |
| `GET` | `/mobile/fuel-line` | Fuel price trends |
| `GET` | `/mobile/road-length` | Road length by region |
| `GET` | `/mobile/accidents-main` | Road accidents overview |
| `GET` | `/mobile/accidents-gender` | Accidents by gender |
| `GET` | `/mobile/license-gender` | Licenses by gender |
| `GET` | `/mobile/license-age` | Licenses by age group |
| `GET` | `/mobile/license-dual` | License dual comparison |
| `GET` | `/mobile/license-sankey` | License Sankey diagram |

### Mobile — Text Endpoints (`/mobile-text/*`)

| Method | Endpoint | Description |
|--------|----------|-------------|
| `GET` | `/mobile-text/ratings` | Selector/filter text data for ratings |
| `GET` | `/mobile-text/top-five` | Top five selectors |
| `GET` | `/mobile-text/full-ratings` | Full ratings selectors |
| `GET` | `/mobile-text/v-quantity` | Vehicle quantity selectors |
| `GET` | `/mobile-text/stacked-area` | Stacked area selectors |
| `GET` | `/mobile-text/export-import` | Export/import selectors |
| `GET` | `/mobile-text/regional-analysis` | Regional analysis selectors |
| `GET` | `/mobile-text/compare` | Compare selectors |
| `GET` | `/mobile-text/fuel` | Fuel selectors |
| `GET` | `/mobile-text/road` | Road selectors |
| `GET` | `/mobile-text/accidents` | Accidents selectors |
| `GET` | `/mobile-text/licenses` | Licenses selectors |

**Common Query Parameters:**

| Parameter | Type | Description |
|-----------|------|-------------|
| `lang` | `String` | Language: `en` or `ka` (default: `ka`) |
| `table` | `Boolean` | `true` = auto_main, `false` = eoyes (default: `false`) |
| `year` | `Integer` | Filter by year |
| `quarter` | `String` | Filter by quarter |

### API — Data Import (`/api/v1/*`)

| Method | Endpoint | Description |
|--------|----------|-------------|
| `POST` | `/inflation/*` | Upload inflation data (Access/Excel) |
| `POST` | `/fasebis-kaleidoskopi/*` | Upload price kaleidoscope data |
| `POST` | `/cpi-calculator/*` | Upload CPI calculator data |
| `POST` | `/pui-portali/*` | Upload FDI portal data |
| `POST` | `/sagareo-vachrobis-portali` | Upload foreign trade data |
| `POST` | `/automobilebis-statistikis-portali/*` | Upload automobile statistics |
| `POST` | `/saertashoriso-shedarebis-portali/*` | Upload international comparison data |
| `POST` | `/khelpasebis-kalkulatori/*` | Upload salary calculator data |

---

## Configuration

### Environment-Specific Profiles

| Property | Description | Default |
|----------|-------------|---------|
| `server.port` | HTTP server port | `8081` (api), `8082` (mobile) |
| `security.type` | Security config type (`api` / `web`) | `api` |
| `spring.datasource.primary.*` | Primary DB (geostat-system) | — |
| `spring.datasource.secondary.*` | Secondary DB (auto) | — |
| `jwt.secret` | JWT signing key | — |
| `jwt.expiration` | JWT token TTL (ms) | `86400000` |
| `storage.export-dir` | Access export directory | `api/storage/access_exports` |

---

## Build & Deployment

### Build All Modules

```bash
./gradlew clean build -x test
```

### Build Artifacts

| Module | Artifact | Type |
|--------|----------|------|
| `core` | `core-0.0.1-SNAPSHOT.jar` | Library |
| `mobile` | `mobile-0.0.1-SNAPSHOT-boot.jar` | Executable (standalone) |
| `mobile` | `mobile-0.0.1-SNAPSHOT-plain.jar` | Library (for api embedding) |
| `api` | `api-0.0.1-SNAPSHOT.jar` | Executable (with mobile embedded) |
| `web` | `web-0.0.1-SNAPSHOT.jar` | Executable |

### Deployment Modes

```
┌──────────────────────────────────────────────────┐
│  Mode A: Monolith (api.jar)                      │
│  ┌────────────────────────────────────────┐      │
│  │  api (port 8081)                       │      │
│  │  ├── admin endpoints                   │      │
│  │  ├── file import/export                │      │
│  │  └── mobile (embedded)                 │      │
│  │      ├── /mobile/* endpoints           │      │
│  │      └── /mobile-text/* endpoints      │      │
│  └────────────────────────────────────────┘      │
└──────────────────────────────────────────────────┘

┌──────────────────────────────────────────────────┐
│  Mode B: Microservices                           │
│  ┌──────────────────┐ ┌───────────────────┐      │
│  │  api (port 8081)  │ │ mobile (port 8082)│      │
│  │  ├── admin        │ │ ├── /mobile/*     │      │
│  │  └── import       │ │ └── /mobile-text/*│      │
│  └──────────────────┘ └───────────────────┘      │
└──────────────────────────────────────────────────┘
```

### Run with Custom Port

```bash
java -jar mobile-boot.jar --server.port=9090
```

---

## Docker & Containerization

Both `api` and `mobile` modules include production-ready Dockerfiles with **multi-stage builds** (JDK 17 build → JRE 17 runtime).

### Project Docker Files

```
gesotat-system/
├── docker-compose.prod.yml     # Production compose
├── .env.prod                   # Production environment (credentials)
├── .env.example                # Environment template
├── .dockerignore               # Build context exclusions
├── api/
│   └── Dockerfile              # API module Dockerfile
├── mobile/
│   └── Dockerfile              # Mobile module Dockerfile
└── scripts/
    ├── deploy.bat              # Build + upload + deploy (Windows)
    ├── manage.bat              # Service management (Windows → SSH)
    └── manage.sh               # Service management (Linux server)
```

### Dockerfile Features

| Feature | Description |
|---------|-------------|
| **Base image** | `eclipse-temurin:17-jre-focal` (Ubuntu 20.04, OpenSSL 1.1.1 — SQL Server compatible) |
| **Non-root user** | Runs as `app:app` via `gosu` for security |
| **Health checks** | Built-in `wget` health check on `/health` |
| **JVM tuning** | G1GC, container-aware memory (`MaxRAMPercentage=75%`) via `JAVA_TOOL_OPTIONS` |
| **Minimal layers** | `apt-get` + user creation in single `RUN` layer |

### Spring Profiles

| Profile | API | Mobile | Description |
|---------|-----|--------|-------------|
| **dev** | `application-dev.yml` | `application-dev.yml` | Debug logging, show-sql, route TRACE |
| **prod** | `application-prod.yml` | `application-prod.yml` | `${ENV}` variables, ddl-auto=validate, WARN logging |

### Quick Start — Development

```bash
# Start both api + mobile
docker-compose -f docker-compose.dev.yml up --build

# Start only mobile
docker-compose -f docker-compose.dev.yml up --build mobile

# Start only api
docker-compose -f docker-compose.dev.yml up --build api

# View logs
docker-compose -f docker-compose.dev.yml logs -f mobile

# Stop everything
docker-compose -f docker-compose.dev.yml down -v
```

> **Dev mode** connects to the host database via `host.docker.internal`, uses debug logging, and allocates moderate memory (512M–1G).

### Quick Start — Production

```bash
# 1. Create environment file from template
cp .env.example .env.prod

# 2. Edit .env.prod with real credentials

# 3. Deploy to server (build + upload + start)
scripts\deploy.bat

# 4. Check status
./manage.sh all status

# 5. View logs
./manage.sh api logs
./manage.sh mobile logs errors

# 6. Stop
./manage.sh all stop
```

### Environment Variables (`.env.prod`)

```bash
# Ports
API_PORT=8081
MOBILE_PORT=8082

# Primary Database (geostat-system)
DB_PRIMARY_URL=jdbc:sqlserver://YOUR_HOST;databaseName=geostat-system;encrypt=true;trustServerCertificate=true
DB_PRIMARY_USER=sa
DB_PRIMARY_PASS=CHANGE_ME

# Secondary Database (auto)
DB_SECONDARY_URL=jdbc:sqlserver://YOUR_HOST;databaseName=auto;encrypt=true;trustServerCertificate=true
DB_SECONDARY_USER=sa
DB_SECONDARY_PASS=CHANGE_ME

# JWT
JWT_SECRET=CHANGE_ME_TO_A_SECURE_64_CHAR_HEX_STRING
```

### Container Resource Limits

| Service | Memory Limit | CPU Limit | JVM Heap | Profile |
|---------|-------------|-----------|----------|---------|
| **api** (dev) | — | — | 256m–1g | dev |
| **api** (prod) | 2560M | 2.0 cores | 512m–2g | prod (G1GC) |
| **mobile** (dev) | — | — | 128m–512m | dev |
| **mobile** (prod) | 1280M | 1.5 cores | 256m–1g | prod (G1GC) |

### Production Compose Features

| Feature | Description |
|---------|-------------|
| **Resource limits** | CPU and memory caps via `deploy.resources` |
| **Restart policy** | `on-failure`, max 5 attempts, 10s delay |
| **Log rotation** | `json-file` driver, 50MB max, 5 files |
| **Health checks** | 30s interval, 60s start period, 5 retries |
| **Named volumes** | `api-storage`, `api-uploads` for persistent data |
| **Bridge network** | `geostat-net` for inter-service communication |
| **G1GC** | Production JVM tuned with G1GC + StringDeduplication |

### Docker Architecture

```
┌─────────────────── Docker Host ───────────────────────┐
│                                                       │
│  ┌─── geostat-net (bridge) ────────────────────────┐  │
│  │                                                 │  │
│  │  ┌──────────────────┐  ┌─────────────────────┐  │  │
│  │  │  geostat-api     │  │  geostat-mobile     │  │  │
│  │  │  :8081           │  │  :8082              │  │  │
│  │  │                  │  │                     │  │  │
│  │  │  Spring Boot     │  │  Spring Boot        │  │  │
│  │  │  + JPA/Hibernate │  │  + JDBC only        │  │  │
│  │  │  + WebSocket     │  │  + 35 REST endpoints│  │  │
│  │  │  + File Import   │  │  + Lightweight      │  │  │
│  │  └────────┬─────────┘  └──────────┬──────────┘  │  │
│  │           │                       │              │  │
│  └───────────┼───────────────────────┼──────────────┘  │
│              │                       │                 │
│  ┌───────────▼───────────────────────▼──────────────┐  │
│  │              SQL Server (external)               │  │
│  │         geostat-system DB  +  auto DB            │  │
│  └──────────────────────────────────────────────────┘  │
│                                                       │
│  Volumes: [api-storage] [api-uploads]                 │
└───────────────────────────────────────────────────────┘
```

### Useful Commands

```bash
# Check status + resource usage
./manage.sh all status

# View logs
./manage.sh api logs
./manage.sh mobile logs errors

# Restart a service
./manage.sh api restart

# Full cleanup (container + image + volumes + files)
./manage.sh api nuke -y

# Rebuild without re-uploading
./manage.sh mobile rebuild

# Enter running container
docker exec -it geostat-mobile sh


# Inspect health status
docker inspect --format='{{.State.Health.Status}}' geostat-api
```

---

## Deploy to Server

Automated deploy scripts handle the full pipeline: **build → upload → docker build → start**.

### Prerequisites

1. **SSH access** to the server:
   ```bash
   ssh-keygen -t ed25519
   ssh-copy-id administrator@192.168.1.199
   ```
2. **Docker & Docker Compose** installed on the server
3. **`.env.prod`** file in project root with database credentials

### Deploy Commands

```bash
# Deploy everything (build + upload + docker start)
scripts\deploy.bat

# Deploy only api service
scripts\deploy.bat api

# Deploy only mobile service
scripts\deploy.bat mobile

# Skip local Gradle build (upload existing jars)
scripts\deploy.bat --no-build
scripts\deploy.bat api --no-build
```

### Module Selection

`gradle.properties`-ში `activeModules` property-ით კონტროლდება რა ჩაირთვება:

```properties
# Both (default)
activeModules=api,mobile

# Only api (mobile won't be embedded)
activeModules=api

# Only mobile standalone
activeModules=mobile
```

`deploy.bat` ავტომატურად გამოიყენებს ამ კონფიგურაც��ას.  
CLI override-იც შეიძლება: `gradlew build -PactiveModules=mobile`

### Deploy Flow

```
scripts\deploy.bat [service]
  │
  ├─ [1/4] gradlew bootJar             — build jar locally
  ├─ [2/4] prepare jar                  — copy to service folder
  ├─ [3/4] scp upload                   — jar + Dockerfile + compose + .env.prod
  └─ [4/4] docker-compose up --build -d — build image & start on server
```

### Server Structure

```
/home/administrator/geostat/
├── docker-compose.prod.yml
├── .env.prod
├── manage.sh                   ← service manager
├── logs/
│   ├── api/                    ← api log files (error, auth, db, app)
│   └── mobile/                 ← mobile log files
├── api/
│   ├── Dockerfile
│   └── api.jar
└── mobile/
    ├── Dockerfile
    └── mobile-boot.jar
```

---

## Scripts

All scripts are in the `scripts/` directory. Only **3 files** — no redundancy.

| File | Description | Runs on |
|------|-------------|---------|
| `scripts/deploy.bat` | Build + upload + docker deploy | Windows (local) |
| `scripts/manage.bat` | Service management via SSH | Windows (local → server) |
| `scripts/manage.sh` | Service management directly | Linux (server) |

> **Dynamic:** სერვისები ავტომატურად აღმოჩენილია `docker-compose.prod.yml`-იდან.  
> ახალი სერვისი compose-ში რომ დაემატება, სკრიპტებში კოდის შეცვლა არ ჭირდება.

### `deploy.bat` — Build & Deploy

```bash
scripts\deploy.bat              # deploy all services
scripts\deploy.bat api          # deploy api only
scripts\deploy.bat mobile       # deploy mobile only
scripts\deploy.bat --no-build   # skip gradle build, upload existing jars
scripts\deploy.bat api --no-build
```

Steps: Gradle build → prepare jars → SCP upload → docker-compose up --build -d

### `manage.bat` / `manage.sh` — Service Manager

ორივე ფაილი იდენტური ფუნქციონალის — `.bat` Windows-იდან SSH-ით, `.sh` სერვერზე პირდაპირ.

#### Usage

```bash
# Windows (SSH-ით სერვერზე)
scripts\manage.bat <service> <action> [flags]

# Linux (სერვერზე პირდაპირ)
./manage.sh <service> <action> [flags]

# Interactive menu (არგუმენტების გარეშე)
scripts\manage.bat
./manage.sh
```

#### Actions

| Action | Description | Example |
|--------|-------------|---------|
| `stop` | Stop container(s) | `manage.sh api stop` |
| `start` | Start container(s) | `manage.sh mobile start` |
| `restart` | Restart container(s) | `manage.sh all restart` |
| `logs` | Docker logs (last 50 lines) | `manage.sh api logs` |
| `logs errors` | Show error.log (tail -f) | `manage.sh api logs errors` |
| `logs auth` | Show auth.log (tail -f) | `manage.sh api logs auth` |
| `logs db` | Show db.log (tail -f) | `manage.sh api logs db` |
| `logs files` | List all log files | `manage.sh all logs files` |
| `status` | Container status + CPU/Memory | `manage.sh all status` |
| `rm` | Stop + remove container(s) | `manage.sh api rm` |
| `nuke` | Remove container + volumes + image + files + logs | `manage.sh api nuke` |
| `rebuild` | No-cache rebuild + start | `manage.sh mobile rebuild` |

#### Service Targets

| Target | Scope |
|--------|-------|
| `api` | API service only |
| `mobile` | Mobile service only |
| `all` | All services |
| *(any new service)* | Auto-discovered from compose |

#### Flags

| Flag | Description |
|------|-------------|
| `-y` / `--force` | Skip `nuke` confirmation prompt |

#### Examples

```bash
# Check status
./manage.sh all status

# View mobile errors
./manage.sh mobile logs errors

# Deploy & start api
scripts\deploy.bat api

# Full cleanup of api (container + image + files + logs)
./manage.sh api nuke -y

# Rebuild mobile without re-uploading jar
./manage.sh mobile rebuild

# Interactive menu
./manage.sh
```

#### Interactive Menu

```
  ==========================================
   GeoStat Service Manager
  ==========================================

   Services:
     1) api
     2) mobile
     3) all

  Service [1-3]: 1

   Action for [api]:
     1) stop       5) status
     2) start      6) rm
     3) restart    7) nuke
     4) logs       8) rebuild

  Action [1-8]: 
```

---

## Project Conventions

### Design Patterns Used

| Pattern | Where | Purpose |
|---------|-------|---------|
| **Strategy** | `strategy/` package (40+ classes) | Dynamic SQL query construction per endpoint/table |
| **Facade** | `PublicMobileFacade` | Simplifies controller→service orchestration |
| **Factory** | `TableQueryStrategyFactory` | Creates strategy instances by name |
| **Template Method** | Query strategies | Base query structure with configurable parts |
| **Builder** | `QueryBuilder` | Fluent SQL query construction |

### Package Naming

```
org.base.core       — Shared foundation
org.base.mobile     — Mobile statistics module
org.base.api        — API/Admin module
```

### Code Standards

- **Java 17** — Records, pattern matching, text blocks
- **Lombok** — `@RequiredArgsConstructor`, `@Slf4j`, `@Data`
- **Constructor Injection** — `private final` fields with `@RequiredArgsConstructor`
- **DTOs** — Java Records where possible
- **Validation** — Jakarta Bean Validation (`@Valid`, `@NotBlank`, `@Pattern`)
- **i18n** — Georgian (`ka`) and English (`en`) via `LanguageFilter` + `LanguageService`

---

## Tech Stack

| Layer | Technology |
|-------|-----------|
| **Language** | Java 17 |
| **Framework** | Spring Boot 3.4.5 |
| **Build** | Gradle 8.x (multi-module) |
| **Database** | Microsoft SQL Server |
| **ORM** | Hibernate 6.x / Spring Data JPA |
| **Security** | Spring Security + JWT |
| **WebSocket** | Spring WebSocket (STOMP) |
| **Template** | Thymeleaf (web module) |
| **File Processing** | Apache POI (Excel), Jackcess (Access), Commons CSV |
| **Serialization** | Jackson |
| **Containerization** | Docker, Docker Compose (dev/prod profiles) |

---

<p align="center">
  <sub>National Statistics Office of Georgia — GeoStat System &copy; 2024–2026</sub>
</p>

