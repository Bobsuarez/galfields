# SPEC 03 — Unidades de venta por producto con factores de conversión

> **Status:** Implemented
> **Depends on:** Ninguno
> **Date:** 2026-07-29
> **Objective:** Agregar unidades de venta por producto con factores de conversión configurables (ej. media cajetilla / cajetilla completa) para que la cigarrería opere múltiples presentaciones del mismo producto sobre un stock base común, con el modelo de datos preparado para futuras extensiones (peso, lotes, variantes) sin implementarlas todavía.

## Scope

**In:**

- **Backend** (`backend/pos`):
  - Nueva tabla `product_units`: `product_id` (FK), `unit_name`, `conversion_factor` (a la unidad base), `unit_price`, `barcode` (nullable, único), `is_base` (marca cuál es la unidad base, factor siempre 1), `is_active`.
  - Migración: cada producto existente recibe automáticamente una fila base (`unit_name = 'Unidad'`, `conversion_factor = 1`, `unit_price` = su precio actual, `barcode` = su barcode actual) — nada deja de venderse el día del deploy.
  - `GET /api/products` incluye las unidades de cada producto (array anidado).
  - CRUD de unidades por producto (crear/editar/desactivar una unidad, con su nombre/factor/precio/barcode).
  - `POST /api/sales`: cada línea de venta referencia qué unidad se vendió; el stock se descuenta convertido a la unidad base (`cantidad × conversion_factor`); se snapshotea nombre/factor/precio de la unidad al momento de la venta (igual que hoy se snapshotea el precio del producto).

- **Mobile** (`apps/galfields-mobile`): pantalla nueva "Unidades de venta" (separada del form de producto) para gestionar las unidades de un producto — listar, crear, editar, desactivar, definir factor/precio/barcode.

- **Desktop** (`apps/galfield-pos`):
  - Migración SQLite local: una fila local por unidad de venta (mismo patrón que ya usan las variantes de ropa hoy — no concepto nuevo en el schema local).
  - `sync.rs` actualizado para traer cada unidad como su propia fila local.
  - POS: tocar un producto con más de 1 unidad abre un selector chico antes de agregarlo al carrito; con 1 sola unidad se agrega directo, sin selector. Escanear un barcode que pertenece a una unidad específica la agrega directo, sin selector.
  - `invoices.rs::create_sale`: valida/descuenta stock convertido a unidad base; el ticket impreso muestra el nombre de la unidad vendida.

- Reportes de bajo stock y ajustes de inventario (`POST /api/inventory/adjustments`) siguen operando en unidad base, sin cambios de umbral ni de contrato.

**Out (queda para otro spec si hace falta):**

- Venta por peso (Fruver), lotes/vencimientos (Farmacia), variantes de talla/color (Ropa) — el modelo queda preparado para no requerir reescritura, pero nada de esto se construye acá.
- Ajustes de inventario en una unidad distinta a la base (ej. "sumá 2 cartones") — el ajuste sigue siendo siempre en unidad base.
- Reportes nuevos de "cuánto se vendió por unidad" (ej. cartones vs sueltas) — el reporte de ventas sigue agregando en base, sin desglose por unidad.
- Cualquier cambio al concepto de variante existente (talla/color) — es una dimensión distinta y ortogonal a unidades de venta, no se toca.

## Data model

**Backend (`backend/pos`, PostgreSQL) — nueva migración:**

```sql
CREATE TABLE product_units (
    product_unit_id   BIGSERIAL PRIMARY KEY,
    product_id        BIGINT NOT NULL REFERENCES products(product_id),
    unit_name         VARCHAR(50) NOT NULL,
    conversion_factor INTEGER NOT NULL CHECK (conversion_factor >= 1),
    unit_price        NUMERIC(12,2) NOT NULL,
    barcode           VARCHAR(64) UNIQUE,
    is_base           BOOLEAN NOT NULL DEFAULT FALSE,
    is_active         BOOLEAN NOT NULL DEFAULT TRUE,
    created_at        TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
-- Exactamente una unidad base por producto (factor = 1, la unidad en la
-- que products.stock_quantity ya se cuenta hoy).
CREATE UNIQUE INDEX uq_product_units_base ON product_units(product_id) WHERE is_base;

-- Backfill: cada producto existente recibe su unidad base automática.
INSERT INTO product_units (product_id, unit_name, conversion_factor, unit_price, barcode, is_base)
SELECT product_id, 'Unidad', 1, price, barcode, TRUE FROM products;
```

