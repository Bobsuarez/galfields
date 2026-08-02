# backend/pos_transactions

API en la nube de Galfields — Spring Boot 4.1 / Java 21, sobre el scaffold [Clean Architecture de Bancolombia](https://medium.com/bancolombia-tech/clean-architecture-aislando-los-detalles-4f9530f35d7a). Es la fuente de verdad de catálogo, inventario, ventas y reportes; tanto [`apps/galfield-pos`](../../apps/galfield-pos) (POS de escritorio) como [`apps/galfields-mobile`](../../apps/galfields-mobile) (app móvil) hablan con esta API.

Reemplaza al antiguo `backend/pos` (arquitectura por capas) — mismo contrato de API, mismo schema de Postgres, mismo bucket de MinIO, mismo servicio/namespace k8s. Ver `specs/04-migracion-pos-clean-architecture.md` para el detalle de la migración.

## Stack

- **Spring Boot 4.1.0** sobre **Java 21**, Gradle multi-módulo (Clean Architecture: `domain/model`, `domain/usecase`, `infrastructure/entry-points/api-rest`, `infrastructure/driven-adapters/*`, `applications/app-service`)
- **PostgreSQL** (primary + réplica de lectura)
- **MinIO** para imágenes, servido públicamente detrás de un CDN
- **Flyway** para migraciones de schema (continúa la numeración que traía `backend/pos`)
- **springdoc-openapi** — Swagger UI autogenerado
- **JWT** (jjwt) — sesión de empleados

## Requisitos

- JDK 21
- El wrapper `./gradlew` sí está commiteado en este módulo (a diferencia del viejo `backend/pos`) — úsalo directamente
- PostgreSQL y MinIO corriendo localmente (ver abajo)

## Comandos

```bash
./gradlew build                          # compilar + tests + pitest + jacoco + ArchUnit (validateStructure)
./gradlew :app-service:bootRun           # correr la app
./gradlew test                            # correr todos los tests
./gradlew :usecase:test --tests "co.com.galfields.pos_transactions.usecase.report.ReportUseCaseTest"   # una clase de test en un módulo
```

## Infraestructura local (Postgres + MinIO)

Este repo trae su propio `compose.yaml` — puertos deliberadamente distintos a cualquier otro stack local:

```bash
podman compose up -d postgres minio      # Postgres en :5434, MinIO en :9002 (API) / :9003 (consola)
source .env.local.example                 # mismas variables que espera application.yaml, apuntadas a localhost
SERVER_PORT=8081 ./gradlew :app-service:bootRun
```

`podman compose down` conserva los volúmenes; `podman compose down -v` los borra.

## Documentación de la API

Con la app corriendo:

- Swagger UI: `http://localhost:8081/swagger-ui.html`
- OpenAPI JSON: `http://localhost:8081/v3/api-docs`

## Endpoints principales

- `/api/products`, `/api/categories`, `/api/brands`, `/api/locations`, `/api/payment-methods`, `/api/product-variants/{id}/units` — catálogo
- `/api/inventory/adjustments` — ajustes de stock idempotentes
- `/api/sales` (+ `.../cancel`) — registro y cancelación de ventas
- `/api/employees`, `/api/employee-roles`, `/api/terminals`, `/api/auth/login` — empleados, roles, terminales y autenticación JWT
- `/api/invoice-numbering-ranges` — numeración de facturas DIAN por terminal
- `/api/reports-access-code` — código de acceso al módulo de reportes del POS de escritorio
- `/api/reports/*` — reportes agregados (ventas, métodos de pago, facturas, inventario, stock bajo)

## Nota sobre el schema

`doc/data_base.sql` es el dump de verdad del schema que corre en el cluster — fuente de verdad por encima de las migraciones de Flyway cuando difieren entre sí. Cualquier cambio de schema debe reflejarse ahí **y** en una migración nueva versionada, empezando en `V11__`. Ver [`CLAUDE.md`](CLAUDE.md) para el detalle completo (arquitectura, convenciones, gotchas de la migración, Docker/CI).

## Más detalle

Este README es solo el punto de entrada — la arquitectura completa, las convenciones de código, y el porqué de cada decisión de diseño están en [`CLAUDE.md`](CLAUDE.md).
