# GeoStat System

> Multi-module Spring Boot platform for the National Statistics Office of Georgia — automobile statistics, trade analytics, data import/export, and public API services.

[![Java](https://img.shields.io/badge/Java-17-orange?logo=openjdk)](https://openjdk.org/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.4.5-brightgreen?logo=springboot)](https://spring.io/projects/spring-boot)
[![Gradle](https://img.shields.io/badge/Gradle-Multi--Module-blue?logo=gradle)](https://gradle.org/)
[![SQL Server](https://img.shields.io/badge/SQL%20Server-2019+-red?logo=microsoftsqlserver)](https://www.microsoft.com/sql-server)
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

---

<p align="center">
  <sub>National Statistics Office of Georgia — GeoStat System &copy; 2024–2026</sub>
</p>

