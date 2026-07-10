# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project

Spring Boot 4.1.0 backend for a POS (point of sale) system, part of the larger Galfields project. Java 21, built with Gradle.

Base package: `co.com.galfields.pos`. Entry point: `src/main/java/co/com/galfields/pos/PosApplication.java`.

The project follows conventional Spring Boot layering (`controller` / `service` / `repository` / `entity` / `dto` / `exception` / `config` packages under `co.com.galfields.pos`). Entities cover the full POS domain (products, variants, inventory, sales, purchase orders, employees, customers, suppliers); only the products/categories/brands/locations/payment-methods/inventory slice currently has controllers and services wired up.

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

Corresponding test-scoped starters (`spring-boot-starter-data-jpa-test`, `spring-boot-starter-restclient-test`) are included for integration testing against JPA repositories and REST clients.

## Configuration

`src/main/resources/application.properties` sets the app name/port, the Postgres datasource, Flyway, and MinIO settings (see below). Add further profile-specific overrides in `application-<profile>.properties` as needed.

## Database schema

`doc/data_base.sql` is the authoritative dump of the schema actually deployed on the cloud Postgres cluster — treat it as ground truth over the Flyway migrations when the two disagree. The cluster's Postgres pod already had this schema created by its own init script before Flyway was introduced, so `V1__init_schema.sql` is now a no-op placeholder and Flyway is baselined (`spring.flyway.baseline-on-migrate=true`, `spring.flyway.baseline-version=1`) rather than replaying `CREATE TABLE` against tables that already exist. New schema changes should land as a new dated migration file *and* an updated `doc/data_base.sql`.

Notable shapes to remember when writing entities/queries against this schema:

- `payment_status_enum` and `purchase_order_status_enum` are real Postgres `ENUM` types (not `VARCHAR` + `CHECK`) — map Java enums with `@JdbcTypeCode(SqlTypes.NAMED_ENUM)` plus `columnDefinition` set to the Postgres type name, or Hibernate's schema validation will fail at boot.
- Images are not stored as a plain string column on `products`/`product_variants`. There's a generic `attach_files` table (name/url/mime_type/size), joined via the 1:1 tables `product_images` and `product_variants_images`, and referenced directly from `employees.logo_image`.
- `product_variants` has no `attribute_name`/`attribute_value` columns — those live in the separate `variant_attributes` table (one variant can have many attributes, e.g. color + size), unique per `(variant_id, attribute_name)`.
- **Gotcha:** several FK columns are declared `BIGSERIAL` in `doc/data_base.sql` instead of plain `BIGINT` (`products.category_id`/`brand_id`, `employees.role_id`/`logo_image`, `sales_transactions.customer_id`). Postgres makes `SERIAL`/`BIGSERIAL` columns `NOT NULL` unconditionally, so these are mandatory in the live DB even where the design clearly intended them optional (e.g. a sale with no customer, an employee with no logo). `ProductRequest.categoryId`/`brandId` are `@NotNull` to match this reality. If you touch employees/sales code, check the actual `\d <table>` output before assuming a FK is nullable — don't trust the DDL's absence of `NOT NULL`. Fixing this upstream (swap to `BIGINT`) requires a coordinated migration, not just an entity change.
- **Local dev seed gap:** `locations` and `brands` ship empty in a fresh local DB (the old `V2__seed_categories_and_default_location.sql` was dropped when Flyway was baselined). `ProductService` hard-depends on a location named `Bodega Principal` existing — product creation throws `ResourceNotFoundException` until you insert one (and at least one brand, since it's now required too).

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

## Category / Brand / Location / Payment Method CRUD endpoints

`/api/categories`, `/api/brands`, `/api/locations`, and `/api/payment-methods` are CRUDs (`*Controller` → `*Service` → `*Repository`). The first three take a plain JSON body; `/api/payment-methods` is multipart because it also carries an optional image (see below) — otherwise all four share the same shape:

- `/api/categories`: `POST`/`PUT` take `{ name, description }` (`name` required); `categories` has a `description` column.
- `/api/brands`: `POST`/`PUT` take `{ name }` only (`name` required) — `brands` has **no** `description` column (see `doc/data_base.sql`), don't add one to `BrandRequest`/`BrandResponse` without a matching migration first.
- `/api/locations`: `POST`/`PUT` take `{ name, address, phone }` (`name` required, `address`/`phone` optional).
- `/api/payment-methods`: `POST`/`PUT` are `multipart/form-data` — a `paymentMethod` JSON part `{ methodName, active }` (both required) plus an optional `image` file part, same two-part shape as `/api/products`. Unlike the other three, `payment_methods` *does* have an `is_active` column, so `active` is a plain required field on the request/response instead of a separate deactivate endpoint like products use.

All four: `GET` lists all or fetches by id, `DELETE` hard-deletes (none of `categories`/`brands`/`locations` have an `is_active`/soft-delete column, unlike products; `payment_methods` has one but `DELETE` still hard-deletes rather than flipping it). None of `categories.name` / `brands.name` / `locations.name` / `payment_methods.method_name` has a `UNIQUE` constraint in the DB, so duplicate names are allowed on purpose (no app-level uniqueness check).

Deleting a row still referenced by a product/inventory/sale/payment (FK, no `ON DELETE` clause → default `RESTRICT`) returns a clean 409 instead of a raw 500: `GlobalExceptionHandler` catches `DataIntegrityViolationException` generically, so this covers any FK-constrained delete across all four, and any future one.

### Payment method image

`payment_methods_images` (migration `V3__payment_methods_images.sql`) is a 1:1 join to `attach_files`, same pattern as `product_images`/`product_variants_images` — `PaymentMethod.image` is a `@OneToOne(mappedBy = "paymentMethod", cascade = ALL, orphanRemoval = true)`. `MinioService#uploadPaymentMethodImage` uploads under the object key prefix `files/payment_method/<method-name-slug>/<uuid>.ext` (singular `payment_method`, unlike the pluralized product paths). `PaymentMethodService` mirrors `ProductService`'s image flow: on create/update, an uploaded `image` replaces any existing one (old MinIO object deleted after the new one is attached); unlike products, `/api/payment-methods` hard-deletes, so `deletePaymentMethod` also explicitly deletes the MinIO object and the `attach_files` row for the image (products never do this since they're soft-deleted and never reach this code path).

