# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

@AGENTS.md

## Commands

```bash
npm start           # Start Expo dev server (opens QR code for Expo Go)
npm run ios         # Start with iOS simulator
npm run android     # Start with Android emulator
npm run web         # Start with web browser
npm run lint        # Run ESLint (eslint-config-expo/flat)
```

There is no test runner configured yet.

## Architecture

This is an **Expo 54 / React Native 0.81** app using **file-based routing via expo-router 6**. The New Architecture (`newArchEnabled: true`) and React Compiler (`experiments.reactCompiler: true`) are both enabled.

### Routing

Routes live under `app/`. expo-router turns filenames into URL segments automatically:

- `app/_layout.tsx` — root Stack navigator; wraps everything in `ThemeProvider` (light/dark)
- `app/(tabs)/_layout.tsx` — bottom tab navigator (Home, Explore)
- `app/(tabs)/index.tsx` — Home tab
- `app/(tabs)/explore.tsx` — Explore tab
- `app/modal.tsx` — modal screen (opened via `router.push('/modal')`)

The `unstable_settings.anchor = '(tabs)'` in the root layout makes the tabs group the default deep-link anchor.

Beyond the tabs, top-level auth-gated route groups are registered in `app/_layout.tsx`'s Stack: `login`, `products`, `settings`, `reports`, `inventory`. Each follows the same shape — a `<Name>Layout` in `app/<name>/_layout.tsx` that redirects to `/login` when `!isAuthenticated` (see `contexts/auth-context.tsx`), a thin per-screen route file that just renders a component from `components/<name>/`, and a hub screen (`index.tsx`) listing sub-screens as cards (see `app/settings/index.tsx` or `app/reports/index.tsx`) — except `inventory`, which is a single screen, not a hub. `services/<name>-api.ts` holds the fetch calls for that feature (`apiBaseUrl()` + `parseApiErrorMessage`, see `services/api-base-url.ts`/`services/api-error.ts`) — copy an existing one (`services/reports-api.ts` is the most recent) rather than inventing a new API-client shape.

`app/reports/` is a read-only dashboard over `backend/pos`'s `GET /api/reports/*` endpoints (see that repo's CLAUDE.md for exactly what each one returns) — sales summary, cash summary, sales by payment method, current inventory, low stock, and invoice history/detail. It depends on the desktop POS (`apps/galfield-pos`) actually reporting sales to the cloud via `POST /api/sales` — before that existed, these screens have nothing real to show.

`app/inventory/` (`components/inventory/inventory-screen.tsx`) is the **read/write** counterpart to `reports/inventory.tsx`'s read-only view — one row per product **variant** (not per product, since each variant has its own stock/barcode; `InventoryScreen`'s local `flattenProduct` mirrors the same "Camiseta Basica - Rojo M" naming convention `apps/galfield-pos`'s `sync.rs` uses), backed by `GET /api/products?includeInactive=true` (`services/products-api.ts`'s `fetchProducts`, extended with an `includeInactive` param — the regular Products catalog screen never passes it, since a deactivated product shouldn't be offered for sale there). A "Mostrar desactivados" switch toggles that flag and triggers a fresh page-0 load. Two write actions per row:
- **Stock**: tapping the stock number opens `StockEditModal`, which asks for the *new total count* (not a +/- delta) — the modal computes `delta = newStock - currentStock` and `services/inventory-api.ts`'s `adjustVariantStock` reports just that delta to `POST /api/inventory/adjustments` (see `backend/pos`'s CLAUDE.md), generating a fresh client-side UUID (`utils/uuid.ts`'s `generateUuid` — a plain `Math.random()`-based v4, not `expo-crypto`, to avoid a new dependency) as that endpoint's required `clientEventId` per call.
- **Activate/deactivate**: the row's toggle icon calls `services/products-api.ts`'s `deactivateProduct`/`activateProduct` (`DELETE`/`PUT .../activate` on `/api/products/{id}` — see `backend/pos`'s CLAUDE.md's "Activate/deactivate" section), confirmed via `Alert.alert` first. Both act on the whole **product** (all its variants together), matching the backend's cascade — there's no independent per-variant toggle.

### Theming

- `constants/theme.ts` — `Colors` (light/dark palettes) and `Fonts` (platform-specific font stacks)
- `hooks/use-color-scheme.ts` — re-exports `useColorScheme` from `react-native`
- `hooks/use-color-scheme.web.ts` — platform override for web
- `hooks/use-theme-color.ts` — resolves a named color key from `Colors` for the current scheme; accepts per-prop overrides

Themed components (`ThemedText`, `ThemedView`) use `useThemeColor` internally.

### Platform-specific files

Use React Native's platform extension convention for platform splits:
- `.ios.tsx` — iOS-only implementation (e.g., `icon-symbol.ios.tsx` uses native SF Symbols)
- `.web.ts` — web-only implementation (e.g., `use-color-scheme.web.ts`)
- No suffix — Android and web fallback

`IconSymbol` maps SF Symbol names (iOS) to Material Icons (Android/web). Add new icon mappings in the `MAPPING` object inside `components/ui/icon-symbol.tsx`.

### Path alias

`@/` resolves to the repo root (configured in `tsconfig.json`). Use `@/components/...`, `@/hooks/...`, `@/constants/...` for all internal imports.
