# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project

Spring Boot 4.1.0 backend for a POS (point of sale) system, part of the larger Galfields project. Java 21, built with Gradle.

Base package: `co.com.galfields.pos`. Entry point: `src/main/java/co/com/galfields/pos/PosApplication.java`.

The project follows conventional Spring Boot layering (`controller` / `service` / `repository` / `entity` / `dto` / `exception` / `config` packages under `co.com.galfields.pos`). Entities cover the full POS domain (products, variants, inventory, sales, purchase orders, employees, customers, suppliers); products/categories/brands/locations/payment-methods/inventory/sales/reports all have controllers and services wired up (see the sections below) — only purchase orders/customers/suppliers are still entity-only, with no endpoints yet.

## Keeping this file in sync

**Every modification to this component (schema, entities, config, endpoints) must update this CLAUDE.md in the same change.** This file is the fastest way for a fresh session to get oriented, so stale docs here are worse than no docs. In particular:

- If the deployed DB schema changes, update `doc/data_base.sql` (the source-of-truth dump of what's actually running in the cluster) and re-check that every JPA entity still matches it column-for-column — Hibernate runs with `ddl-auto=validate`, so a mismatch fails at boot, not silently.
- If you add/remove entities, repositories, controllers, or config properties, reflect that in the relevant section below instead of leaving it describing an older state.

## Commands

Use the Gradle wrapper (`./gradlew`), not a system-installed `gradle`. Note: as of now the `gradlew`/`gradlew.bat` scripts themselves are missing from the repo (only `gradle/wrapper/gradle-wrapper.jar` and `.properties` are present) — regenerate them with `gradle wrapper` before relying on `./gradlew`, or fall back to a system `gradle` in the meantime.

- Build: `./gradlew build`
- Run the app: `./gradlew bootRun`
- Run all tests: `./gradlew test`
- Run a single test class: `./gradlew test --tests "co.com.galfields.pos.SomeTestClass"`
- Run a single test method: `./gradlew test --tests "co.com.galfields.pos.SomeTestClass.someMethod"`
- Clean build output: `./gradlew clean`

Tests use JUnit 5 (JUnit Platform).

## Dependencies of note

- **Spring Data JPA** (`spring-boot-starter-data-jpa`) — persistence layer.
- **Spring RestClient** (`spring-boot-starter-restclient`) — for outbound HTTP calls.
- **Lombok** — available at compile time only (`compileOnly`/`annotationProcessor`); use annotations like `@Getter`/`@Setter`/`@Data` instead of hand-written boilerplate.
- **PostgreSQL** driver at runtime — the intended production/dev database.
- **SQLite** driver also present at runtime — check `application.properties` (or `application-<profile>.properties`, none exist yet) to see which datasource is actually active before assuming Postgres.
- **springdoc-openapi** (`springdoc-openapi-starter-webmvc-ui`) — generates the OpenAPI spec and Swagger UI from the existing controllers/DTOs automatically; see "API documentation" below.
- **Spring Security** (`spring-boot-starter-security`, added for spec `01-login-empleados-roles`) — now backed by a real `SecurityFilterChain` (`SecurityConfig`, see "Employee login / JWT" and "Authorization" below), not Spring Boot's default HTTP-Basic-everything lockdown that was in place from step 4 through step 6 of this spec. `BCryptPasswordEncoder` (`EmployeeService`) only actually needed the `spring-security-crypto` module the starter pulls in, but the full starter is what the real filter chain needs.
- **jjwt** (`jjwt-api`/`jjwt-impl`/`jjwt-jackson` 0.12.6, added for spec `01-login-empleados-roles`) — signs and validates the employee-session JWT (`JwtService`). Chosen over Spring Security's own OAuth2/JOSE resource-server stack since there's no external identity provider here — this backend both issues and validates its own tokens with a single shared HMAC secret (`app.jwt.secret`, env `JWT_SECRET`, required, no local default — same convention as the `DB_*`/`MINIO_*` secrets above).

Corresponding test-scoped starters (`spring-boot-starter-data-jpa-test`, `spring-boot-starter-restclient-test`) are included for integration testing against JPA repositories and REST clients.

## Configuration

`src/main/resources/application.properties` sets the app name/port, the Postgres datasource, Flyway, and MinIO settings (see below). Add further profile-specific overrides in `application-<profile>.properties` as needed.

## Database schema

`doc/data_base.sql` is the authoritative dump of the schema actually deployed on the cloud Postgres cluster — treat it as ground truth over the Flyway migrations when the two disagree. New schema changes should land as a new dated migration file *and* an updated `doc/data_base.sql`.

**`V1__init_schema.sql` is a real, full schema-creation migration** (all tables/enums/triggers/indexes + default seed data — categories, brands, payment methods, locations), not a placeholder. It's the sole bootstrap path now: `infra-repo-kinforgeworks`'s `init-schema-configmap.yaml` (a hand-maintained Postgres init script that used to create the schema directly, before Flyway was wired up correctly) has been retired — Postgres itself still creates the empty database/user from `POSTGRES_DB`/`POSTGRES_USER`/`POSTGRES_PASSWORD` (no custom script needed for that), and Flyway (V1 onward) is what creates every table and the default seed data, for any environment.

**The already-deployed cloud database is baselined, not migrated, at V4** (`spring.flyway.baseline-on-migrate=true`, `spring.flyway.baseline-version=4`) — its schema was built by hand over time (the old ConfigMap script, kept manually in sync with V1 through V4's intent) and, confirmed directly against the live DB, already has V1-V4's full cumulative effect: `stock_adjustments`, `payment_methods_images`, `sales_transactions.client_event_id`, and the placeholder `pos-terminal` employee/role V4 inserts. `baseline()` never executes a migration's SQL, it only records every version up to and including the baseline version as "already satisfied" — **this must stay in sync with whatever the cloud DB truly already has**, or the next real migration Flyway attempts will fail with "relation already exists" (confirmed locally: baselining at 1 instead of 4 makes Flyway try to re-run V2's `CREATE TABLE stock_adjustments` and crash exactly like the original incident, just one version later). Only V5 onward ever actually executes against that database today. V1-V4's content only ever runs in full against a genuinely fresh/empty database (a new environment, local dev, disaster recovery) — always verify against the live DB (`\d <table>`) before bumping `baseline-version` further, don't just assume a later migration's intent was already manually applied.

**Gotcha that caused a real production outage:** Flyway silently never ran at all (not even its baseline step — `flyway_schema_history` didn't exist) because Spring Boot 4.x split Flyway auto-configuration into its own artifact, `org.springframework.boot:spring-boot-flyway` — this project only had the raw `org.flywaydb:flyway-core`/`flyway-database-postgresql` (the library itself), never the Spring Boot glue that actually wires `Flyway.migrate()` into app startup. No error, no log line — the auto-configuration class simply wasn't on the classpath. Fixed by adding `spring-boot-flyway` to `build.gradle`. Separately, `DataSourceConfig.java`'s `@Primary` `DataSource` bean is a `RoutingDataSource` (routes primary/replica based on the *current transaction*, which doesn't exist yet during Flyway's early startup) — `primaryDataSource()` is annotated `@FlywayDataSource` so Flyway always migrates through the real primary connection, never through that routing proxy. If a third `DataSource` bean is ever added to this class, keep the `@FlywayDataSource` qualifier on whichever one is the real write connection.

Notable shapes to remember when writing entities/queries against this schema:

- `payment_status_enum` and `purchase_order_status_enum` are real Postgres `ENUM` types (not `VARCHAR` + `CHECK`) — map Java enums with `@JdbcTypeCode(SqlTypes.NAMED_ENUM)` plus `columnDefinition` set to the Postgres type name, or Hibernate's schema validation will fail at boot.
- Images are not stored as a plain string column on `products`/`product_variants`. There's a generic `attach_files` table (name/url/mime_type/size), joined via the 1:1 tables `product_images` and `product_variants_images`, and referenced directly from `employees.logo_image`.
- `product_variants` has no `attribute_name`/`attribute_value` columns — those live in the separate `variant_attributes` table (one variant can have many attributes, e.g. color + size), unique per `(variant_id, attribute_name)`.
- **Gotcha:** several FK columns are declared `BIGSERIAL` in `doc/data_base.sql` instead of plain `BIGINT` (`products.category_id`/`brand_id`, `employees.role_id`/`logo_image`, `sales_transactions.customer_id`). Postgres makes `SERIAL`/`BIGSERIAL` columns `NOT NULL` unconditionally, so these are mandatory in the live DB even where the design clearly intended them optional (e.g. a sale with no customer, an employee with no logo). `ProductRequest.categoryId`/`brandId` are `@NotNull` to match this reality. If you touch employees/sales code, check the actual `\d <table>` output before assuming a FK is nullable — don't trust the DDL's absence of `NOT NULL`. Fixing this upstream (swap to `BIGINT`) requires a coordinated migration, not just an entity change. Note: `V1__init_schema.sql` declares these as plain `BIGINT` (matching the original ConfigMap, nullable as designed) — the live cloud DB's `BIGSERIAL`/`NOT NULL` reality came from manual fixes applied directly to production after that, never fed back into the bootstrap script. Since V1 never runs against that database (see baseline note above), this mismatch is harmless — just don't assume a fresh install behaves identically to production for these specific columns until that's reconciled.

## Product creation/update endpoint

`POST /api/products` and `PUT /api/products/{id}` create/update a product and its variants in a single `multipart/form-data` call — no separate round trips per variant or per image:

- `product` (JSON part): `{ name, description, categoryId, brandId }` — product-level fields only.
- `image` (file part, optional): the product's own image.
- `variants` (JSON part): array of `{ sku, barcode, price, costPrice, initialStock, attributes: [{name, value}] }`. Required and non-empty on create; optional on update (omit to leave existing variants untouched).
- `variantImage_<index>` (file part, optional, one per variant that has an image): `<index>` is the zero-based position of the variant in the `variants` array, e.g. `variantImage_0` is the image for `variants[0]`.

On update, variants are upserted by `sku` against the product's existing variants: a matching `sku` updates that variant (fields, attributes, stock, image); an unmatched `sku` creates a new variant. Variants omitted from the request are left as-is (nothing is deleted). Attribute lists are diffed by name rather than replaced wholesale, to avoid a unique-constraint violation from Hibernate flushing an orphan-removal delete after the corresponding re-insert.

`ProductService` validates sku/barcode uniqueness both within the request payload and against the DB before writing anything.

### Listing/sorting

`GET /api/products` takes standard Spring `Pageable` params (`page`, `size`, `sort`), defaulting to `sort=createdAt,desc`. **Don't pass the raw client-supplied `Sort` straight to the repository** — `findByActiveTrue(Pageable)` queries the `Product` entity directly, so a sort property that isn't an actual JPA path on `Product` (e.g. `price`, `sku`, `stock` - those live on `ProductVariant`, one level down) blows up with `InvalidDataAccessApiUsageException` (500) instead of a clean error. `ProductController` remaps every requested sort key through the `SORTABLE_PROPERTIES` whitelist (`productId`, `name`, `active`, `createdAt`, `updatedAt`, `categoryName` → `category.name`, `brandName` → `brand.name`) before building the `Pageable`, and rejects anything else with a 400. Add a new sortable column here (not in the repository) if the API needs one.

`?includeInactive=true` (default `false`) switches the query from `findByActiveTrue` to a plain `findAll`, so deactivated products (and their variants) show up too — the regular catalog view (mobile's Products screen) omits it on purpose (selling screen shouldn't offer a deactivated product), but the mobile Inventario module (see `apps/galfields-mobile`'s CLAUDE.md) needs to see and reactivate them, so it always passes `includeInactive=true`.

### Activate/deactivate

`DELETE /api/products/{productId}` is a **soft** delete (`ProductService#deleteProduct`): flips `products.is_active` and every one of its variants' `is_active` to `false`, never removes rows. `PUT /api/products/{productId}/activate` (`ProductService#activateProduct`) is the counterpart — flips both back to `true`. Both cascade product → all its variants together; there's no independent per-variant active toggle exposed anywhere (matches how deactivation always worked before `activate` existed). Reactivating an already-active product (or deactivating an already-inactive one) is a harmless no-op, not an error.

### Sale units with conversion factors (`product_units`, spec `03-unidades-venta-conversion`, step 1)

`product_units` (migration `V10__product_units.sql`) lets one `product_variant` be sold under multiple presentations that share the same physical stock (e.g. cigarettes: "Media"/"Completa" cajetilla of the same SKU) — each row has its own `unit_name`/`conversion_factor`/`unit_price`/`barcode`, and exactly one per variant is the base unit (`is_base`, `conversion_factor` always `1`, enforced by `uq_product_units_base` — a unique index on `variant_id` filtered to `is_base`).

**Hangs off `product_variants`, not `products`** — deliberate deviation from the spec's literal data model, which assumed `products` carries its own `price`/`barcode`/stock. In this schema those live on `product_variants` (stock via `inventory`, keyed by `variant_id`), so `product_units.variant_id` is the FK, and `GET /api/products` nests `units` inside each entry of `variants[]` (`ProductVariantResponse.units: List<ProductUnitResponse>`), not directly on the product. `sale_items.variant_id` is unchanged — it still identifies the physical stock row; the new `sale_items.product_unit_id`/`unit_name`/`conversion_factor` columns (same migration) say which presentation of that variant was actually sold, mirroring how `unit_price`/other sale fields are already snapshotted at sale time.

`V10__product_units.sql` backfills one base `ProductUnit` (`unit_name = 'Unidad'`, `conversion_factor = 1`) per existing `product_variants` row, copying that variant's `price`/`barcode` — so every already-sellable variant keeps working unchanged. **This backfill only covers variants that existed at migration time** — `ProductService#createProduct`/`updateProduct` do **not** yet auto-create a base unit for a variant created afterward (out of scope for step 1: entity/repo/`GET` only, no CRUD wiring yet). A variant created between this migration and step 2's unit CRUD landing will have an empty `units` array until one is added by hand.

`ProductUnitResponse.stock` is **not persisted** — computed on the fly in `ProductService#toUnitResponse` as `Math.floorDiv(variantStock, unit.getConversionFactor())` (the variant's own `stockOf(variant)` result, shared across all its units so it's computed once per variant, not once per unit), per this spec's Data model note that a unit's "available in this presentation" figure is the backend's job to convert, not the client's. `floorDiv` (not truncating `/`) matches the spec's literal `floor()` wording even for a negative (oversold) base stock — e.g. base stock `-7` at factor `20` is `-1` "Completa" available (floor), not `0` (truncation toward zero). This is what `apps/galfield-pos`'s `sync.rs` copies verbatim into its local per-row `stock_quantity`, same as `ProductVariantResponse.stock` already was before this field existed.

`ProductUnit`/`ProductUnitRepository` (`findByVariant_VariantId`, `findByProductUnitIdAndVariant_VariantId`) follow the same plain-JPA-entity / Spring-Data-interface shape as `VariantAttribute`/`VariantAttributeRepository` above.

**CRUD (`/api/product-variants/{variantId}/units`, step 2):** `ProductUnitController` → `ProductUnitService` → `ProductUnitRepository`. Deviates from the spec's literal `/api/products/{productId}/units` path for the same reason `product_units` hangs off `variant_id` above — scoping by `variantId` avoids "which variant" ambiguity if a product ever had more than one (still an unsupported combination per the spec's Risks table, but the route doesn't have to assume it).

- `POST`/`PUT` take `{ unitName, conversionFactor, unitPrice, barcode }` (`unitName` required, `conversionFactor` required `>= 1`, `unitPrice` required `>= 0`, `barcode` optional) — **no `isBase` field on the request at all**: a unit created through this CRUD is always non-base (`ProductUnitService#createUnit` hardcodes `base = false`); the base unit is exclusively the migration backfill's job (see the Decisions in `specs/03-unidades-venta-conversion.md`: admins add secondary presentations here, they don't hand-designate a base). Since `isBase` is never in the request, it's also never reassignable via `PUT` — the base flag, once set, is immutable through this API by construction, not by a runtime check.
- **`PUT` on the base unit itself is allowed** (name/price/barcode editable) **except its `conversionFactor` must stay `1`** — `updateUnit` throws `IllegalArgumentException` (400) if the base unit's factor is requested to change, since factor `1` is definitionally what "base" means (the unit `inventory.quantity_on_hand` is already counted in).
- **`DELETE` is a soft deactivate** (`ProductUnitService#deactivateUnit`, flips `is_active` only), matching the spec's "desactivar" wording and every other soft-delete in this codebase (products, employees) — never a real row delete. Deactivating the unit marked `is_base` throws `InvalidStateException` (409) — the one validation the spec explicitly calls for.
- A duplicate `barcode` (`UNIQUE` in the DB, nullable) falls through to the generic `DataIntegrityViolationException` → 409 handling, no bespoke check — same convention as categories/brands/terminals above.
- **Known gap, not addressed by step 2:** a `ProductVariant` created after `V10__product_units.sql` (via `POST`/`PUT /api/products`) still has no base unit and this CRUD doesn't create one either (it only ever creates non-base units) — `ProductService#createProduct`/`updateProduct` would need to auto-create a base `ProductUnit` per new variant for that gap to close; out of scope until asked for.

## Category / Brand / Location / Payment Method CRUD endpoints

`/api/categories`, `/api/brands`, `/api/locations`, and `/api/payment-methods` are CRUDs (`*Controller` → `*Service` → `*Repository`). The first three take a plain JSON body; `/api/payment-methods` is multipart because it also carries an optional image (see below) — otherwise all four share the same shape:

- `/api/categories`: `POST`/`PUT` take `{ name, description }` (`name` required); `categories` has a `description` column.
- `/api/brands`: `POST`/`PUT` take `{ name }` only (`name` required) — `brands` has **no** `description` column (see `doc/data_base.sql`), don't add one to `BrandRequest`/`BrandResponse` without a matching migration first.
- `/api/locations`: `POST`/`PUT` take `{ name, address, phone }` (`name` required, `address`/`phone` optional).
- `/api/payment-methods`: `POST`/`PUT` are `multipart/form-data` — a `paymentMethod` JSON part `{ methodName, active }` (both required) plus an optional `image` file part, same two-part shape as `/api/products`. Unlike the other three, `payment_methods` *does* have an `is_active` column, so `active` is a plain required field on the request/response instead of a separate deactivate endpoint like products use.

All four: `GET` lists all or fetches by id, `DELETE` hard-deletes (none of `categories`/`brands`/`locations` have an `is_active`/soft-delete column, unlike products; `payment_methods` has one but `DELETE` still hard-deletes rather than flipping it). None of `categories.name` / `brands.name` / `locations.name` / `payment_methods.method_name` has a `UNIQUE` constraint in the DB, so duplicate names are allowed on purpose (no app-level uniqueness check).

**List ordering:** all four `GET` (list) endpoints return rows alphabetically by name — `findAllByOrderByNameAsc()` on `CategoryRepository`/`BrandRepository`/`LocationRepository`, `findAllByOrderByMethodNameAsc()` on `PaymentMethodRepository` (derived Spring Data query methods, not a manual `@Query`). Plain `findAll()` has no guaranteed order in Postgres — don't revert to it in these four services' `list*` methods, or the list order goes back to being whatever the DB feels like. `ProductController#list` is unrelated to this — it's paginated and already has its own explicit default sort (`createdAt,desc`, client-overridable via `SORTABLE_PROPERTIES`, see above).

Deleting a row still referenced by a product/inventory/sale/payment (FK, no `ON DELETE` clause → default `RESTRICT`) returns a clean 409 instead of a raw 500: `GlobalExceptionHandler` catches `DataIntegrityViolationException` generically, so this covers any FK-constrained delete across all four, and any future one.

## Terminals CRUD (`/api/terminals`)

`TerminalController` → `TerminalService` → `TerminalRepository`, backed by `terminals` (migration `V9__employee_auth.sql`, part of spec `01-login-empleados-roles`). Same CRUD shape as the four above: `POST`/`PUT` take `{ terminalCode, name, active }` (`terminalCode` required and `UNIQUE` in the DB — a duplicate falls through to the generic `DataIntegrityViolationException` → 409 handling, no bespoke check; `name` optional; `active` required, same pattern as `/api/payment-methods`'s `active`). `GET` lists ordered by `terminalCode` (`findAllByOrderByTerminalCodeAsc`) or fetches by id; `DELETE` hard-deletes — will 409 once `invoice_numbering_ranges` or `employee_terminals` references a terminal (FK, no `ON DELETE`).

This table used to not exist — `invoice_numbering_ranges` carried `terminal_code` as a loose, unrelated string (see below). Same terminal, now one real row instead of two unsynced strings; `employee_terminals` (added in the same migration) assigns employees to terminals for desktop cashier login — no controller/entity of its own, it's a plain `@ManyToMany` (`Employee.terminals`, `@JoinTable(name = "employee_terminals")`), managed entirely through `/api/employees` below.

## Employees CRUD (`/api/employees`)

`EmployeeController` → `EmployeeService` → `EmployeeRepository`, part of spec `01-login-empleados-roles`. Employees themselves aren't new (`employees` table has existed since `V1__init_schema.sql`, used so far only for the placeholder `pos-terminal` row — see "Sale recording endpoint" below) but this is the first real CRUD over them.

**Bootstrap admin (chicken-and-egg, not addressed by the spec):** with the real `SecurityFilterChain` (see "Authorization" below) in place, `/api/employees` itself requires an `ADMIN`-authority JWT — but getting that JWT requires logging in as an `Administrador`, and there's no other endpoint that can create the first one. `V9__employee_auth.sql` seeds one directly: username `admin`, password `admin123` (bcrypt hash inlined in the migration, verified against `BCryptPasswordEncoder` before landing — see the migration's comment). **Rotate this password via `PUT /api/employees` immediately after the first real login in any environment this migration runs against** — it's a known, publicly-documented default, not a secret.

- `POST`/`PUT` take `{ firstName, lastName, username, password, roleId, terminalIds }`. `password` is **required on create**, throwing a clean 400 (`IllegalArgumentException`, now handled generically by `GlobalExceptionHandler` alongside the bean-validation exceptions) if blank/missing; on update it's **optional** — blank/null leaves `password_hash` untouched, non-blank rehashes and replaces it. This is the deliberate mechanism for "admin resets a forgotten password" (see spec's Decisions/Out-of-scope: no separate reset flow, the admin just edits the employee). Passwords are hashed with `BCryptPasswordEncoder` (`PasswordConfig`, a `PasswordEncoder` bean split out from the full `SecurityConfig`/`SecurityFilterChain`, see "Authorization" below) — `password_hash` is never plaintext, and the response DTO never echoes password/hash back.
- `roleId` must reference an existing `employee_roles` row (404 if not); `terminalIds` (optional, defaults to none) must all exist (404 listing the bad ids if not) — persisted via `Employee.terminals`, the `@ManyToMany` over `employee_terminals` mentioned above.
- **No photo upload in this CRUD.** `employees.logo_image` is `BIGSERIAL` → `NOT NULL` in production (see the "Notable shapes" gotcha above) but this spec doesn't add an image part like `/api/products`/`/api/payment-methods` do — every employee created here gets the same shared placeholder row, `attach_files` name `'no-employee-photo'` (seeded by `V9__employee_auth.sql`, looked up by name in `EmployeeService#defaultLogoImage`). Distinct from `V4__sales_recording.sql`'s `'placeholder-employee-logo'`, which stays scoped to the `pos-terminal` system employee — don't reuse or rename either one for the other's purpose.
- `GET` lists all (no special ordering specified by the spec, unlike the alphabetical-by-name convention above) or fetches by id.
- **`DELETE` is a soft deactivate**, not a hard delete — flips `is_active` to `false` only (`EmployeeService#deactivateEmployee`), matching the spec's literal "crear/listar/editar/desactivar" wording and the same convention `/api/products` already uses. There's no reactivate endpoint yet (the spec doesn't ask for one); add `PUT /api/employees/{id}/activate` mirroring `ProductController#activateProduct` if that's ever needed.
- A duplicate `username` (`UNIQUE` in the DB) falls through to the generic `DataIntegrityViolationException` → 409 handling, no bespoke check.

## Employee roles CRUD (`/api/employee-roles`)

`EmployeeRoleController` → `EmployeeRoleService` → `EmployeeRoleRepository`, part of spec `01-login-empleados-roles`. `employee_roles` itself existed since `V1__init_schema.sql`; this is the first CRUD over it, plus the two login flags `V9__employee_auth.sql` added.

- `POST`/`PUT` take `{ roleName, permissions, canLoginMobile, canLoginDesktop }`, all required. `permissions` is a plain `Map<String, Boolean>` on the wire (e.g. `{ "pos": true, "inventario": false, "reportes": false, "sync": true }`) — **not restricted to a fixed key set**, so a future module can gate on a new permission key without a code change (matches the spec's "dynamic roles" decision). The DB column (`employee_roles.permissions`, `JSONB`) stores it as a raw JSON string (`EmployeeRole.permissions: String`, `@JdbcTypeCode(SqlTypes.JSON)`); `EmployeeRoleService` does the `Map<String,Boolean>` ⇄ JSON-string conversion itself via a plain injected `ObjectMapper` (Jackson, already on the classpath from `spring-boot-starter-web`) — a malformed `permissions` payload on write throws `IllegalArgumentException` → clean 400; corrupt JSON already in the DB on read throws `IllegalStateException` → 500 (a data-integrity bug, not a client error, so it isn't caught by `GlobalExceptionHandler`).
- `canLoginMobile`/`canLoginDesktop` replace comparing by `roleName` ("Administrador") — see the seed rows in `V9__employee_auth.sql` and "Employee login / JWT" below: any role can be flagged for mobile and/or desktop login without touching code.
- `GET` lists all (no specified ordering) or fetches by id. **`DELETE` hard-deletes** (unlike `/api/employees`'s soft deactivate — the spec's wording is literally "eliminar" here vs. "desactivar" there); deleting a role still referenced by an employee (`employees.role_id` FK, no `ON DELETE`) 409s via the same generic `DataIntegrityViolationException` handling as everywhere else.
- No app-level uniqueness check on `roleName` (no `UNIQUE` constraint in the DB), same as `categories.name`/`brands.name`/etc. above — duplicates are allowed on purpose.

## Employee login / JWT (`POST /api/auth/login`)

`AuthController` → `AuthService` (+ `JwtService`), part of spec `01-login-empleados-roles`. Takes `{ username, password, terminalCode }` (`terminalCode` optional) and returns `{ token, employeeId, username, roleId, roleName, permissions, terminalId }` — the same employee/role info the JWT claims carry, returned directly so mobile/desktop don't need to decode the token just to populate their own UI/session cache (see "Desktop (galfield-pos) — sesión cacheada" / "Mobile — sesión" in the spec).

- **Mobile login** (no `terminalCode`): requires `employee_roles.can_login_mobile = true` on the employee's role; `terminalId` is absent from both the response and the JWT.
- **Desktop login** (`terminalCode` present): requires `can_login_desktop = true` **and** an `employee_terminals` row assigning this employee to that specific terminal (resolved via `TerminalRepository#findByTerminalCode`, matched by id against `Employee.terminals` — not `Set#contains`/entity `equals`, since `Terminal` has no `equals`/`hashCode` override and relying on Hibernate's session-identity-map behavior there would be implicit and fragile). `terminalId` is included in both the response and the JWT (`terminalId` claim).
- **Every failure mode returns the exact same generic message** (`AuthenticationFailedException` → 401, "Usuario, clave o terminal inválidos") — unknown username, inactive employee (`employees.is_active = false`), wrong password, role not allowed to log in on the side attempted, unknown `terminalCode`, or a real terminal the employee just isn't assigned to. Deliberately never reveals which check failed (e.g. "wrong password" vs. "unknown user" would let an attacker enumerate valid usernames).
- **JWT claims** (`JwtService#issueToken`, HS256 via `app.jwt.secret`): `sub` (employeeId), `username`, `roleId`, `roleName`, `permissions` (the role's `Map<String,Boolean>`, same conversion as `/api/employee-roles` via the shared `util.PermissionsJson`), `canLoginMobile`, `canLoginDesktop`, `terminalId` (desktop logins only), `exp`. **`canLoginMobile`/`canLoginDesktop` aren't in the spec's example claims block** — added so `SecurityConfig`'s authorization rules (below) can check "is this an administrative role" per-request straight from the token, with no DB round trip and no falling back to comparing `roleName` strings (the spec's Decisions explicitly reject hardcoding "Administrador" by name).
- **`exp` is always the next midnight in `America/Bogota`, not a fixed duration from issuance** — a login at 20:00 expires at 00:00 the same night; a login at 08:00 expires at 00:00 that night (~16h later). Computed as `ZonedDateTime.now(BOGOTA).toLocalDate().plusDays(1).atStartOfDay(BOGOTA)` — always explicitly zoned, never the server's default timezone (see this spec's Risks table: a misconfigured server timezone must not shift what "midnight" means here).

## Authorization (`SecurityConfig`, `JwtAuthenticationFilter`)

The real `SecurityFilterChain` for spec `01-login-empleados-roles`, replacing the temporary "everything behind Spring Boot's default HTTP Basic" state that existed from this spec's step 4 (when `spring-boot-starter-security` was first added) through step 6.

- **`JwtAuthenticationFilter`** (`security/`, runs via `addFilterBefore(..., UsernamePasswordAuthenticationFilter.class)`): reads `Authorization: Bearer <jwt>`, and if present and valid, converts the token's claims into a `UsernamePasswordAuthenticationToken` with synthetic `GrantedAuthority`s — `ADMIN` (from `canLoginMobile`), `DESKTOP` (from `canLoginDesktop`), and one `PERM_<module>` per `permissions` entry that's `true` (e.g. `PERM_pos`, `PERM_reportes`) — so `SecurityConfig`'s rules read as plain `hasAuthority`/`hasAnyAuthority` checks. A missing, malformed, or expired token is **not** rejected inside the filter — it just leaves the request unauthenticated (`SecurityContextHolder` untouched/cleared) and lets it continue down the chain, so a route that doesn't require auth (`/api/auth/login`) still works with no token, and a route that does require it gets a clean 401 from the entry point below rather than an exception bubbling out of the filter itself.
- **`SecurityConfig`**: stateless (`SessionCreationPolicy.STATELESS`, CSRF disabled — there's no cookie/session to protect), and its `authorizeHttpRequests` rules are copied **1:1 from the spec's endpoint→authorization table** — that table is treated as the single source of truth (per the spec's own Risks entry: a misconfigured chain silently leaves a sensitive endpoint unprotected), so **any endpoint not explicitly listed there falls through to `anyRequest().denyAll()`**, not an implicit "any authenticated user is fine." Three deliberate additions beyond the table's literal text: `/swagger-ui/**`/`/v3/api-docs/**` are `permitAll()` (the table doesn't mention them at all; leaving Swagger itself locked out would make it useless as a manual-testing tool for an internal single-business API), `POST /api/sales/**` (not just the exact `POST /api/sales`) requires `PERM_pos` so the sale-cancellation endpoints (`/api/sales/{id}/cancel`, `/api/sales/by-client-event/{id}/cancel` — not itemized separately in the spec's table) inherit the same gate as recording a sale instead of falling through to `denyAll()`, and **`GET /api/invoice-numbering-ranges/by-terminal/**` is carved out of the broader `/api/invoice-numbering-ranges/**` `ADMIN`-only bucket to also accept `DESKTOP`** (`hasAnyAuthority(ADMIN, DESKTOP)`, matched *before* the broader rule — `authorizeHttpRequests` is first-match-wins) — found and fixed while wiring up `apps/galfield-pos`'s side of this spec (step 16): the table groups this endpoint under the same prefix as the admin-only range CRUD, but it's what a desktop terminal (`apps/galfield-pos`'s `invoice_numbering.rs::sync_invoice_numbering`) calls to pull *its own* assigned range — and a desktop-only login (Cajero, `canLoginDesktop=true`) never gets `ADMIN`. Left as literally-ADMIN-only, invoice numbering sync would have 403'd for every real terminal the moment step 16 started attaching real JWTs to outbound calls.
- **401 vs. 403, in the same JSON shape as `GlobalExceptionHandler`**: `authenticationEntryPoint` (missing/invalid token on a protected route) → 401; `accessDeniedHandler` (valid token, wrong/missing authority) → 403. Both write directly to the raw `HttpServletResponse` (they run outside Spring MVC's normal dispatch — a rejected request never reaches a controller, so `@RestControllerAdvice` never triggers) via the same `ErrorResponseBody.build(status, message)` that `GlobalExceptionHandler` now also delegates to, extracted specifically so a security-layer rejection and a controller-thrown exception produce byte-identical bodies.

### Payment method image

`payment_methods_images` (migration `V3__payment_methods_images.sql`) is a 1:1 join to `attach_files`, same pattern as `product_images`/`product_variants_images` — `PaymentMethod.image` is a `@OneToOne(mappedBy = "paymentMethod", cascade = ALL, orphanRemoval = true)`. `MinioService#uploadPaymentMethodImage` uploads under the object key prefix `files/payment_method/<method-name-slug>/<uuid>.ext` (singular `payment_method`, unlike the pluralized product paths). `PaymentMethodService` mirrors `ProductService`'s image flow: on create/update, an uploaded `image` replaces any existing one (old MinIO object deleted after the new one is attached); unlike products, `/api/payment-methods` hard-deletes, so `deletePaymentMethod` also explicitly deletes the MinIO object and the `attach_files` row for the image (products never do this since they're soft-deleted and never reach this code path).

**Indispensable:** `ProductService` hard-codes the location named `Bogotá - Chapinero` (`DEFAULT_LOCATION_NAME`) as the inventory location for every product/variant created or updated through `/api/products` — it's not configurable yet. Renaming or deleting that location breaks product creation/stock updates (`ResourceNotFoundException` on create; deletion is blocked by the FK-conflict 409 once any inventory row references it, but renaming it isn't blocked by anything). If you need a different default location, update `ProductService.DEFAULT_LOCATION_NAME` too, don't just change the row via `/api/locations`.

## Invoice numbering ranges (`/api/invoice-numbering-ranges`)

`InvoiceNumberingRangeController` → `InvoiceNumberingRangeService` → `InvoiceNumberingRangeRepository`, backed by `invoice_numbering_ranges` (migration `V5__invoice_numbering_ranges.sql`). This is the cloud side of DIAN-compliant invoice numbering: each desktop POS terminal (`apps/galfield-pos`) is authorized a non-overlapping numeric range (`prefix` + `[range_start, range_end]`), assigned centrally from the mobile app's Configuración → Numeración de facturas (`apps/galfields-mobile`'s CLAUDE.md), so no two terminals can ever mint the same invoice number while working offline.

**Migrated (spec `01-login-empleados-roles`):** `V9__employee_auth.sql` replaced the loose `terminal_code` string with a real `terminals` table (see "Terminals CRUD" above) and swapped `invoice_numbering_ranges.terminal_code` for a `terminal_id` FK (`UNIQUE`, same one-row-per-terminal invariant `terminal_code UNIQUE` used to enforce).

- One row per terminal — `terminal_id UNIQUE`, resolved to a real `terminals` row (`Terminal`, not a loose string) via `TerminalRepository`.
- Standard CRUD, same shape as `/api/categories`/`/api/brands`/`/api/locations` above: `POST`/`PUT` take `{ terminalId, prefix, rangeStart, rangeEnd }`; response echoes back `terminalId` **and** the denormalized `terminalCode` (same convention as `ProductResponse.categoryId`/`categoryName`), so callers don't need a second round trip to `/api/terminals` just to display which terminal a range belongs to. `GET` (list, ordered by `terminal.terminalCode`)/`GET {id}`/`PUT {id}`/`DELETE {id}`. An unknown `terminalId` on create/update 404s (`ResourceNotFoundException`); a `terminalId` already assigned a range falls through to the generic `DataIntegrityViolationException` → 409 handling (the `UNIQUE` constraint on `terminal_id`) — no bespoke uniqueness check needed.
- **`GET /api/invoice-numbering-ranges/by-terminal/{terminalCode}`** is the one non-CRUD addition: what the desktop POS calls to pull its own assigned range by the code it has configured locally — it only knows its own `terminal_code`, never a numeric `terminalId`/`rangeId`, so `InvoiceNumberingRangeService#getRangeByTerminalCode` resolves `terminal_code` → `Terminal` → range via `TerminalRepository`/`InvoiceNumberingRangeRepository#findByTerminal_TerminalId` internally. Same external contract as before the `terminal_id` migration: 404 (via `ResourceNotFoundException`, same message) whether the `terminal_code` itself is unknown or it's a real terminal with no range assigned yet.
- This endpoint only hands out the *authorized range* — it does not track which numbers within that range have actually been consumed. Each terminal owns that bookkeeping locally (`apps/galfield-pos`'s `app_settings` key `invoicing.next_number`) and is expected not to re-pull/reset until its current range is exhausted; the cloud has no visibility into how much of a range is left.

## Inventory adjustment endpoint (`POST /api/inventory/adjustments`)

`InventoryController` → `InventoryService` → `InventoryRepository`/`StockAdjustmentRepository`. This is how offline-first clients (currently: the desktop POS's outbox, `apps/galfield-pos`'s `sales_sync.rs` — see that repo's CLAUDE.md) report stock changes that already happened locally (a sale decrements; a future return/manual correction could increment), batched one call per sale. **Note:** the POS outbox now calls `POST /api/sales` instead (see "Sale recording endpoint" below), which applies this same adjustment internally — this endpoint is still hit directly for that, and remains available as a standalone primitive for any future caller that only needs a stock delta, not a full sale record:

```json
{
  "clientEventId": "<uuid the client generated for this sale>",
  "items": [
    { "variantId": 12, "quantityDelta": -2 },
    { "variantId": 45, "quantityDelta": -1 }
  ]
}
```

- **Idempotent per `(clientEventId, variantId)`** — `stock_adjustments` (migration `V2__stock_adjustments.sql`) has a `UNIQUE (client_event_id, variant_id)` constraint backing `existsByClientEventIdAndVariant_VariantId`. A retried batch (client applied it locally but never saw the response, or only part of a previous batch succeeded before a crash) replays `alreadyProcessed: true` per already-seen item instead of double-applying — safe to retry the exact same request any number of times.
- **Scoped to `DEFAULT_LOCATION_NAME`** ("Bogotá - Chapinero"), same as every other inventory write in this codebase — there's no per-request location, and no multi-location support yet (see the indispensable note above).
- **Negative resulting stock is allowed, not rejected.** The physical sale already happened by the time this is called (e.g. two terminals both sold the last unit before either synced) — recording an oversell truthfully is more useful than rejecting a call that can't undo something that already occurred in the real world.
- `resultingQuantity` in the response is `inventory.quantity_on_hand` *after* applying that item (or the value from the original application, on an idempotent replay) — callers can use it to detect oversells after the fact, not to gate anything server-side.
- Unlike `/api/categories`/`/api/brands`/`/api/locations`, this is not a CRUD resource — there's no `GET`/list endpoint, since `stock_adjustments` is an append-only audit log of what's already been applied to `inventory`, not something clients browse.

## Sale recording endpoint (`POST /api/sales`)

`SalesController` → `SalesService` → `SalesTransactionRepository`/`SaleItemRepository`/`PaymentRepository`. This is how a POS terminal reports a **completed sale** — until this endpoint existed, `sales_transactions`/`sale_items`/`payments` had JPA entities and repositories but zero controllers/services using them; the only thing terminals reported was the stock delta above. `POST /api/sales` replaces that for sale reporting (it doesn't remove `/api/inventory/adjustments` — that stays a valid generic primitive, and `SalesService` reuses it internally, see below):

```json
{
  "clientEventId": "<uuid the client generated for this sale>",
  "items": [
    { "variantId": 12, "productUnitId": 45, "quantity": 2, "unitPrice": 4500.00, "subtotal": 9000.00 }
  ],
  "payments": [
    { "paymentMethodId": 3, "amount": 9000.00 }
  ],
  "discountAmount": 0,
  "totalAmount": 9000.00
}
```

- **Idempotent per `clientEventId`** at the whole-transaction level (`sales_transactions.client_event_id UNIQUE`, migration `V4__sales_recording.sql`) — a retried report (terminal applied it locally, never saw the response) returns the already-created transaction (`alreadyProcessed: true`) instead of duplicating the sale, its items, or its payments.
- **Atomically applies the matching stock adjustment** by building a `StockAdjustmentBatchRequest` (same `clientEventId`, `quantityDelta = -quantity` per line) and calling `InventoryService.applyAdjustments` directly, in the same `@Transactional` method — no duplicated stock-decrement logic, and the sale record + stock decrement either both happen or neither does.
- **Employee attribution is a placeholder.** The desktop POS has no real per-cashier login, so every sale is attributed to one seeded employee (`username = 'pos-terminal'`, seeded by `V4__sales_recording.sql` along with a placeholder `employee_roles`/`attach_files` row it needs to satisfy the `BIGSERIAL` NOT-NULL gotcha above). This employee can't log in anywhere — there's no employee auth endpoint in this codebase at all yet — it exists purely as a valid FK target. Revisit `SalesService.DEFAULT_EMPLOYEE_USERNAME` when real cashier login exists.
- `taxAmount` is always `0` — the local POS schema has no IVA/tax breakdown to report, so there's nothing to send.
- Scoped to the same `DEFAULT_LOCATION_NAME` as everything else (see above) — no per-request location yet.
- **`invoicePrefix`/`invoiceNumber`** (migration `V8__sales_invoice_number.sql`) are the DIAN-authorized invoice number the terminal snapshotted at sale creation (`apps/galfield-pos`'s `invoices.rs::create_sale`, sent by `sales_sync.rs`) — nullable on both the request and `sales_transactions`, since a transaction reported before this column existed (or, in principle, a terminal reporting without numbering configured) has nothing to put there. `GET /api/reports/invoices` / `GET /api/reports/invoices/{id}` echo both fields back; `apps/galfields-mobile`'s Historial de facturas falls back to displaying `#{transactionId}` when `invoiceNumber` is null.
- **`productUnitId`** per line (migration `V10__product_units.sql`, spec `03-unidades-venta-conversion`, step 3) is **optional** — a line with none sells at the base unit, same behavior as before this feature existed (`SaleItem.unitName`/`conversionFactor` default to `'Unidad'`/`1`, same column defaults old rows got from the migration's backfill). When present, `SalesService#recordSale` resolves it via `ProductUnitRepository#findByProductUnitIdAndVariant_VariantId` (**validates it actually belongs to the line's `variantId`** — a mismatched pair 404s rather than silently trusting the client), snapshots `productUnit`/`unitName`/`conversionFactor` onto the `SaleItem` (same "freeze at sale time" pattern as `unitPrice`), and **converts `quantity` to base units for the stock decrement**: `StockAdjustmentItemRequest`'s delta is `-quantity * conversionFactor`, not `-quantity` — `quantity` on a `SaleItem` now means "how many of the sold unit" (e.g. 2 "Media"), not necessarily base units. **Cancelling a sale (`SalesService#cancel`) reverses using the same `quantity * conversionFactor` multiplication** — a plain `item.getQuantity()` reversal (the pre-this-spec code) would under-restock any sale that used a non-base unit. `GET /api/reports/invoices/{id}` (`InvoiceLineResponse`) echoes `unitName`/`conversionFactor` back per line, same convention as `unitPrice`/`subtotal`.

### Cancelling a sale (`POST /api/sales/{transactionId}/cancel`, `POST /api/sales/by-client-event/{clientEventId}/cancel`)

Two entry points into the same `SalesService#cancel` logic, because the two callers each only know one identifier: mobile's Historial de facturas already has `transactionId` (from `GET /api/reports/invoices`); the desktop POS terminal never learns its own transaction's `transactionId` (it's never returned to nor stored by `apps/galfield-pos`'s `sales_sync.rs`) — the only thing it has is the `clientEventId` (`sync_uuid`) it originally reported the sale under, which **is** this transaction's `clientEventId`.

- `cancelledAt` (migration `V6__sales_cancellation.sql`) is a separate axis from `paymentStatus` — a `Paid` sale that gets voided is still "was Paid", just cancelled now; `paymentStatus` is never touched by cancellation. Cancelling an already-cancelled sale throws `InvalidStateException` → 409.
- **Reverses the stock decrement** the same way `recordSale` applied it: builds a `StockAdjustmentBatchRequest` from the transaction's `sale_items` (`SaleItemRepository.findByTransaction_TransactionId`, one `StockAdjustmentItemRequest(variantId, +quantity)` per line — positive this time) and calls `InventoryService.applyAdjustments`. **Must use a different `clientEventId`** than the original sale (`"cancel-" + clientEventId`) — `stock_adjustments`' idempotency key is `(clientEventId, variantId)`, so reusing the original would look "already processed" to `applyOne` and the reversal would silently never apply.
- Both endpoints return `204 No Content` on success — nothing to hand back beyond confirmation.
- No cancellation reason/note field, and no time limit on when a sale can be cancelled — kept deliberately minimal until a real need for either surfaces.

## Reports access code (`/api/reports-access-code`)

`ReportsAccessCodeController` → `ReportsAccessCodeService` → `ReportsAccessCodeRepository`, backed by `reports_access_codes` (migration `V7__reports_access_code.sql`). This is the gate that keeps cashiers from freely browsing financial reports on the desktop POS: the mobile app's Configuración → Acceso a Reportes generates a 6-digit code on demand, and the desktop POS (`apps/galfield-pos`'s `reports_access.rs`) validates whatever the cashier types against it before letting them into the Reportes module — every time they enter it, not just once per session.

- **`POST /api/reports-access-code`** (no body) — generates a fresh random 6-digit code (`SecureRandom`, zero-padded) and **inserts a new row** rather than updating one; called from mobile when the manager taps "Generar código".
- **`POST /api/reports-access-code/validate`** (`{ code }`) — returns `{ valid }`, comparing against the row with the latest `generated_at` (`findFirstByOrderByGeneratedAtDesc`).
- **Append-only, no expiry column on purpose**: the code is meant to stay valid until a new one is generated, not rotate on a timer — generating a new code implicitly invalidates the previous one simply by becoming the new "most recent" row. Older rows are never deleted (a harmless growing audit log), but only the latest one is ever checked.
- **Global, not per-terminal**: unlike invoice numbering ranges above, there's exactly one active code for the whole business — no `terminal_code` column, and no way to have two valid codes at once.
- **No auth on either endpoint** — matches the rest of this backend today (no employee login/auth exists anywhere in this codebase yet, see the sale-recording section's "Employee attribution is a placeholder" note). Revisit if real auth is ever added.

## Report endpoints (`GET /api/reports/*`)

`ReportController` → `ReportService`, backing the mobile app's report screens (see `apps/galfields-mobile`'s CLAUDE.md). All date-ranged reports take `from`/`to` as plain `YYYY-MM-DD` dates (inclusive); omitting both defaults to today, omitting just `from` scopes to the single `to` day.

- `GET /api/reports/sales-summary?from=&to=` — total sales / transaction count / average ticket over the range. Mobile's "Ventas del día" calls this with no params (defaults to today).
- `GET /api/reports/sales-by-payment-method?from=&to=` — sum/count grouped by `payment_methods`. Backs both "Ventas por método de pago" **and** "Cierre de caja" — the latter is this same aggregate called with `from=to=<today>`; there's deliberately no separate cash-session endpoint (no open/close-shift concept exists — "cierre de caja" here means a same-day payment-method summary, not a formal register session).
- `GET /api/reports/invoices?from=&to=&page=&size=` — paginated (`PagedModel`, same convention as `ProductController#list`) invoice list for "Historial de facturas". `GET /api/reports/invoices/{transactionId}` returns the full line-item + payment breakdown for one invoice.
- `GET /api/reports/inventory?page=&size=` — current stock per variant/location, join across `inventory`/`product_variants`/`products`/`categories`/`locations`. Doesn't depend on anything above — works today regardless of whether any sale has ever been reported.
- `GET /api/reports/low-stock?threshold=&page=&size=` — same shape, filtered to `quantity_on_hand <= threshold` (default `5`, matching the desktop POS's own hardcoded low-stock threshold; `products`/`product_variants` still have no per-product minimum-stock column).

`ReportService` is class-annotated `@Transactional(readOnly = true)` — this isn't just a style choice: `RoutingDataSource` (see `config/`) routes any read-only transaction to the Postgres **replica**, primary only for writes. Any new report method added here should stay read-only for that reason; don't add a write inside `ReportService` without moving it elsewhere.

## Image compression utility

`co.com.galfields.pos.util.ImageCompressor` (Thumbnailator-backed, with the `org.sejda.imageio:webp-imageio` ImageIO plugin providing the WebP writer SPI) downscales images to a 1600px max dimension (never upscales) and **re-encodes every JPEG/PNG upload to WebP** (~80% quality) before upload — not just compresses in its original format. GIF/SVG/other unrecognized content types pass through untouched. It's a plain `@Component` with a single `compress(MultipartFile): CompressedImage` method (`CompressedImage` is a `record(byte[] data, String contentType, String extension)`), meant to be called by any service before handing bytes to `MinioService` — not specific to products. `ProductService` (product + variant images) and `PaymentMethodService` (payment method image) both call it; any new file-upload feature should reuse it too rather than compressing/uploading directly.

**Why `CompressedImage` carries its own `contentType`/`extension`:** since the output format no longer matches the original upload's format (a `.png` in can become a `.webp` object), the object key extension and the `attach_files.mime_type` value must come from what `ImageCompressor` actually produced, not from `MultipartFile.getOriginalFilename()`/`getContentType()`. `MinioService#uploadProductImage`/`uploadVariantImage`/`uploadPaymentMethodImage` take a `CompressedImage` (not a raw `MultipartFile` + `byte[]`) for exactly this reason — don't revert to deriving the extension from the original filename.

Images uploaded before this change keep their original JPEG/PNG object key and `mime_type` — nothing retroactively re-encodes existing `attach_files` rows, so the CDN will keep serving a mix of `.jpg`/`.png` (legacy) and `.webp` (new) URLs. This is expected, same as the pre-`files/`-prefix rows noted below.

## API documentation (Swagger / OpenAPI)

springdoc-openapi is wired up with no extra per-endpoint annotations required — it reflects existing `@RestController`/DTOs into the spec automatically:

- Swagger UI: `http://localhost:8080/swagger-ui.html` (redirects to `/swagger-ui/index.html`)
- Raw OpenAPI JSON: `http://localhost:8080/v3/api-docs`
- Metadata (title/description/version) lives in `co.com.galfields.pos.config.OpenApiConfig`; paths/sorting are set under `springdoc.*` in `application.properties`.

Add `@Operation`/`@Schema` annotations on controllers/DTOs as the API grows if the auto-generated descriptions aren't clear enough; not required for new endpoints to show up.

## MinIO wiring gotcha

Bucket-existence bootstrapping lives in a separate `MinioBucketInitializer` component that takes `MinioClient` as a constructor dependency, not in a `@PostConstruct` inside `MinioConfig` itself. Do not merge that `@PostConstruct` back into `MinioConfig` and call the `@Bean` method directly from it — Spring Boot's default circular-reference guard rejects that (a config bean depending on its own factory method during initialization) and the app fails to start.

## Image URLs / CDN (indispensable)

`ProductResponse.imageUrl` / `ProductVariantResponse.imageUrl` are **plain, unsigned URLs** — `minio.public-endpoint + "/" + objectKey`, built by simple string concatenation in `MinioService#getPublicUrl`. There is no SigV4 signing, no expiry, no query string: the bucket is served publicly through the CDN, so a browser/app can hit the URL directly and forever (no re-fetching a fresh link before it expires).

This depends on two things being true, both indispensable:

1. **`MINIO_PUBLIC_ENDPOINT` must be set in production** to `https://cdn.galfields.kinforgeworks.com` (see `application.properties`; defaults to `minio.endpoint` so local dev works without a CDN in front of the dev MinIO container).
2. **The bucket must allow anonymous public GET** through the CDN (e.g. a public-read bucket policy, or the CDN injects its own credentials transparently) — since these URLs carry no signature, whatever serves them at `minio.public-endpoint` has to authorize the request itself. This is infra config outside this repo; if images 403/404 through the CDN but upload fine, check the bucket policy / CDN origin config first, not the app code.

Object keys uploaded via `MinioService` are prefixed with `files/` (e.g. `files/bebidas/camiseta-basica/<uuid>.jpg`), so the final public URL looks like `https://cdn.galfields.kinforgeworks.com/files/bebidas/camiseta-basica/<uuid>.jpg`. Rows created before this prefix existed don't have it retroactively — their stored `attach_files.url` (and therefore their public URL) simply lacks the `files/` segment; this is expected for old data, not a bug.

`MinioConfig` only has a single `MinioClient` bean now (used for `putObject`/`removeObject`) — there is deliberately no second client for signing, since nothing is signed anymore. Don't reintroduce presigned URLs / a `publicMinioClient` bean here without discussing it first; a previous version of this did exactly that and it was reverted because it's unnecessary complexity for public catalog images (presigned URLs make sense for private/sensitive files, not product photos).

## Local infra (Postgres + MinIO + app)

**`compose.yaml` in this directory** is a self-contained local stack — the shared `../postgrest/compose.yaml`/`../../minio/compose.yaml` this section used to point at live in a separate infra repo (`infra-repo-kinforgeworks`) that isn't checked out alongside this one, so those paths don't resolve here; use this file instead for local dev/validation. Three services: `postgres`, `minio`, and `app` (builds the existing `Dockerfile` — multi-stage, `eclipse-temurin:21-jdk` build → `21-jre` runtime, already in this directory and previously unused by anything else here).

```bash
podman compose up -d                    # infra + app (builds the image first time / after --build)
podman compose up -d postgres minio     # infra only — pair with .env.local.example + `gradle bootRun`
                                         # on the host instead, for faster iteration (no image
                                         # rebuild per code change; see below)
podman compose up -d --build            # rebuild the app image after a code change, then start
podman compose down                     # stop, keep data
podman compose down -v                  # stop and wipe all volumes
```

Single Postgres instance (not a real primary/replica pair — `DB_URL_PRIMARY`/`DB_URL_REPLICA` both point at it, which is fine for local dev since `RoutingDataSource` just needs two reachable `DataSource`s at boot, not actual replication), on host port `5433`; MinIO on `9000`/`9001` (console); `app` on `8080`. `app`'s `depends_on` uses `condition: service_healthy` for both — **must** wait for MinIO specifically, not just Postgres: `MinioBucketInitializer`'s `@PostConstruct` fails the whole Spring context if MinIO isn't reachable yet, not just an isolated feature. Inside the compose network, `app` reaches the other two by **service name** (`postgres`, `minio`), not `localhost` — its `DB_URL_PRIMARY`/`MINIO_ENDPOINT` are set accordingly in `compose.yaml` directly (a fixed, committed dev-only `JWT_SECRET` too — same "not a real secret, just this local stack" status as the Postgres/MinIO credentials alongside it).

**`.env.local.example`** (also in this directory) is for the host-side `gradle bootRun` path instead (skip the `app` service, `podman compose up -d postgres minio` only) — same env vars `application.properties` requires, but pointed at `localhost` + the host-mapped ports above, since a host process doesn't resolve compose service names. `source .env.local.example` before `gradle bootRun` (or `./gradlew bootRun` once the wrapper script is regenerated — see "Commands" above; `gradle` — the system install — works identically for this). Prefer this path over `app` while actively changing code — no image rebuild between runs.

**Validated end-to-end against this stack** (2026-07-28, spec `01-login-empleados-roles`): fresh DB, all 9 migrations apply cleanly including `V9__employee_auth.sql`'s bootstrap admin, Hibernate's `ddl-auto=validate` passes against every entity, `POST /api/auth/login` issues a correctly-shaped JWT (`exp` lands on the next Bogota midnight), missing/wrong-permission/wrong-password paths return the exact 401/403 bodies documented above, and the `DESKTOP`-authority carve-out on `GET /api/invoice-numbering-ranges/by-terminal/**` (see "Authorization" above) actually works — a Cajero-only JWT reaches the controller (404, not 403) instead of being rejected by the filter chain. This is also what surfaced the Jackson 3 bug immediately below; the app had never actually been booted since `SecurityConfig` was added in step 7.

**Bug found and fixed by that first real boot — Spring Boot 4.1 defaults to Jackson 3 (`tools.jackson.*`), not classic Jackson 2 (`com.fasterxml.jackson.*`):** `PermissionsJson`, `EmployeeRoleService`, `AuthService`, and `SecurityConfig` all originally injected `com.fasterxml.jackson.databind.ObjectMapper` — compiles fine (that groupId is still present transitively, pulled in by springdoc/swagger-core, which hasn't moved to Jackson 3 itself yet), but fails at boot with `UnsatisfiedDependencyException: No qualifying bean of type 'com.fasterxml.jackson.databind.ObjectMapper'`, because `JacksonAutoConfiguration` (`org.springframework.boot.jackson.autoconfigure`) only registers a `tools.jackson.databind.json.JsonMapper` bean now. Fixed by switching all four files to `tools.jackson.databind.ObjectMapper`/`tools.jackson.core.type.TypeReference`/`tools.jackson.core.JacksonException` (replaces `JsonProcessingException` — Jackson 3 made it **unchecked**, so `catch` blocks changed type but methods didn't need new `throws` clauses). If you add another `ObjectMapper` injection anywhere in this codebase, use the `tools.jackson.*` one — `com.fasterxml.jackson.*` types will compile (the jar's on the classpath) but silently have no bean to autowire until something actually boots the app and hits this exact error.