**Indispensable:** `ProductService` hard-codes the location named `Bodega Principal` (`DEFAULT_LOCATION_NAME`) as the inventory location for every product/variant created or updated through `/api/products` — it's not configurable yet. Renaming or deleting that location breaks product creation/stock updates (`ResourceNotFoundException` on create; deletion is blocked by the FK-conflict 409 once any inventory row references it, but renaming it isn't blocked by anything). If you need a different default location, update `ProductService.DEFAULT_LOCATION_NAME` too, don't just change the row via `/api/locations`.

## Inventory adjustment endpoint (`POST /api/inventory/adjustments`)

`InventoryController` → `InventoryService` → `InventoryRepository`/`StockAdjustmentRepository`. This is how offline-first clients (currently: the desktop POS's outbox, not yet built — see that repo's CLAUDE.md) report stock changes that already happened locally (a sale decrements; a future return/manual correction could increment), batched one call per sale:

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
- **Scoped to `DEFAULT_LOCATION_NAME`** ("Bodega Principal"), same as every other inventory write in this codebase — there's no per-request location, and no multi-location support yet (see the indispensable note above).
- **Negative resulting stock is allowed, not rejected.** The physical sale already happened by the time this is called (e.g. two terminals both sold the last unit before either synced) — recording an oversell truthfully is more useful than rejecting a call that can't undo something that already occurred in the real world.
- `resultingQuantity` in the response is `inventory.quantity_on_hand` *after* applying that item (or the value from the original application, on an idempotent replay) — callers can use it to detect oversells after the fact, not to gate anything server-side.
- Unlike `/api/categories`/`/api/brands`/`/api/locations`, this is not a CRUD resource — there's no `GET`/list endpoint, since `stock_adjustments` is an append-only audit log of what's already been applied to `inventory`, not something clients browse.

## Image compression utility

`co.com.galfields.pos.util.ImageCompressor` (Thumbnailator-backed) downscales images to a 1600px max dimension (never upscales) and re-encodes JPEGs at ~82% quality before upload; PNGs and unrecognized content types pass through mostly as-is. It's a plain `@Component` with a single `compress(MultipartFile): byte[]` method, meant to be called by any service before handing bytes to `MinioService` — not specific to products. `ProductService` calls it for both product and variant images.

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

## Local infra (Postgres + MinIO)

There is no compose file in this directory for local Postgres/MinIO — use the shared ones instead, so local dev matches the same topology (`gf-db-primary`/`gf-db-replica`, DB name `pos`) that production uses:

```bash
# Postgres primary/replica, exposed on host ports 5433/5434
podman-compose -f ../postgrest/compose.yaml -f ../postgrest/compose.dev.yml up -d

# MinIO, exposed on host ports 9000/9001
podman-compose -f ../../minio/compose.yaml -f ../../minio/compose.dev.yml up -d
```

Both require the external network `galfields-net` to exist first: `podman network create galfields-net`.

Point `application-dev.properties` at `jdbc:postgresql://localhost:5433/pos` and MinIO endpoint `http://localhost:9000` (credentials default to `postgres`/`bob123` and `minioadmin`/`minioadmin` — see each service's compose file).
