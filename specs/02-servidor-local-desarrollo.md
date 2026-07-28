# SPEC 02 — Atajo de servidor local para pruebas de desarrollo

> **Status:** Approved
> **Depends on:** Ninguno
> **Date:** 2026-07-28
> **Objective:** Agregar un botón "Usar servidor local" en Configuración de mobile y desktop que resuelve automáticamente la URL del backend local (sin tipear IPs), y hacer que el build de desarrollo del desktop arranque apuntando a local por defecto de forma consistente.

## Scope

**In:**

- **Mobile** (`apps/galfields-mobile`):
  - `services/api-base-url.ts`: nueva función que detecta la IP LAN que el celular ya usó para conectarse al bundler de Expo (`Constants.expoConfig?.hostUri` o equivalente), y arma `http://<esa-ip>:8080`.
  - `components/settings/server-screen.tsx`: nuevo botón "Usar servidor local" que llama a esa detección y guarda el resultado con el `setApiBaseUrl` que ya existe. Si la detección falla (por ejemplo, corriendo un build de producción sin Expo Go/dev client), el botón se oculta o muestra un mensaje claro — nunca guarda una URL vacía o inválida.

- **Desktop** (`apps/galfield-pos`):
  - `src-tauri/src/http_client.rs`: `api_base_url()` gana una rama para builds de debug (`cfg!(debug_assertions)`) — si el valor guardado en `sync.api_base_url` todavía es exactamente el default de producción sembrado por la migración (o sea, nunca lo tocaste a mano), devuelve `http://localhost:8080` en su lugar. Cualquier valor explícitamente guardado por vos gana siempre, sin importar el tipo de build.
  - `src/features/configuration/components/ConfigFormDefaults.vue`: nuevo botón "Usar servidor local" junto al campo "URL del servidor (nube)" que llena `http://localhost:8080` en el formulario (seguís usando el botón Guardar general que ya existe, igual que hoy).
  - Builds de producción (`npm run tauri build`) nunca entran a esta rama — siguen exactamente igual que hoy.

- Puerto fijo en **8080** para ambos presets (coincide con `SERVER_PORT` por defecto y el `compose.yaml` de hoy). El campo de texto libre que ya existe en ambas apps sigue disponible para cualquier otro puerto/URL.

**Out (queda para otro spec si hace falta):**

- Cualquier cambio en `backend/pos` (CORS, binding a `0.0.0.0`, etc.) — se asume que el backend local ya es alcanzable, tal como quedó armado con `compose.yaml`/`gradle bootRun`.
- Puerto configurable en el preset — siempre 8080; para otro puerto, se sigue escribiendo la URL completa a mano.
- Cualquier comportamiento distinto en builds de producción/release — mobile y desktop en producción siguen apuntando a la nube exactamente igual que hoy, sin excepciones.
- Detección automática sin botón en mobile (auto-aplicar sin que el usuario haga clic) — mobile siempre requiere la acción explícita del botón, a diferencia del default automático del build de dev en desktop.
- Nueva clave de configuración/migración para "trackear" si el valor fue tocado a mano — el desktop se resuelve con el heurístico de comparar contra el valor sembrado, sin tocar el schema.
- Escenarios donde el backend corre en una máquina distinta a la del bundler de Expo o la del Tauri dev — el feature asume que backend, Metro y Tauri corren en la misma máquina/red.

## Data model

No aplica — no se introduce ninguna estructura de datos nueva. Mobile reutiliza la clave `apiBaseUrl` de `AsyncStorage` que ya existe (vía el `setApiBaseUrl` ya existente). Desktop reutiliza la clave `sync.api_base_url` de `app_settings` que ya existe, sin tocar schema ni migraciones.

## Implementation plan

1. **Mobile**: agregar `detectLocalBackendUrl()` en `services/api-base-url.ts` — lee `Constants.expoConfig?.hostUri` (vía `expo-constants`, ya es dependencia del proyecto), extrae el host y arma `http://<host>:8080`. Devuelve `null` si no hay `hostUri` disponible. Sin UI todavía, nada se rompe.
   Test manual: loguear el resultado temporalmente y confirmar que, corriendo con `npm start` + Expo Go en el celular, devuelve la IP LAN correcta de la máquina de desarrollo.

2. **Mobile**: agregar el botón "Usar servidor local" en `components/settings/server-screen.tsx`, que llama `detectLocalBackendUrl()` y, si no es `null`, guarda el resultado con el `setApiBaseUrl()` que ya existe. Oculto o deshabilitado si la detección devuelve `null`.
   Test manual: desde el celular físico, tocar el botón, confirmar que la URL guardada es la correcta y que una llamada real (ej. login) contra el backend local funciona.

