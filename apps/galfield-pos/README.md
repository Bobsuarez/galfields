# galfield-pos

Terminal de punto de venta de escritorio — Tauri 2 + Vue 3, con una base SQLite local. Trabaja offline-first: vende sin internet, sincroniza el catálogo bajo demanda contra [`backend/pos`](../../backend/pos) y reporta cada venta de vuelta a la nube en segundo plano apenas hay conexión.

## Stack

- **Frontend**: Vue 3 (`<script setup>`) + TypeScript + Vite
- **Backend nativo**: Rust + Tauri 2
- **Base de datos local**: SQLite embebida (bundlada con la app, migraciones propias en `src-tauri/migrations/`)

## Requisitos

- Node.js
- Toolchain de Rust (`rustup`) + [prerrequisitos de Tauri 2](https://tauri.app/start/prerequisites/) para tu sistema operativo
- En Linux, además: `libwebkit2gtk-4.1-dev`, `libappindicator3-dev`, `librsvg2-dev`, `libgtk-3-dev`, `libudev-dev`, `patchelf`, `pkg-config` (mismo set que usa el CI, ver `.github/workflows/build-galfield-pos-desktop.yml`)

## Comandos

```bash
npm install          # instalar dependencias
npm run tauri dev    # correr la app de escritorio en modo desarrollo
npm run tauri build  # generar el instalador (.msi/.dmg/.deb/.AppImage/etc.)

npm run dev           # solo el frontend Vue en el navegador (sin Tauri) — útil para iterar UI rápido
npm run build         # type-check (vue-tsc) + build del frontend

cd src-tauri && cargo test    # tests del backend Rust
cd src-tauri && cargo check   # chequeo rápido de compilación
```

## Primer arranque

La base de datos local empieza vacía. Antes de poder vender hay que sincronizar el catálogo al menos una vez desde **Sincronización** en la app (trae productos, categorías, marcas, ubicaciones y métodos de pago desde `backend/pos`).

## Funcionalidades

- **Punto de venta**: catálogo con búsqueda, carrito, cobro con múltiples métodos de pago, impresión/PDF de factura
- **Inventario** (solo lectura): consulta de stock actual sincronizado
- **Sincronización**: trae catálogo desde la nube; reporta ventas pendientes cuando hay conexión (idempotente por `clientEventId`, tolera reintentos e interrupciones)
- **Reportes**: reportes locales sobre lo vendido en esta terminal
- **Configuración**: impresora, apariencia (tema y colores personalizables), y la URL del backend en la nube — configurable en runtime, sin necesidad de una build nueva por instalación

## Más detalle

Este README es solo el punto de entrada — la arquitectura completa (estructura de `src-tauri/`, el sistema de logging numerado de las llamadas HTTP, las convenciones de sincronización e idempotencia, el sistema de theming) está en [`CLAUDE.md`](CLAUDE.md).