`sale_items` gana el snapshot de qué unidad se vendió (mismo criterio que ya usa el resto de la venta — precio/prefijo de factura quedan congelados al momento de vender):
```sql
ALTER TABLE sale_items ADD COLUMN product_unit_id   BIGINT REFERENCES product_units(product_unit_id);
ALTER TABLE sale_items ADD COLUMN unit_name         VARCHAR(50) NOT NULL DEFAULT 'Unidad';
ALTER TABLE sale_items ADD COLUMN conversion_factor INTEGER NOT NULL DEFAULT 1;
```
`sale_items.quantity` (ya existente) pasa a significar "cuántas de esta unidad" — el equivalente en unidad base (`quantity × conversion_factor`) se calcula al momento de descontar `products.stock_quantity`, no se persiste aparte.

**Desktop (`apps/galfield-pos`, SQLite local) — migración nueva sobre `products`:**
```sql
ALTER TABLE products ADD COLUMN remote_product_id      INTEGER;
ALTER TABLE products ADD COLUMN remote_product_unit_id INTEGER;
ALTER TABLE products ADD COLUMN unit_name              TEXT NOT NULL DEFAULT 'Unidad';
ALTER TABLE products ADD COLUMN conversion_factor      INTEGER NOT NULL DEFAULT 1;
```
- `remote_product_unit_id`: identifica esta fila local exacta (lo que se manda de vuelta en `POST /api/sales` como la unidad vendida).
- `remote_product_id`: agrupa las filas hermanas de un mismo producto (Media/Completa apuntan al mismo `remote_product_id`) — necesario porque comparten stock real.
- `products.stock_quantity` en cada fila sigue viniendo directo del sync (`sync.rs`), ya convertido por el backend a "disponible en esta unidad" (`floor(stock base ÷ conversion_factor)`) — sin cambios en cómo se puebla.
- Al vender localmente (antes del próximo sync), `invoices.rs::create_sale` descuenta la fila vendida **y sus hermanas** (mismo `remote_product_id`), convirtiendo a base y aplicando `floor()` por cada una — optimista, no perfectamente exacto entre syncs, corregido siempre por el próximo `sync_products`.

## Implementation plan

1. **Backend**: migración `product_units` + backfill de unidad base para productos existentes (ver Data model). Entidad `ProductUnit`, repositorio, y `GET /api/products` extendido para incluir el array de unidades de cada producto. Nada se rompe — comportamiento actual sigue igual, solo se agrega data nueva.
   Test manual: `GET /api/products` muestra cada producto con al menos su unidad base (`unit_name: "Unidad"`, `conversion_factor: 1`).

2. **Backend**: CRUD de unidades — `POST/PUT/DELETE /api/products/{productId}/units` (crear, editar, desactivar). Validación: no se puede desactivar/borrar la unidad marcada `is_base`.
   Test manual: crear "Media" y "Completa" para un producto de prueba vía Postman/curl, confirmar que aparecen en el siguiente `GET /api/products`.

3. **Backend**: `POST /api/sales` acepta `productUnitId` por línea (en vez de asumir siempre unidad base). Convierte a base (`quantity × conversion_factor`) para el descuento real de `products.stock_quantity`, y snapshotea `unit_name`/`conversion_factor` en `sale_items`.
   Test manual: reportar una venta de prueba con una línea en unidad "Completa", confirmar que `products.stock_quantity` bajó por el múltiplo correcto, no por 1.

