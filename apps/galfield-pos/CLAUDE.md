# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Stack

**Tauri 2 + Vue 3 + TypeScript + Vite** desktop POS app for Galfields.

- Frontend: `src/` — Vue 3 SFCs using `<script setup>` syntax
- Backend: `src-tauri/src/` — Rust (Tauri 2 commands)

## Commands

```bash
npm run tauri dev      # full dev mode: starts Vite (port 1420) + Tauri window
npm run tauri build    # production build + bundle installer
npm run dev            # frontend only (Vite), no Tauri window
npm run build          # frontend type-check (vue-tsc) + Vite build only
```

There are no tests yet. Cargo tests can be run from `src-tauri/`:
```bash
cd src-tauri && cargo test
```

## Architecture

The app is split into two processes that communicate via Tauri's IPC bridge:

- **Vite dev server** is locked to port 1420 (`strictPort: true`). Tauri's `beforeDevCommand` starts it automatically when you run `tauri dev`.
- **Rust → JS**: Tauri commands are defined in `src-tauri/src/lib.rs`, registered via `invoke_handler!`, and called from Vue using `invoke()` from `@tauri-apps/api/core`.
- **Capabilities / permissions** for Tauri plugins are declared in `src-tauri/capabilities/default.json`. Any new plugin capability must be added there.
- **Bundling** is configured in `src-tauri/tauri.conf.json`. The frontend dist is at `../dist` relative to `src-tauri/`.
- **Routing**: `src/router/index.ts` uses `createWebHashHistory` (required — Tauri serves from `dist/` with no server-side routing) with one lazy-loaded route per feature (`/pos`, `/inventario`, `/reportes`, `/sync`, `/configuracion`), each pointing at that feature's `<Name>View.vue`.
- **Shared state — module-level singletons, no Pinia**: `src/composables/useAppConfig.ts`, `useToast.ts`, and `usePaymentMethods.ts` each export a `reactive`/`ref` declared at module scope, so every component calling the composable shares the same instance without a store library. `useAppConfig` is the important one: it loads `config` once (`get_settings` → `src/utils/settingsMapper.ts`'s `applyRecord`, which maps the flat `key → value` rows from the `app_settings` table onto the nested `ConfigSettings` shape) and a `watchEffect` re-applies `config.styles` as CSS custom properties on `:root` any time it changes. That means the Garfield palette below (`## Mockups de módulos`) is the *default* seed data, not a hardcoded theme — colors are runtime-configurable from the Configuración module. `applyStyles` derives **13 CSS vars from only 4 of the 5 user-facing color fields** (`bgColor`/`lightBg`/`primaryColor`/`lightText`, via fixed alpha blends and lighten/darken shifts) — `secondaryText` only ever reaches `--color-accent`, which has exactly one consumer in the whole app (the scrollbar thumb in `main.css`), so changing it is barely visible anywhere. `--color-success`/`--color-danger`/`--color-warning` are fixed in `main.css` and never touched by `applyStyles` — status colors stay semantically consistent regardless of theme, on purpose, not configurable from Personalización de Estilos.
- **Configuración's `settings` is an isolated draft, not the live `config` singleton** (`useConfig.ts`, explicit comment: "isolated from the global singleton so the user can discard unsaved changes without affecting the running app") — editing colors doesn't touch the rest of the running app until `save()` calls `save_settings` then `reloadConfig()`. The **only** live feedback while editing is `ConfigFormStyles.vue`'s "Vista Previa del Sistema" widget, bound to `--pw-bg`/`--pw-primary`/`--pw-text` (all three sourced straight from `settings.styles`, not hardcoded) — if you add a new configurable color field, wire a matching `--pw-*` var here too, or the preview goes back to lying about that field the same way it used to for background/text before this was fixed.
- **Shared TypeScript types** live in `src/types/index.ts` (`Product`, `CartItem`, `PendingSale`, `ConfigSettings`, etc.) — check there before adding a duplicate interface in a feature folder.

## Backend modules (`src-tauri/src/`)

Each module owns one domain's `#[tauri::command]`s, all registered in `lib.rs`'s `invoke_handler!`:

- `db/mod.rs` — opens `<app_data_dir>/galfield.db` (SQLite via `rusqlite`, bundled) and runs migrations.
- `http_client.rs` — every outbound call to the cloud (`sync.rs`, `catalog_sync.rs`, `sales_sync.rs`) goes through this instead of each module building its own bare `reqwest::Client`. Holds the single shared `reqwest::Client` (connection pooling, built once via `OnceLock`). `get`/`post_json` log the method, URL, request body (POST only), response status, and response body on every call via `logging::step` — that's the "interceptor" for API visibility during dev (`npm run tauri dev`'s terminal), not user-facing UI. `parse_json(context, body)` is the centralized point for turning "the cloud answered but the JSON shape didn't match" into an error: it tags the message with `context` (`"module::function"`) so the string that reaches the frontend names the exact call site, instead of three copy-pasted `.map_err(|e| format!("Respuesta inesperada..."))` sites each saying the same generic thing. Add any new cloud call/JSON parse through this module, not a fresh `reqwest::Client::new()` or bare `serde_json::from_str`, or it silently loses both the logging and the context tag.
- **Cloud API base URL is a runtime setting, not a hardcoded constant** — `http_client::api_base_url(&db)` reads `sync.api_base_url` from `app_settings` (migration `007_api_base_url_setting`, seeded to `https://galfields.kinforgeworks.com`, same as the old constant's value), editable from Configuración → Reglas y Sincronización (`ConfigFormDefaults.vue`, `settings.sync.apiBaseUrl` via `settingsMapper.ts`). `DEFAULT_API_BASE_URL` still exists as a fallback for the rare case the setting row is missing (a local DB older than migration 007 that hasn't run yet), but every real call goes through `api_base_url(&db)`. **Every caller (`sync.rs`, `catalog_sync.rs`, `sales_sync.rs`) reads it fresh, in a scope that drops the `MutexGuard` before the next `.await`** — same "lock, read, drop, then await" pattern already used for other settings in this codebase; never hold `state.db.lock()` across an awaited HTTP call. This is what lets one installation point at a different backend (staging, a separate Galfields deployment) without a rebuild.
- `logging.rs` — `step(location, message)` prints a sequential, numbered trace line (`[0001] module::function - message`) shared by one global counter across the whole backend, so the terminal's print order is also the call order regardless of which module logged it. Used at the meaningful checkpoints of a flow (entering a command, a validation passing/failing, a DB write finishing) in `sync.rs`, `catalog_sync.rs`, `sales_sync.rs`, `invoices.rs::create_sale`, and by `http_client.rs` for the request/response lines themselves. When something breaks, the numbered lines in the terminal are the trace to read — no separate log viewer needed.
- `settings.rs` — generic key/value store (`app_settings` table) for app config: `get_settings`/`save_settings`.
- `pending_sales.rs` — full CRUD for held/parked sales (`save_pending_sale`/`get_pending_sales`/`delete_pending_sale`).
- `payment_methods.rs`, `reports.rs`, `products.rs` — **read-only** queries: `list_payment_methods` (only `is_active = 1` rows — see `catalog_sync.rs` below for how that column gets set); `reports.rs`'s `get_sales_summary`/`get_top_products`/`get_financial_summary` all aggregate `sales`/`sale_items` over a `[date_from, date_to]` range (period-over-period comparison is composed by calling a command twice from the frontend, not done in Rust); `products.rs`'s `get_low_stock_products` filters `products` by a fixed stock threshold, and `get_products` returns every local product (ordered by name) — this is what both POS and Inventory list from (see below). There is still no product create/update/delete command — the local `products` table is populated only by `sync.rs`'s `sync_products`, never written to directly by the frontend. Don't confuse `products.rs` with `src/features/products/`, an empty, unused scaffold folder from initial setup.
- `sync.rs` — `sync_products`: pulls the full cloud catalog (`GET {cloud}/api/products`, paginated) and upserts it into the local `products` table by barcode. Part of the "Sincronizar catálogo" chain on the Sincronización screen (`src/features/sync/SyncView.vue`, orchestrated by `useCatalogSync.ts`) — manual and user-triggered only, never runs automatically on app start. The cloud schema is product+variants (one product can have several sellable barcodes, each with its own price/stock); the local `products` table is intentionally flat (see `001_initial.sql`), so **each cloud variant becomes one local row**, not each product — multi-variant products get their variant's attribute values appended to the name (e.g. "Camiseta Basica - Rojo M") since the local schema has no separate variant concept to disambiguate them otherwise. Each row also stores the cloud `variantId` in `remote_variant_id` (migration `003_product_remote_link`), which is what `sales_sync.rs` (below) addresses the cloud with when reporting a sale. After upserting everything the current run saw, any local row whose `last_sync_at` is still older than when this run started (i.e. it wasn't in the feed at all — deleted or unpublished in the cloud) gets `is_active = 0` — never deleted, so sale history (`sale_items` FK) stays intact; same visual treatment as an explicit cloud-side deactivation. **Gotcha:** `RemotePage` mirrors Spring's `PagedModel` shape (`@EnableSpringDataWebSupport(pageSerializationMode = VIA_DTO)` in `backend/pos`'s `PosApplication.java`) — pagination metadata is nested under `page: { number, totalPages, ... }`, not a flat `last` field. If `backend/pos` ever changes its pagination serialization mode again, this struct (and `is_last`'s `page.number + 1 >= page.total_pages` computation) has to change with it, or `sync_products` fails with a `parse_json` "missing field" error.
- `catalog_sync.rs` — `sync_categories`/`sync_payment_methods`: the other two steps of the same "Sincronizar catálogo" chain, run right after `sync_products` (see `useCatalogSync.ts`). **The two commands use deliberately different reconciliation strategies**, because only one of the two local tables has something pointing at it:
  - `sync_categories` pulls `GET /api/categories` and does a **full replace** (`DELETE FROM categories` then re-insert everything returned) — safe because nothing local has a foreign key into `categories.id`; `products.category` is a plain denormalized text column, not a real reference.
  - `sync_payment_methods` pulls `GET /api/payment-methods` and **upserts by name** (`payment_method.name` is `UNIQUE`), then flips `is_active = 0` (migration `005_payment_method_active`) for any local row the cloud stopped returning — it deliberately never deletes, because `sales.payment_method` **is** a real foreign key (`PRAGMA foreign_keys = ON`), and deleting a row a past sale points to would fail outright. Same "never delete, just deactivate" fix as `sync_products`'s stale sweep above. Also captures the cloud's `paymentMethodId` into `remote_payment_method_id` (migration `006_payment_method_remote_link`) — same purpose as `products.remote_variant_id`: `sales_sync.rs` needs it to report which cloud payment method a sale used. **Bug fixed:** the `ON CONFLICT(name) DO UPDATE SET` clause originally listed only `is_active`/`remote_payment_method_id`, omitting `url` — so `payment_method.url` was set correctly on first insert (e.g. from the `002_payment_methods.sql` seed, which never sets one) but then frozen forever on every later sync, since every payment method after the first run always takes the `ON CONFLICT` branch. Any new column added to this upsert must be added to **both** the `INSERT` column list and the `DO UPDATE SET` clause, or it'll silently stop updating exactly like this did.
- `invoices.rs` — `create_sale`: besides inserting `sales`/`sale_items`, decrements `products.stock_quantity` per line **in the same transaction**, and — if `defaults.validate_stock` (Configuración → Por Defecto) is on — rejects the whole sale up front (before writing anything) if any line asks for more than what's on hand, with a message naming the product. Also generates `sales.sync_uuid` (a fresh `Uuid::new_v4()` per sale) — the idempotency key `sales_sync.rs` reports it under. Also owns the printer/cash-drawer `trigger_*` commands (see Peripheral event model below) and `get_port_lock`.
- `sales_sync.rs` — `push_pending_sales`: the other direction of sync, POS → cloud. Reports every sale with `synced_at IS NULL` to the cloud's **`POST /api/sales`** (see `backend/pos`'s CLAUDE.md, "Sale recording endpoint") — the whole sale (items with quantity/unit price/subtotal, the one payment method used, discount and total), not just a stock delta; the backend applies the matching stock adjustment atomically on its side. One HTTP call per sale, oldest first, **stopping at the first network/server failure** instead of skipping ahead — keeps sales in order and avoids hammering the network when a terminal is genuinely offline. A sale is **skipped (left pending, loop continues to the next one)** — not treated as a failure — if any of its products lacks a `remote_variant_id` or its payment method lacks a `remote_payment_method_id` (both only populated by their respective catalog syncs): sending a partial sale would misreport the total, so it waits for the next catalog sync to resolve instead of blocking every later sale behind it. A sale with **zero line items** is marked synced immediately without a network call (nothing the cloud could ever do with it). Triggered two ways, both fire-and-forget from checkout's perspective: (1) right after `create_sale` succeeds (`useCheckout.ts`), and (2) `spawn_background_retry_loop` (started once from `lib.rs`'s `.setup()`) — pushes immediately on app start (catches up on anything left pending from a previous offline session), then sleeps for `sync.sales_retry_minutes` (re-read from `app_settings` every wake, so changing it in Configuración takes effect without restarting) and repeats forever. Every push attempt emits a `sales-sync-status` event (`{ pendingCount, pushedCount, error }`) — `useSalesSyncStatus.ts` subscribes and drives the "N sin sincronizar" badge in `AppStatusbar.vue` (fire-and-listen, same convention as the hardware peripherals below).
- `peripheral_manager.rs` / `peripherals.rs` — continuous peripheral listeners (barcode scanner) and device discovery (`list_serial_ports`, `list_video_devices`). **Cross-platform gotcha:** the barcode reader thread needs a blocking read with a timeout (see the "Peripheral event model" section below for why) — `open_port`/`wait_for_data` at the top of `peripheral_manager.rs` split this per-platform: Unix opens the port as a plain `std::fs::File` and uses a raw-fd `libc::poll()` (proven against real hardware, unchanged); Windows has no equivalent for a bare file handle, so it opens the port through the `serialport` crate instead (already a dependency — see `peripherals.rs`) with `.timeout(...)` configured, and `wait_for_data` becomes a no-op since `read_until` itself now blocks up to that timeout and returns `ErrorKind::TimedOut`, handled as "no data yet" in the reader loop. `WINDOWS_BAUD_RATE` defaults to `9600` (typical for POS barcode scanners in serial-emulation mode) and is **unverified against real Windows hardware** — this only exists to make `.github/workflows/build-galfield-pos-desktop.yml`'s Windows build compile at all (it previously failed outright: `std::os::unix`/`libc::poll` referenced unconditionally, `E0433`/`E0422`/`E0425`). If a deployed Windows scanner needs a different baud rate, or misbehaves, this is the code to revisit first.

