# galfields-mobile

App móvil de gestión — Expo 54 / React Native, con `expo-router` (file-based routing) y New Architecture habilitada. Administra catálogo, inventario y reportes directamente contra [`backend/pos`](../../backend/pos); a diferencia del POS de escritorio, no trabaja offline — cada pantalla lee/escribe en vivo contra la API.

## Stack

- **Expo 54** / **React Native 0.81**, New Architecture + React Compiler habilitados
- **expo-router 6** — rutas basadas en archivos bajo `app/`
- **TypeScript**

## Requisitos

- Node.js
- La app **Expo Go** en tu teléfono, o un emulador/simulador (Android Studio / Xcode)

## Configuración

```bash
npm install
cp .env.example .env.local
```

Edita `.env.local` y define `EXPO_PUBLIC_API_BASE_URL` con la URL de tu `backend/pos`. En un dispositivo físico con Expo Go, `localhost` no resuelve a tu máquina de desarrollo — usa la IP LAN (ej. `http://192.168.1.50:8080`). Esta variable es solo el valor inicial: la URL real es configurable en runtime desde **Configuración → Servidor** dentro de la app, sin necesidad de una build nueva.

También necesitas `EXPO_PUBLIC_CLIPDROP_API_KEY` (key gratuita en [clipdrop.co/apis](https://clipdrop.co/apis)) para el recorte de fondo de imágenes de producto.

## Comandos

```bash
npm start         # servidor de desarrollo de Expo (QR para Expo Go)
npm run ios       # simulador de iOS
npm run android   # emulador de Android
npm run web       # navegador web
npm run lint      # ESLint
```

## Funcionalidades

- **Login**: gate de autenticación (`contexts/auth-context.tsx`) delante del resto de la app
- **Productos**: catálogo con variantes, atributos e imágenes con fondo recortado automáticamente
- **Configuración**: CRUDs de categorías, marcas, ubicaciones y métodos de pago; pantalla de Servidor para apuntar a un backend distinto
- **Reportes**: ventas del día, cierre de caja, ventas por método de pago, inventario actual, stock bajo, historial de facturas — todo sobre `GET /api/reports/*`
- **Inventario**: vista de lectura/escritura por variante — editar stock y activar/desactivar productos

## Más detalle

Este README es solo el punto de entrada — la estructura de rutas, el sistema de theming, y las convenciones de cada feature están en [`CLAUDE.md`](CLAUDE.md).
