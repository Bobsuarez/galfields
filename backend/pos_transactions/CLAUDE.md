# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project

Spring Boot 4.1.0 backend for a POS (point of sale) system, part of the larger Galfields project. Java 21, multi-module Gradle, built on the [Bancolombia Clean Architecture scaffold](https://medium.com/bancolombia-tech/clean-architecture-aislando-los-detalles-4f9530f35d7a).

**This is `backend/pos`, re-architected.** It replaced the original layered `backend/pos` (controller/service/repository/entity) via `specs/04-migracion-pos-clean-architecture.md`, in 10 sequential phases ("Fases"), each validated endpoint-by-endpoint for behavioral parity before the next started. Same API contract, same Postgres schema, same MinIO bucket, same k8s Service/Deployment — this is a structural migration (layers → clean architecture), not a functional one. Base package: `co.com.galfields.pos_transactions`.

## Architecture

```
co.com.galfields.pos_transactions
├── model.<módulo>       # domain/model — pure domain entities + gateway (port) interfaces, no framework annotations
├── usecase.<módulo>     # domain/usecase — business logic, orchestrates model + gateways, framework-free
├── api.<módulo>         # infrastructure/entry-points/api-rest — @RestController + request/response DTOs
├── jpa.<módulo>         # infrastructure/driven-adapters/jpa-repository — @Entity + Spring Data repos + gateway adapters
├── minio.<módulo>       # infrastructure/driven-adapters/minio-storage — image compression + MinIO upload/storage
├── security             # infrastructure/driven-adapters/security — JWT issuing/validation, password hashing
└── config               # applications/app-service — MainApplication, @ComponentScan wiring, SecurityConfig, OpenApiConfig, DataSourceConfig
```

Gradle modules (`settings.gradle`):

| Module | Path | Contains |
|---|---|---|
| `model` | `domain/model` | Domain records/classes + `gateways.*` port interfaces + domain exceptions |
| `usecase` | `domain/usecase` | One `*UseCase` class per module — auto-registered as a Spring bean via `UseCasesConfig`'s `@ComponentScan(pattern = "^.+UseCase$")`, so usecases carry **no** Spring annotations themselves |
| `api-rest` | `infrastructure/entry-points/api-rest` | Controllers, request/response DTOs, `GlobalExceptionHandler`, `CorsConfig`, `OpenApiConfig` |
| `jpa-repository` | `infrastructure/driven-adapters/jpa-repository` | `@Entity` classes, Spring Data `JpaRepository` interfaces, `@Repository` gateway adapters |
| `minio-storage` | `infrastructure/driven-adapters/minio-storage` | `ThumbnailatorImageCompressionAdapter`, `MinioImageStorageAdapter`, bucket bootstrap |
| `security` | `infrastructure/driven-adapters/security` | `JwtTokenService`, `TokenIssuerAdapter`, `BCryptPasswordHasherAdapter` |
| `app-service` | `applications/app-service` | `MainApplication` (composition root — the only `main()`), `SecurityConfig`/`JwtAuthenticationFilter`, `DataSourceConfig`, Flyway migrations, `application.yaml` |

**Domain/usecase modules never depend on Spring, JPA, or any driven-adapter module** — `model`'s gateway interfaces are the only contract crossing that boundary; `jpa-repository`/`minio-storage`/`security` each implement the gateways their module needs. `SecurityConfig`/`JwtAuthenticationFilter` live in `app-service`, not `api-rest`, since the filter chain's `authorizeHttpRequests` table needs visibility across every module's endpoints (the composition root, not any one entry-point module).

### Flat JPA entities — no `@ManyToOne`/`@OneToMany`

Every `@Entity` in `jpa.*` uses plain `@Column Long xId` foreign-key fields instead of JPA relationship mappings. Deliberate, to avoid the Hibernate flush-ordering/`orphanRemoval` hazards `backend/pos` (the old layered version) hit repeatedly. Consequence: JPQL path-navigation joins (`t.employee.firstName`) aren't available — reads that need a join either do multiple repository calls in the adapter (see `ProductRepositoryAdapter#toDomain`) or use a native `@Query(nativeQuery = true)` with a Spring Data interface projection (see `jpa.report`, below).

### Shadow entities / "reuse the real thing"

Since modules were built in a fixed order (Sale → Inventory → Catalog → Employee/Auth → Invoicing → Reports-access → Reports), an early module sometimes needs read access to a table a later module doesn't own yet. Pattern: a **private, minimal, read-only JPA entity** mirroring just the columns needed (e.g. `jpa.sale.shadow.EmployeeShadowEntity`, `jpa.inventory.LocationRefEntity`) — these coexist harmlessly with whichever module's *real* entity eventually owns that table, since Hibernate `ddl-auto=validate` only checks the columns each entity actually declares, not whether some other entity also maps the same table.

**Once a module is genuinely built, a *later* module reuses its real domain gateway directly instead of adding a new shadow** — e.g. `ProductUseCase` (Fase 4/Catálogo) injects Fase 3's real `model.inventory.gateways.InventoryRepository`; `InvoiceNumberingRangeRepositoryAdapter` (Fase 6) imports Fase 5's real `jpa.employee.TerminalEntity`/`TerminalJpaRepository` directly (same Gradle module, no new dependency needed). This reuse only flows *forward* in build order — an earlier module's shadows (e.g. `jpa.sale.shadow`'s Employee/Location/PaymentMethod/ProductVariant/ProductUnit/Inventory shadows, `jpa.inventory`'s Location/ProductVariant shadows) were never retroactively swapped out once the real owning module landed; they're small, private, and harmless to leave as-is. One exception left intentionally as a shadow rather than a cross-module reuse: `jpa.employee.AttachFileRefEntity` (Fase 5, resolving the placeholder employee-photo row) — a single-lookup shadow was simpler and more decoupled than reaching into `jpa.catalog.AttachFileEntity` for one FK resolution.

**Bean-name collision gotcha:** Spring's default `@Repository`/`@Component` naming uses the simple class name — two shadow adapters with the identical class name in different packages (`LocationReferenceGatewayAdapter` in both `jpa.sale.shadow` and `jpa.inventory`) threw `ConflictingBeanDefinitionException` once; fixed by module-prefixing adapter class names (`Sale*ReferenceGatewayAdapter`, `Inventory*ReferenceGatewayAdapter`). Keep new adapter class names prefixed by their module if a name collision is plausible.

### Reports: native SQL + Spring Data projections

`jpa.report`'s 4 repositories (`ReportSalesJpaRepository`, `ReportPaymentJpaRepository`, `ReportSaleItemJpaRepository`, `ReportInventoryJpaRepository`) are bound to `SalesTransactionEntity`/`PaymentEntity`/`SaleItemEntity`/`InventoryEntity` (Fase 2/3's real entities) purely as `JpaRepository<T, Long>` type parameters — they don't modify those entities. Every query method is `@Query(nativeQuery = true)` since flat entities can't express `backend/pos`'s old JPQL joins (`p.transaction.transactionDate`, `t.employee.firstName`); return types are interface-based projections (`InvoiceSummaryProjection`, `InventoryRowProjection`, etc.) whose getter names map to the native query's column aliases — native queries can't use JPQL's `SELECT new pkg.Dto(...)` constructor-expression syntax, so this is the equivalent for a raw-SQL result set.