### Product data flow (cloud → local → POS/Inventory)

The local `products` table is a read-only mirror of whatever was last pulled from the cloud, never edited from this app: the cloud catalog (managed from the mobile app's Configuración/Productos screens) is the single source of truth. `src/composables/useProducts.ts` is the shared low-level fetch (`invoke('get_products')`) both features build on:

- **POS** (`src/features/pos/composables/useProductCatalog.ts`) shows every product matching the active category/search — including deactivated or out-of-stock ones — and derives its category tabs from whatever distinct, non-empty `category` strings are present (no fixed category list anymore, exactly whatever text values ended up on synced products). `ProductCard.vue` computes `unavailableReason` (`'Desactivado'` beats `'Sin stock'` when both apply) to render those dimmed with a centered stamp instead of hiding them, and disables adding them to the cart. `useActiveSale.ts`'s `addProduct`/`increaseQty` enforce the same precedence server-adjacent: deactivation always blocks regardless of config, `defaults.validate_stock` additionally gates the stock check (so the cashier finds out immediately, not just at checkout where `create_sale` enforces both again).
- **Inventory** (`src/features/inventory/`) is a **read-only** list/search/paginate view over every local product (active or not) — no create/edit/delete UI, since editing a row locally would just get silently overwritten by the next sync. `ProductDetail.vue` shows real fields only (name, category, barcode, stock, price, last sync time); it intentionally does **not** have purchase price, IVA, supplier, or unit-of-sale fields — those don't exist in the local schema or the cloud API, and adding fake inputs for them would just be different-flavored mock data. Deactivated products (`is_active = 0`, whether from an explicit cloud-side deactivation or the "not in the last sync feed" sweep above) render dimmed (`.table-row--inactive` / `.detail-panel--inactive`) with a "Desactivado" badge in both `InventoryTable.vue` and `ProductDetail.vue`, instead of being hidden.
- `src/utils/stock.ts` exports `LOW_STOCK_THRESHOLD` (currently `5`) and `deriveStockStatus` — shared between Inventory's status badges and the sidebar banner's low-stock cycle (`useSidebarBanner.ts`), since `products` still has no `min_stock` column (see below).
- **Product image `<img>` failures are logged, not silent**: both `ProductCard.vue` and `ProductDetail.vue` bind `@error="handleImageError"` on the product photo, which `console.error`s the exact failed URL before falling back to the category emoji. Plain `<img :src>` load failures don't go through `http_client.rs` (that module only covers Rust-side cloud calls, not the webview's own image loading) — if a product photo won't load, open the webview's DevTools (right-click → Inspeccionar) and check Console/Network for that request's real error.
- **Resolved gotcha — CDN hotlink protection was returning 403 for the desktop app's own origin**: `cdn-galfields.kinforgeworks.com` (behind Cloudflare) used to 403 image requests whose `Referer` wasn't the main `galfields.kinforgeworks.com` domain — confirmed with `curl -e "http://localhost:1420/" <url>` → 403 vs `curl` with no referer → 200 (User-Agent/Sec-Fetch-* headers didn't matter, only `Referer` did). Since the desktop app's origin is `http://localhost:1420` in `npm run tauri dev` and `tauri://localhost` in a production build, every image request got blocked from this app specifically (mobile wasn't affected — React Native's image loader doesn't send a `Referer`). Two layers of mitigation are in place client-side: `index.html`'s `<meta name="referrer" content="no-referrer">` (the one that actually suppresses the header — the per-`<img>` `referrerpolicy` attribute alone didn't work, most likely because WebKitGTK 2.52, Tauri's Linux webview, doesn't reliably honor it) plus the `<img>`-level `referrerpolicy` attributes left in place too (harmless, defense-in-depth). **The CDN-side hotlink protection has since been disabled/adjusted directly on Cloudflare too** — confirmed working end-to-end, images load correctly from the desktop app now. Don't remove the `index.html` meta tag on the assumption the CDN fix alone covers it; both layers are cheap to keep and either one failing shouldn't reintroduce this bug.

Some report/inventory metrics are intentionally out of scope until the schema grows: `products` has no `min_stock` or cost/purchase-price column, and `sales` has no `seller` column — so per-product reorder thresholds, margin/COGS, and top-cashier reporting all need a new migration (see below) before they can be real instead of mocked.

### Sales push-sync (POS → cloud)

Migration `004_sales_sync.sql` adds `sales.sync_uuid`/`sales.synced_at` (the outbox) and seeds `sync.sales_retry_minutes` (default `5`) into `app_settings`. Pre-existing sales at migration time are backfilled with a synthetic `sync_uuid` and marked already-synced — retroactively pushing old sales could double-count stock the business already reconciled some other way before this feature existed; only sales created after the migration ever enter the push queue. See `sales_sync.rs` above for the full flow, and `backend/pos`'s CLAUDE.md for the cloud endpoint's contract (`POST /api/sales`: idempotency, employee-attribution placeholder, default-location scoping, negative-stock handling) and its `GET /api/reports/*` endpoints (backing the mobile app's report screens — see `apps/galfields-mobile`'s CLAUDE.md).

### Database migrations

- Plain `.sql` files in `src-tauri/migrations/`, embedded at compile time via `include_str!` and applied in `db::run_migrations`.
- Each migration is applied at most once, tracked by filename in the `schema_migrations` table — required because later migrations use non-idempotent DDL (`ALTER TABLE ... ADD COLUMN`), unlike `001_initial.sql` which is safe to re-run.
- Adding a migration: create `src-tauri/migrations/NNN_description.sql`, then wire it up with an `apply_migration("NNN_description", include_str!(...))` call in `run_migrations`.
- SQL identifiers stay in English (see language rule below) even though schema comments/docs may be in Spanish.

## Peripheral event model

Hardware peripherals (barcode scanner, printer, cash drawer, …) are never driven by a
plain `invoke()` call awaited for its result. Instead they follow a fire-and-listen
split: a Tauri command *dispatches* the action, and the outcome arrives later as a
Tauri event. This is what lets independent peripheral actions (e.g. printing a
receipt and opening the cash drawer) run without one blocking, delaying, or being
skipped because of the other's success or failure.

- **Event naming**: `peripheral-<device>-<event>`, e.g. `peripheral-barcode-found`,
  `peripheral-printer-status`, `peripheral-cash-drawer-error`.
- **Rust side** (`src-tauri/src/peripheral_manager.rs`, `src-tauri/src/invoices.rs`):
  a `#[tauri::command]` spawns a `std::thread` that does the actual I/O (read/write to
  a serial port or raw device file) and emits a `peripheral-<device>-status` or
  `peripheral-<device>-error` event via `AppHandle::emit` when it's done. The command
  itself returns almost immediately — its `Result` only reflects whether the job was
  *dispatched*, not whether the hardware action *succeeded*.
- **Two shapes, same convention**:
  - *Continuous listeners* (barcode scanner, future fingerprint reader): started with
    `start_peripheral_listener(deviceType, port)`, stopped with
    `stop_peripheral_listener(deviceType)`, tracked in `AppState.peripheral_stops` so a
    new start can cancel a previous one. Emits events repeatedly for as long as it runs.
  - *One-shot actions* (printer, cash drawer): dedicated `trigger_*` commands
    (`trigger_print_invoice`, `trigger_open_cash_drawer`) with no stop/state tracking —
    the thread does one job and exits. Emits exactly one `-status` or `-error` event.
- **Same-port serialization**: two one-shot actions can target the same physical port
  (the cash drawer is wired through the printer's RJ11, so both write to
  `printer_port`). Opening that device file from two threads at once fails or corrupts
  the output, so each `trigger_*` command grabs a per-port `Arc<Mutex<()>>` from
  `AppState.port_locks` (`invoices::get_port_lock`) *before* spawning its thread, and the
  thread holds that lock only around the `write_to_port` call. A second trigger for the
  same port simply blocks until the first one releases it, then proceeds — this keeps
  "independent of the other's success" true (the drawer still opens whether or not
  printing succeeded) while making sure they never write to the port at the same instant.
  This is transparent to the frontend: no polling, delay, or sequencing needed there.
- **Vue side** (`src/composables/peripherals/`): **one composable per device**, each the
  only place that knows its own event names and payload shapes (SRP — a change to
  printer events touches `usePrinterBus.ts` only, never barcode or cash-drawer code):
  - `useListenerDevice.ts` — generic, device-agnostic `startDevice(deviceType, port)` /
    `stopDevice(deviceType)` wrapper around the continuous-listener commands, plus the
    shared `DeviceStatusPayload` type. Knows nothing about any specific device.
  - `useBarcodeBus.ts` — barcode-only events (`onBarcodeFound`, `onBarcodeNotFound`,
    `onBarcodeStatus`, `onBarcodeError`); composes `useListenerDevice` for its
    start/stop lifecycle.
  - `usePrinterBus.ts` — printer-only: `triggerPrint`, `onPrinterStatus`, `onPrinterError`.
  - `useCashDrawerBus.ts` — cash-drawer-only: `triggerCashDrawer`, `onCashDrawerStatus`,
    `onCashDrawerError`.
  Each exposes `on<Device><Event>()` subscribers (returning `UnlistenFn`, matching
  `@tauri-apps/api/event`'s `listen()`) and, for one-shot devices, a `trigger*` dispatch
  function. Feature composables (e.g. `useBarcodeScanner.ts` uses `useBarcodeBus`;
  `useCheckout.ts` uses `usePrinterBus` + `useCashDrawerBus`) subscribe to the events
  they care about in `onMounted`, clean up in `onUnmounted`, and treat
  `trigger*`/`startDevice` calls as fire-and-forget — business logic branches on the
  event payload, never on whether the dispatching `invoke()` resolved.

**Extending to a new peripheral:**
1. Continuous listener → add a `spawn_<device>_listener` in `peripheral_manager.rs` and
   handle its `device_type` string in `start_peripheral_listener`. One-shot action → add
   a new `trigger_<action>` command (see `trigger_open_cash_drawer` for the minimal shape).
2. Emit `peripheral-<device>-status` / `peripheral-<device>-error` (reuse
   `peripheral_manager::DeviceStatusPayload` when a plain `{ connected, port }` shape fits).
3. Add a new `use<Device>Bus.ts` file under `src/composables/peripherals/` with that
   device's `on<Device>*` / `trigger*` helpers — don't add another device's events to an
   existing device's file, and don't call `listen()` directly outside this folder.
4. Subscribe from the feature composable that needs it.

## Modal convention

There's no shared `<Modal>` component — every modal (`SaveSaleModal.vue`, `PaymentMethodModal.vue`,
`ReportsPasswordModal.vue`, `InvoiceModal.vue`, …) is a standalone SFC that copies the same
`.modal-backdrop`/`.modal-card`/`.modal-header`/`.modal-body`/`.modal-footer` structure and
`Transition name="modal"` + `modal-in`/`modal-out` keyframe CSS. When adding a new modal, copy an
existing one's markup/CSS (`SaveSaleModal.vue` is the simplest reference) rather than inventing new
chrome. Props are always `visible: boolean`; emits are always `confirm`/`cancel`.

## VS Code

Recommended extensions are in `.vscode/extensions.json`: Vue - Official (Volar), Tauri, and rust-analyzer.

# Principios que debes aplicar obligatoriamente
- SOLID: cada componente tiene una sola responsabilidad
- DRY: cero lógica duplicada, extraer composables para lógica reutilizable
- Escalabilidad: estructura de carpetas por feature, no por tipo de archivo
- TypeScript estricto: interfaces para todos los modelos, sin `any`
- Separación clara: UI → Composable → Tauri Command (nunca lógica de negocio en el template)

# Idioma del código — regla obligatoria
Todo el código fuente **debe estar en inglés**: variables, funciones, structs, interfaces, enums, tipos, nombres de archivos, tablas SQL, columnas SQL, comentarios en código, y mensajes de log.
- Permitido en español: texto visible al usuario (labels, placeholders, mensajes de UI), contenido de strings literales en seeds SQL.
- Incorrecto: `let nombreProducto`, `fn guardar_config()`, `CREATE TABLE ventas`
- Correcto: `let productName`, `fn save_config()`, `CREATE TABLE sales`

# Restricciones
- Sin Pinia por ahora — los composables manejan el estado local del módulo
- Sin llamadas HTTP — toda comunicación con datos via invoke() de Tauri
- Sin lógica en templates — todo en composables o métodos del setup
- Los componentes shared deben ser genéricos, sin acoplamiento al módulo POS

## Mockups de módulos

### Tema visual: Garfield — colores cálidos

Paleta:
  - #0D0D0D fondo oscuro
  - #F28D35 naranja primario
  - #8C501B café acento
  - #F2E399 crema claro
  - #F2C7AE durazno suave

### Layout: 
sidebar izquierdo fijo, contenido a la derecha

### Módulo POS
![POS principal](./docs/mockup/pos-principal.png)

### Módulo Inventario  
![Inventario](./docs/mockup/inventario.png)

### Módulo sincronización
![Inventario](./docs/mockup/sincronizador.png)

### Módulo reportes
![Inventario](./docs/mockup/reportes.png)

### Módulo configuración
![Configuración](./docs/mockup/configuracion.png)