3. **Desktop**: en `src-tauri/src/http_client.rs`, agregar la constante `LOCAL_DEV_DEFAULT = "http://localhost:8080"` y la rama condicional en `api_base_url()`: si es build de debug (`cfg!(debug_assertions)`) y el valor guardado en `sync.api_base_url` sigue siendo exactamente `DEFAULT_API_BASE_URL` (nunca lo tocaste desde Configuración), devuelve `LOCAL_DEV_DEFAULT` en su lugar.
   Test manual: correr `npm run tauri dev` con una base local sin tocar Configuración todavía, y confirmar en los logs (`logging::step`) que las llamadas salen hacia `http://localhost:8080`.

4. **Desktop**: agregar el botón "Usar servidor local" en `ConfigFormDefaults.vue`, junto al campo "URL del servidor (nube)" — llena `http://localhost:8080` en el formulario (no guarda solo; seguís usando el botón Guardar general que ya existe).
   Test manual: click en el botón, click en Guardar, confirmar que `sync.api_base_url` queda en `http://localhost:8080` y que un ciclo de sync/venta sigue funcionando contra el backend local.

## Acceptance criteria

- [ ] Mobile: con el celular físico conectado vía Expo Go, tocar "Usar servidor local" en Configuración → Servidor llena una URL con la IP LAN correcta de la máquina de desarrollo (no `localhost`).
- [ ] Mobile: tras tocar el botón, una llamada real al backend (ej. login) funciona correctamente contra el backend local.
- [ ] Mobile: si la detección no está disponible (ej. build de producción sin Expo Go/dev client), el botón se oculta o muestra un error claro — nunca guarda una URL vacía o inválida.
- [ ] Desktop: al correr `npm run tauri dev` sobre una base de datos local que nunca tuvo `sync.api_base_url` editado a mano, la app usa `http://localhost:8080` automáticamente, sin ninguna acción manual.
- [ ] Desktop: si el usuario edita manualmente `sync.api_base_url` a cualquier otro valor (local o no) y lo guarda, ese valor gana siempre, en dev y en producción.
- [ ] Desktop: el botón "Usar servidor local" en Configuración llena el campo con `http://localhost:8080`; tras guardar con el botón general, el ciclo de sync/venta funciona contra el backend local.
- [ ] Desktop: `npm run tauri build` (producción) nunca resuelve a `localhost:8080` — siempre el default de nube, sin importar el estado de `sync.api_base_url`.
- [ ] Ninguno de los dos apps requiere cambios en `backend/pos` para que esto funcione.

## Decisions

- **Yes:** auto-detectar la IP LAN de mobile vía `Constants.expoConfig?.hostUri` en vez de pedirla a mano. Cero tipeo, y sigue funcionando aunque cambies de red WiFi.
- **No:** guardar una IP LAN fija como preset manual. Se rompe apenas cambia la red o el DHCP renueva la IP — exactamente el problema que se quiere evitar.
- **Yes:** el build de desarrollo del desktop arranca en local por default, salvo override explícito guardado por el usuario. Pedido explícito ("que sea consistente"), reduce fricción en cada `npm run tauri dev`.
- **No:** agregar una clave nueva de tracking (ej. `sync.api_base_url_source`) para distinguir "nunca tocado" de "explícitamente guardado". Complejidad innecesaria — se resuelve con un heurístico simple (comparar contra el valor sembrado por la migración), sin tocar schema ni el flujo de guardado existente.
- **Yes:** agregar el mismo botón "Usar servidor local" en desktop, además del default automático. Simetría con mobile, y permite volver a local con un clic si en medio de una sesión se apuntó manualmente a producción para comparar algo.
- **Yes:** puerto fijo en 8080 para ambos presets, no configurable desde el botón. Coincide con el default real de la app y la infra local ya armada; el campo de texto libre existente cubre cualquier otro puerto.
- **No:** cualquier cambio en `backend/pos` (CORS, binding, etc.). Ya se confirmó que el backend es alcanzable tal como quedó armado con `compose.yaml`/`gradle bootRun`.
- **No:** auto-aplicar la URL local en mobile sin botón. Se mantiene la acción explícita del usuario — evita sorpresas si el celular se conecta a una red distinta sin que el usuario lo note.

## Risks

| Risk | Mitigation |
|---|---|
| El heurístico de desktop (comparar contra el valor sembrado) se rompe si alguna vez guardaste manualmente el texto exacto de la URL de producción — un futuro build de dev lo interpretaría como "nunca tocado" y lo pisaría con local | Caso extremadamente raro (nadie teclea a mano la URL completa de producción en desarrollo); se documenta como limitación conocida, sin plumbing adicional para resolverlo |
| El celular puede estar en una red distinta a la de la máquina de desarrollo (datos móviles, WiFi de invitados con aislamiento de clientes) — la IP detectada no sería alcanzable | Sin mitigación automática; el campo de texto libre ya existente sigue disponible para escribir la URL correcta a mano si la detección falla en la práctica |
| Si el backend corre en un contenedor con el puerto expuesto solo a `127.0.0.1` del host (no a la interfaz LAN), el celular físico no lo alcanza aunque la URL detectada sea correcta | Fuera de alcance de este spec — verificar que el firewall/red de la máquina de desarrollo permite conexiones entrantes al puerto 8080 desde la red local |