4. **Mobile**: `services/product-units-api.ts` + pantalla nueva "Unidades de venta" (accesible desde el detalle de un producto) — listar/crear/editar/desactivar unidades de un producto, con sus campos nombre/factor/precio/barcode.
   Test manual: desde el celular, crear "Media" (factor 10) y "Completa" (factor 20) para un producto de cigarrillos real, confirmar que quedan guardadas.

5. **Desktop**: migración SQLite (nuevas columnas en `products`, ver Data model) + `sync.rs` actualizado — cada unidad de venta del cloud se sincroniza como su propia fila local, con `remote_product_id`/`remote_product_unit_id`/`unit_name`/`conversion_factor` poblados.
   Test manual: correr "Sincronizar catálogo", confirmar en Inventario que el producto de prueba aparece dos veces (Media y Completa), cada una con su propio precio.

6. **Desktop**: selector de unidad en el POS — tocar un producto con más de una unidad hermana abre un selector chico antes de agregarlo al carrito; con una sola unidad se agrega directo. Escanear un barcode que pertenece a una unidad específica la agrega sin selector.
   Test manual: tocar el producto de prueba en el POS, confirmar que aparece el selector Media/Completa; escanear (si configuraste barcode en "Completa") confirma que agrega directo esa unidad.

7. **Desktop**: `invoices.rs::create_sale` — valida/descuenta stock convertido a base, descuenta también las filas hermanas (mismo `remote_product_id`, con `floor()`), y el ticket impreso muestra el nombre de la unidad vendida.
   Test manual: vender 1 "Completa", confirmar en Inventario que tanto "Completa" como "Media" bajaron su stock mostrado proporcionalmente, y que el ticket dice "Completa" no "Unidad".

8. **Desktop**: `sales_sync.rs` — al reportar la venta a la nube, envía `productUnitId` (no solo `variantId`) por línea.
   Test manual: con el backend local corriendo, confirmar en logs (`logging::step`) que la venta se reporta con el `productUnitId` correcto y el stock en la nube baja igual que el local.

## Acceptance criteria

- [x] Backend: `GET /api/products` devuelve cada producto con su array de unidades (mínimo la unidad base auto-creada por la migración).
- [x] Backend: se puede crear/editar/desactivar una unidad de venta (ej. "Media", factor 10) para un producto vía la API, sin afectar la unidad base.
- [x] Backend: no se puede desactivar ni borrar la unidad marcada como base de un producto.
- [x] Backend: `POST /api/sales` con una línea en unidad "Completa" (factor 20) descuenta `products.stock_quantity` en 20, no en 1.
- [x] Backend: `sale_items` de una venta reportada conserva el nombre y factor de la unidad vendida, aunque esa unidad se edite/desactive después.
- [x] Mobile: desde "Unidades de venta" se puede crear, editar y desactivar unidades de un producto, con nombre/factor/precio/barcode propios.
- [x] Desktop: tras sincronizar catálogo, un producto con 2 unidades aparece como 2 filas locales independientes en Inventario, cada una con su propio precio.
- [x] Desktop: en el POS, tocar un producto con más de una unidad abre un selector; un producto con una sola unidad se agrega directo, sin selector.
- [x] Desktop: escanear el barcode específico de una unidad la agrega al carrito directamente, sin selector.
- [x] Desktop: vender una unidad descuenta el stock mostrado de todas sus unidades hermanas (mismo producto), de forma proporcional.
- [x] Desktop: el ticket impreso y el historial de venta muestran el nombre de la unidad vendida (ej. "Completa"), no un genérico "Unidad" cuando no corresponde.
- [x] Desktop: la venta reportada a la nube (`sales_sync.rs`) incluye el `productUnitId` correcto, y el stock en la nube queda coherente con el stock local tras el sync.
- [x] Productos existentes (sin unidades configuradas antes de este deploy) siguen vendiéndose exactamente igual que hoy, sin selector ni cambios visibles, hasta que alguien les agregue una segunda unidad.
- [x] Reportes de bajo stock y ajustes de inventario siguen operando en unidad base, sin cambios de umbral ni de contrato de API.

