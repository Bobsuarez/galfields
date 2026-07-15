# backend/pos

API en la nube de Galfields — Spring Boot 4 / Java 21. Es la fuente de verdad de catálogo, inventario, ventas y reportes; tanto [`apps/galfield-pos`](../../apps/galfield-pos) (POS de escritorio) como [`apps/galfields-mobile`](../../apps/galfields-mobile) (app móvil) hablan con esta API.

## Stack

- **Spring Boot 4.1.0** sobre **Java 21**, compilado con Gradle
- **PostgreSQL** (primary + réplica de lectura)
- **MinIO** para imágenes, servido públicamente detrás de un CDN
- **Flyway** para migraciones de schema
- **springdoc-openapi** — Swagger UI autogenerado

## Requisitos

- JDK 21
- Gradle — el wrapper `./gradlew` no está commiteado en este repo (solo `gradle/wrapper/gradle-wrapper.jar`/`.properties`); regenéralo con `gradle wrapper` o usa un `gradle` de sistema mientras tanto
- PostgreSQL y MinIO corriendo localmente (ver abajo)

## Comandos

```bash
./gradlew build          # compilar
./gradlew bootRun        # correr la app
./gradlew test           # correr todos los tests
./gradlew test --tests "co.com.galfields.pos.SomeTestClass"   # una clase de test
./gradlew clean          # limpiar el build
```

## Infraestructura local (Postgres + MinIO)

No hay `compose.yaml` en este repo — usa los compartidos, para que el entorno local coincida con la misma topología que producción:

```bash
# Postgres primary/réplica, expuesto en los puertos 5433/5434
podman-compose -f ../postgrest/compose.yaml -f ../postgrest/compose.dev.yml up -d

# MinIO, expuesto en los puertos 9000/9001
podman-compose -f ../../minio/compose.yaml -f ../../minio/compose.dev.yml up -d
```

Requiere la red externa `galfields-net` (`podman network create galfields-net` si no existe). Apunta `application-dev.properties` a `jdbc:postgresql://localhost:5433/pos` y a `http://localhost:9000` — credenciales por defecto y más detalle en [`CLAUDE.md`](CLAUDE.md).

**Nota:** una base local recién creada no trae `locations` ni `brands` — crear productos falla hasta insertar al menos una ubicación llamada `Bogotá - Chapinero` (nombre hardcodeado, ver `ProductService.DEFAULT_LOCATION_NAME`) y una marca.

## Documentación de la API

Con la app corriendo:

- Swagger UI: `http://localhost:8080/swagger-ui.html`
- OpenAPI JSON: `http://localhost:8080/v3/api-docs`

## Endpoints principales

- `/api/products` — catálogo de productos y variantes (multipart, con imágenes)
- `/api/categories`, `/api/brands`, `/api/locations`, `/api/payment-methods` — CRUDs de catálogo
- `/api/inventory/adjustments` — ajustes de stock idempotentes
- `/api/sales` — registro de una venta completa (ítems, pagos, totales) — aplica el ajuste de inventario correspondiente en la misma llamada
- `/api/reports/*` — reportes agregados (ventas, métodos de pago, facturas, inventario, stock bajo) que consume la app móvil

## Nota sobre el schema

`doc/data_base.sql` es el dump de verdad del schema que corre en el cluster — es la fuente de verdad por encima de las migraciones de Flyway cuando difieren entre sí. Cualquier cambio de schema debe reflejarse ahí **y** en una migración nueva versionada. Ver [`CLAUDE.md`](CLAUDE.md) para el detalle completo (gotchas de columnas `BIGSERIAL`, la convención de imágenes vía `attach_files`, tipos `ENUM` de Postgres, etc.).

## Más detalle

Este README es solo el punto de entrada — la arquitectura completa, las convenciones de código, y el porqué de cada decisión de diseño están en [`CLAUDE.md`](CLAUDE.md).