### Other gotchas hit during the migration

- **`-parameters` javac flag** (`main.gradle`'s `tasks.withType(JavaCompile)`) is required for Spring to resolve `@PathVariable`/`@RequestParam` names by reflection — **still keep an explicit `name = "..."` on every one anyway** as defense-in-depth (a controller test once 400'd across every optional `@RequestParam` because two were missing the explicit name — caught by `ReportControllerTest`, not by manual boot-testing).
- **Hibernate flush-ordering**: `deleteAll()` immediately followed by `save()` of new rows sharing a unique key (e.g. `variant_attributes`' `(variant_id, attribute_name)`) can insert-before-delete within one flush and trip the constraint. Fix: an explicit `.flush()` call between the delete and the re-insert (see `ProductRepositoryAdapter#saveVariant`).
- **`createdAt` gotcha**: constructing a fresh `@Entity` object for every `save()` (create *or* update) without copying the domain's existing `createdAt` forward causes Hibernate's `merge()` to null it out in the *returned* Java object (the DB row itself stays correct — the column is excluded from the UPDATE SQL by `updatable=false`/`@CreationTimestamp`). Every adapter that re-saves an existing row must `entity.setCreatedAt(domain.getCreatedAt())` explicitly.
- **Jackson 3, not Jackson 2**: Spring Boot 4.1 only auto-configures a `tools.jackson.databind.ObjectMapper` bean (Jackson 3) — `com.fasterxml.jackson.databind.ObjectMapper` (Jackson 2) compiles fine (still on the classpath transitively) but fails at boot with `UnsatisfiedDependencyException` the moment something tries to autowire it. Any new `ObjectMapper` injection must use `tools.jackson.*` types.
- **`validateStructure` (Bancolombia plugin) breaks in a shallow Docker build context**: it miscomputes a mangled file path (`NoSuchFileException: /app/..lications.-service/build.gradle`) when the project root is `/app` inside a container. Harmless to skip there — it's an architecture-layering lint already enforced by `./gradlew build` in CI/local dev, not something the runtime jar needs — so the release `Dockerfile` runs with `-x validateStructure`.
- **Process-restart gotcha**: killing the `./gradlew bootRun` wrapper process doesn't reliably kill the underlying `java` process holding the port. Verify via `ss -ltnp | grep <port> | grep -oP 'pid=\K[0-9]+'` and kill that exact PID, confirm the port is free, then confirm a *new* PID appears before re-testing — several early false negatives came from testing against a stale process that never actually picked up the latest code.
- **Live-boot validation caught every non-trivial bug** — unit tests alone missed the flush-ordering and `createdAt` issues above; both only surfaced by actually booting `MainApplication` against real Postgres/MinIO and exercising endpoints with `curl`. Keep doing this for any new endpoint, not just `./gradlew test`.

## Commands

```bash
./gradlew build          # compile + test + pitest + jacoco + ArchUnit (validateStructure) across every module
./gradlew compileJava     # compile only
./gradlew test            # all tests
./gradlew :app-service:bootRun          # run the app (needs .env.local.example sourced first, see below)
./gradlew :usecase:test --tests "co.com.galfields.pos_transactions.usecase.report.ReportUseCaseTest"   # one test class in one module
```

There's no single-module shortcut for "just compile api-rest" that skips its dependencies — Gradle resolves `model`/`usecase` first automatically.

## Dependencies of note

- **Bancolombia Clean Architecture Gradle plugin** (`co.com.bancolombia.cleanArchitecture`, `3.26.4`) — provides the `validateStructure` ArchUnit-based task (wired to run before every `compileJava`, see the Docker gotcha above) and ties the ArchUnit test into `app-service`.
- **Spring Boot 4.1.0** / Java 21.
- **jjwt** (`0.12.6`) — employee-session JWTs (HS256), same as `backend/pos` used.
- **springdoc-openapi** (`2.8.14`, in `api-rest`'s `build.gradle`) — reflects controllers/DTOs into Swagger UI/OpenAPI JSON automatically. `OpenApiConfig` (metadata bean) lives in `api-rest/api/config`, **not** `app-service` — springdoc/swagger-core is an `implementation` dependency of `api-rest`, which doesn't leak to `app-service`'s *compile* classpath through `project(':api-rest')` (only its runtime classpath), so a config class needing `io.swagger.v3.oas.models.*` types has to live in the module that actually declares that dependency.
- **Thumbnailator + `org.sejda.imageio:webp-imageio`** (`minio-storage`) — same image pipeline as `backend/pos`: downscale to 1600px max, re-encode JPEG/PNG to WebP.
- **`tools.jackson.core:jackson-databind`** (Jackson 3) — explicit in `jpa-repository`'s `build.gradle` for the `permissions` JSON↔`Map` conversion (`EmployeeRoleUseCase`).
- **pitest + jacoco**, wired into every subproject's `check`/`build` — mutation testing runs as part of `./gradlew build`, not just line coverage.

## Configuration

`applications/app-service/src/main/resources/application.yaml` — datasource (`DataSourceConfig`'s `RoutingDataSource`, same primary/replica split as `backend/pos`), Flyway, MinIO, JWT secret, CORS, actuator, springdoc. All secrets/environment-specific values come from env vars with **no local default** for the ones that matter (`DB_*`, `JWT_SECRET`) — same convention `backend/pos` used. `spring.servlet.multipart.max-file-size`/`max-request-size` are `10MB` (needed for `/api/products`/`/api/payment-methods` image uploads — missing this defaults to Spring Boot's 1MB limit and silently breaks larger uploads that worked fine on `backend/pos`).

## Database schema

`doc/data_base.sql` — same authoritative cluster-schema dump `backend/pos` used to maintain, copied over at cutover. Migrations `V1__init_schema.sql` through `V10__product_units.sql` (`applications/app-service/src/main/resources/db/migration`) are **byte-identical** to `backend/pos`'s own migration files (verified via `diff -rq` before cutover) — this matters because Flyway validates already-applied migrations' checksums against the files on disk; any difference, even whitespace, would fail `flyway migrate` against the real production DB the moment this app took over Flyway ownership.

**Flyway ownership transferred to this repo at cutover (Fase 10).** `backend/pos` no longer exists and never runs Flyway again — this app's `spring.flyway.baseline-on-migrate=true`/`baseline-version=4` (identical to what `backend/pos` ran) is what now validates/extends `flyway_schema_history` on the real production Postgres. **The next new migration must be `V11__...sql`** — the spec's explicit decision was to continue `backend/pos`'s existing numbering, not restart from V1, precisely so there's one continuous migration history across the cutover instead of two histories to reconcile.

Same schema notes that applied to `backend/pos` still apply here (nothing schema-level changed): `payment_status_enum`/`purchase_order_status_enum` are real Postgres `ENUM`s (`@JdbcTypeCode(SqlTypes.NAMED_ENUM)`); images go through the generic `attach_files` table + `product_images`/`product_variants_images`/`payment_methods_images` 1:1 join tables; `variant_attributes` is a separate table, not columns on `product_variants`; several FK columns (`products.category_id`/`brand_id`, `employees.role_id`/`logo_image`, `sales_transactions.customer_id`) are `BIGSERIAL` in the live cluster (Postgres makes those unconditionally `NOT NULL`, even where conceptually optional) — `V1__init_schema.sql` itself declares them nullable `BIGINT`, but that migration never runs against the real DB (baselined at V4), so this mismatch is harmless as long as nothing assumes a *fresh* install behaves like production for these columns.

## Local infra (Postgres + MinIO)

**Deliberately separate containers/ports/volumes from `backend/pos`'s old local stack** (5433/9000/9001) — `compose.yaml` in this directory runs Postgres on `5434` and MinIO on `9002`/`9003` (console), so both could run side-by-side during the migration without one's Flyway history stepping on the other's. Now that `backend/pos` is gone this separation has no remaining purpose beyond "already the convention" — not worth renumbering back.

```bash
podman compose up -d postgres minio      # infra only
source .env.local.example                 # same var names application.yaml expects, pointed at localhost + the ports above
SERVER_PORT=8081 ./gradlew :app-service:bootRun
```

`podman compose down` keeps volumes; `podman compose down -v` wipes them.

## Docker / CI-CD

`Dockerfile` (repo root, **not** `deployment/Dockerfile` — that one's the Bancolombia scaffold's unused placeholder, which assumes a pre-built jar is copied in rather than building from source) is a real multi-stage build: `eclipse-temurin:21-jdk` copies every module's source (`applications`/`domain`/`infrastructure` — a multi-module project needs all of them in the build context, unlike `backend/pos`'s single-module Dockerfile) and runs `./gradlew :app-service:bootJar -x test -x pitest -x jacocoTestReport -x validateStructure`, then `eclipse-temurin:21-jre` copies out `applications/app-service/build/libs/pos_transactions.jar` (the bootJar's fixed output name — `app-service/build.gradle`'s `bootJar.archiveFileName` resolves to the *root* project's name, `pos_transactions`, not `app-service`).

`.github/workflows/deploy-pos-transactions.yml` (triggers on push to `master` touching `backend/pos_transactions/**`) replaces `backend/pos`'s old `deploy-pos-backend.yml` (deleted at cutover). It calls the same reusable `_build-push.yml` with **`service: pos-backend`, unchanged from the old workflow on purpose** — this is a code migration, not a new k8s workload; keeping the same service name is what makes the new image land in the *same* Deployment/Service in `infra-repo-kinforgeworks` that `backend/pos` used, rather than standing up a parallel one. Same GitOps pin-and-push job as before (`apps/galfields/micro/deployment.yaml` in the infra repo, `ghcr.io/bobsuarez/galfields/pos-backend` image name unchanged).

## Security / Authorization

Same JWT/`SecurityFilterChain` design as `backend/pos`'s spec `01-login-empleados-roles`, ported 1:1: `JwtAuthenticationFilter` (`app-service/config`, stateless, reads `Authorization: Bearer`, converts claims into synthetic `GrantedAuthority`s — `ADMIN`, `DESKTOP`, one `PERM_<module>` per `true` permission) + `SecurityConfig` (`authorizeHttpRequests` table copied 1:1 from the original spec's endpoint→authorization mapping, `anyRequest().denyAll()` as the safe default — any endpoint not explicitly listed falls through to denied, not implicitly "any authenticated user"). `/swagger-ui/**`, `/v3/api-docs/**`, `/actuator/**` are `permitAll()`.

Bootstrap admin is the same seeded row (`V9__employee_auth.sql`): username `admin`, password `admin123` — rotate immediately after first real login in any environment this migration reaches, same as it always was.

## Module reference

Same endpoints, same request/response shapes, same business rules as `backend/pos` — only the internal layering changed. Full behavioral detail (idempotency keys, soft-delete vs hard-delete conventions, default-location scoping, etc.) is preserved in this migration's git history and in `specs/04-migracion-pos-clean-architecture.md`; summarized here per module:

- **Ventas** (`model`/`usecase`/`api`.`sale`) — `POST /api/sales` (idempotent per `clientEventId`, applies the matching stock adjustment atomically, converts `productUnitId` line quantities to base units), `POST /api/sales/{id}/cancel` and `.../by-client-event/{id}/cancel` (reverses the stock decrement, requires a distinct `clientEventId` prefix to avoid looking "already processed").
- **Inventario** (`.inventory`) — `POST /api/inventory/adjustments` (idempotent per `(clientEventId, variantId)`, negative resulting stock allowed — an oversell already happened in the real world, recording it truthfully beats rejecting the call).
- **Catálogo** (`.catalog`) — `/api/products` (multipart create/update, upsert-by-sku variants, soft activate/deactivate), `/api/categories`, `/api/brands`, `/api/locations`, `/api/payment-methods` (multipart, has its own `is_active`), `/api/product-variants/{id}/units` (sale units with conversion factors, base unit immutable via this CRUD). `DEFAULT_LOCATION_NAME = "Bogotá - Chapinero"` is still hardcoded in `ProductUseCase` — same indispensable gotcha as before: renaming that location breaks product creation/stock updates.
- **Empleados/Roles/Terminales + Auth** (`.employee`) — `/api/employees` (password required on create, optional on update — blank leaves it untouched), `/api/employee-roles` (dynamic `permissions` map, `canLoginMobile`/`canLoginDesktop` flags), `/api/terminals`, `POST /api/auth/login` (generic failure message regardless of which check failed, JWT `exp` = next Bogotá midnight).
- **Numeración de facturas** (`.invoicing`) — `/api/invoice-numbering-ranges` CRUD + `GET .../by-terminal/{terminalCode}` (the one endpoint a desktop-only `DESKTOP` authority, not just `ADMIN`, can call — it's how a terminal pulls its own assigned range).
- **Código de acceso a reportes** (`.reportsaccess`) — `POST /api/reports-access-code` (generates + inserts a new 6-digit code), `POST /api/reports-access-code/validate` (checks against the most recently generated one — append-only, no expiry column, generating a new code implicitly invalidates the previous one).
- **Reportes** (`.report`) — `GET /api/reports/sales-summary`, `/sales-by-payment-method`, `/invoices` (paginated), `/invoices/{id}` (full line-item + payment breakdown), `/inventory`, `/low-stock` (default threshold `5`). `from`/`to` are inclusive `YYYY-MM-DD` dates, defaulting to today. Sales aggregates exclude cancelled transactions.

## Testing

Every module/endpoint has JUnit 5 tests (usecase-level with Mockito, controller-level with standalone `MockMvc`) verifying parity with `backend/pos`'s original behavior — this was the acceptance gate for each migration phase before the next started. `./gradlew build` runs all of them plus ArchUnit (`validateStructure`) plus pitest mutation testing; treat a `BUILD SUCCESSFUL` there, **not** just `./gradlew test`, as the real green light.