## Decisions

- **Yes:** tabla `product_units` nueva, en vez de reusar el concepto de variantes existente. **Por qué:** las unidades comparten el mismo stock físico (vender una fracción no es un SKU independiente), a diferencia de color/talla en Ropa, que sí tienen stock propio.
- **Yes:** migración crea automáticamente la unidad base para cada producto existente. **Por qué:** cero productos rotos el día del deploy — todo sigue vendiéndose igual hasta que alguien agregue una segunda unidad a mano.
- **No:** exigir que un admin configure unidades a mano antes de que un producto pueda venderse. **Por qué:** fricción innecesaria sobre el catálogo actual completo, sin ganancia real.
- **Yes:** pantalla separada en mobile para gestionar unidades, no dentro del form de producto. **Por qué:** mantiene el form de producto simple; decisión explícita del usuario.
- **Yes:** stock y ajustes de inventario siempre en unidad base. **Por qué:** coincide con lo que el backend ya trackea hoy, evita reinterpretar el umbral de bajo stock existente.
- **No:** permitir ajustes de inventario en unidades no-base (ej. "sumá 2 cartones") en esta spec. **Por qué:** fuera de alcance — se puede agregar después si hace falta, sin bloquear esto.
- **Yes:** barcode opcional por unidad, no obligatorio. **Por qué:** un cigarrillo suelto o media cajetilla no trae código de fábrica; el selector manual sigue como respaldo siempre disponible.
- **Yes:** selector de unidad al tocar el producto en el POS, no botones separados por unidad en la card. **Por qué:** cero fricción visual para la mayoría de productos (una sola unidad), sin recargar la card.
- **Yes:** en desktop, una fila local por unidad — mismo patrón que ya usan las variantes de ropa — en vez de una tabla `product_units` nueva en SQLite. **Por qué:** reusa `sync.rs`/POS ya probados, menos superficie nueva en el schema local.
- **Yes:** una venta local descuenta también las filas hermanas del mismo producto (`remote_product_id`), no solo la vendida. **Por qué:** evita mostrarle al cajero un stock desactualizado en la unidad hermana hasta el próximo sync; el pequeño desfase de redondeo posible se autocorrige en el siguiente `sync_products`.
- **No:** meter Fruver (peso), Farmacia (lotes/vencimiento) o Ropa (variantes) en esta misma spec. **Por qué:** cada uno trae su propio dominio de decisiones — mezclarlos multiplica riesgo y tiempo antes de tener algo probado en producción con la cigarrería real, que es la necesidad inmediata.

## Risks

| Riesgo | Mitigación |
|---|---|
| Desfase de redondeo en el descuento local de filas hermanas (`floor()` por cada unidad) si se venden varias unidades del mismo producto sin sincronizar entre medio | Tolerado a propósito — el próximo `sync_products` siempre pisa con el número real de la nube; mismo criterio de eventual consistency que ya usa hoy la sincronización de catálogo. |
| `sale_items` de ventas viejas no tienen `product_unit_id`/`unit_name`/`conversion_factor` reales | Columnas nuevas con default (`unit_name = 'Unidad'`, `conversion_factor = 1`, `product_unit_id NULL`) reproducen exactamente el comportamiento implícito de hoy — ninguna venta histórica cambia de significado. |
| Colisión de barcode entre unidades (ej. cargar el mismo código en dos unidades por error de tipeo) | `barcode` es `UNIQUE` a nivel de base de datos — el error se detiene al crear/editar, con mensaje claro, no como bug silencioso de escaneo ambiguo. |
| Un producto con variantes (Ropa, fuera de alcance) y unidades (cigarrillos) a la vez no está soportado — el patrón de "una fila local por unidad" asume que solo una de las dos dimensiones aplica por producto | Combinación no soportada, documentada como tal. Si algún día hace falta (ej. cigarrillos con sabor + unidades), es una spec aparte que decide cómo combinar ambas dimensiones. |
