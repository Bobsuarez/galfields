# SPEC 01 — Login de empleados, roles y terminales

> **Status:** Approved
> **Depends on:** Ninguno (primer spec del repo)
> **Date:** 2026-07-28
> **Objective:** Agregar autenticación de empleados (JWT diario) con roles y permisos dinámicos por módulo, gestionada desde mobile, con login de cajero por terminal en galfield-pos y caché offline de sesión.

## Scope

**In:**

- **Backend (`backend/pos`)**
  - Agregar `spring-boot-starter-security`; autenticación JWT integrada a un `SecurityFilterChain`.
  - Tabla nueva `terminals` (`terminal_code` UNIQUE, `name`, `is_active`).
  - Migrar `invoice_numbering_ranges.terminal_code` (string) → `terminal_id` FK a `terminals` (migración de datos: crear una fila `terminals` por cada `terminal_code` distinto ya existente, luego cambiar la columna).
  - Tabla nueva `employee_terminals` (many-to-many empleado ↔ terminal).
  - `POST /api/auth/login`: valida usuario/clave (+ `terminalCode` opcional, obligatorio para login de `Cajero`, validado contra `employee_terminals`); devuelve JWT que siempre vence a la medianoche siguiente (América/Bogotá), sin importar la hora de emisión.
  - CRUD de empleados (`/api/employees`): crear/listar/editar/desactivar; clave hasheada (BCrypt); asigna rol + terminales.
  - CRUD de roles (`/api/employee-roles`): crear/listar/editar/eliminar; `permissions` como mapa booleano por módulo (`pos`, `inventario`, `reportes`, `sync`).
  - CRUD de terminales (`/api/terminals`): crear/listar/editar/eliminar.
  - Seed de 2 roles: `Administrador` (`canLoginMobile=true`, todos los módulos en `true`) y `Cajero` (`canLoginDesktop=true`, `pos`/`sync` en `true`, `inventario`/`reportes` en `false`).
  - `SecurityFilterChain` centraliza el mapeo endpoint → autorización requerida (derivado de los permisos embebidos en el JWT) — sin checks manuales por controller.
  - `AuthenticationEntryPoint`/`AccessDeniedHandler` centralizados, integrados a la misma convención de error limpio que ya usa `GlobalExceptionHandler`.
  - El código de acceso a Reportes (`/api/reports-access-code*`) sigue igual, como capa adicional sobre el permiso `reportes` (coexisten).

- **Mobile (`apps/galfields-mobile`)**
  - Reemplazar el login demo (`auth-context.tsx`, `admin`/`1234`) por login real contra `POST /api/auth/login` — solo empleados con `canLoginMobile=true` pueden entrar aquí.
  - Configuración → **Empleados**: listar/crear/editar (usuario, clave, rol, terminales asignadas).
  - Configuración → **Roles**: listar/crear/editar permisos booleanos por módulo + flags `canLoginMobile`/`canLoginDesktop`.
  - Configuración → **Terminales**: listar/crear/editar (mismo registro que ya consume Numeración de facturas, ahora vía `terminal_id`).
  - JWT persistido (AsyncStorage, mismo patrón que `api-base-url.ts`) y enviado en cada llamada autenticada.

- **Desktop (`apps/galfield-pos`)**
  - Pantalla de login (usuario/clave) al arrancar si no hay sesión cacheada válida para el día en curso. **Configuración queda siempre accesible, sin login.**
  - Login manda el `terminal_code` ya configurado localmente; solo pasa si el empleado tiene esa terminal asignada.
  - Sesión (JWT + empleado) cacheada en SQLite, válida el resto del día calendario sin importar conectividad — no vuelve a pedir clave para sync o ventas hasta medianoche o logout explícito.
  - Acción "Cerrar sesión" explícita (permite otro cajero en la misma terminal el mismo día).
  - Rutas/menú (POS/Inventario/Reportes/Sync) ocultas o bloqueadas según los permisos del rol logueado.
  - Cada llamada de `http_client.rs` manda `Authorization: Bearer <jwt>` de la sesión cacheada.
  - Reportes conserva su modal de código de acceso existente, como capa adicional sobre el permiso `reportes`.

**Out of scope (for future specs):**

- Atribución real del vendedor en `POST /api/sales` (sigue bajo el placeholder `pos-terminal`) — spec aparte.
- Recuperación/reset de clave olvidada (el admin edita el empleado desde mobile y le pone una nueva).
- Múltiples roles por empleado (sigue un solo `role_id` por empleado, como ya está en el schema).
- Permisos finos por acción dentro de un módulo (solo booleano por módulo completo, por ahora).
- Wizard de "primer arranque sin login" — Configuración ya resuelve esto quedando abierta.

## Data model

### Backend (`backend/pos`, nueva migración `VN__employee_auth.sql`)

```sql
CREATE TABLE terminals (
    terminal_id   BIGSERIAL PRIMARY KEY,
    terminal_code VARCHAR(50) UNIQUE NOT NULL,
    name          VARCHAR(100),
    is_active     BOOLEAN NOT NULL DEFAULT true,
    created_at    TIMESTAMP NOT NULL DEFAULT now()
);

-- Migración de datos: una fila terminals por cada terminal_code distinto
-- ya existente en invoice_numbering_ranges, luego swap de columna.
ALTER TABLE invoice_numbering_ranges ADD COLUMN terminal_id BIGINT REFERENCES terminals(terminal_id);
-- (backfill terminal_id desde terminal_code aquí)
ALTER TABLE invoice_numbering_ranges DROP COLUMN terminal_code;
ALTER TABLE invoice_numbering_ranges ALTER COLUMN terminal_id SET NOT NULL;

CREATE TABLE employee_terminals (
    employee_id BIGINT NOT NULL REFERENCES employees(employee_id),
    terminal_id BIGINT NOT NULL REFERENCES terminals(terminal_id),
    PRIMARY KEY (employee_id, terminal_id)
);

-- employee_roles.permissions ya existe (JSON) — se usa así:
-- { "pos": true, "inventario": true, "reportes": false, "sync": true }

ALTER TABLE employee_roles ADD COLUMN can_login_mobile  BOOLEAN NOT NULL DEFAULT false;
ALTER TABLE employee_roles ADD COLUMN can_login_desktop BOOLEAN NOT NULL DEFAULT false;

-- Seed:
-- Administrador: can_login_mobile=true,  can_login_desktop=false, permissions: {pos:true, inventario:true, reportes:true, sync:true}
-- Cajero:        can_login_mobile=false, can_login_desktop=true,  permissions: {pos:true, inventario:false, reportes:false, sync:true}
```

`can_login_mobile`/`can_login_desktop` reemplazan comparar por nombre de rol ("Administrador") — así el sistema sigue siendo dinámico: cualquier rol futuro puede marcarse para loguear en uno u otro lado (o ambos) sin tocar código.

### JWT claims

```json
{
  "sub": "employeeId",
  "username": "jperez",
  "roleId": 2,
  "roleName": "Cajero",
  "permissions": { "pos": true, "inventario": false, "reportes": false, "sync": true },
  "terminalId": 3,
  "exp": 1735689600
}
```

- `terminalId` solo está presente en logins de escritorio (login validado contra `employee_terminals`); ausente en logins de mobile.
- `exp` siempre es la medianoche siguiente (América/Bogotá) al momento del login, no una duración fija — login a las 8pm vence a las 12am, login a las 8am vence esa misma noche.

### Mapeo endpoint → autorización requerida (`SecurityFilterChain`)

| Endpoint | Autorización requerida |
|---|---|
| `POST /api/auth/login` | Público |
| `/api/employees/**`, `/api/employee-roles/**`, `/api/terminals/**`, `/api/invoice-numbering-ranges/**` | `can_login_mobile` (rol administrativo) |
| `POST /api/reports-access-code` (generar) | `can_login_mobile` |
| `POST /api/reports-access-code/validate` | permiso `reportes` |
| `GET /api/products`, `/api/categories`, `/api/payment-methods` (pull de catálogo) | `can_login_mobile` **o** permiso `sync` |
| `POST/PUT/DELETE /api/products/**`, `/api/categories/**`, `/api/brands/**`, `/api/locations/**`, `/api/payment-methods/**` | `can_login_mobile` |
| `POST /api/inventory/adjustments`, `GET /api/reports/inventory`, `GET /api/reports/low-stock` | `can_login_mobile` |
| `POST /api/sales` | permiso `pos` |
| `GET /api/reports/sales-summary`, `/sales-by-payment-method`, `/invoices**` | `can_login_mobile` **o** permiso `reportes` |

`inventario` (módulo de galfield-pos) no llama al backend directamente — lee `products` local vía `invoke()`, así que no necesita gate en el `SecurityFilterChain`; solo gatea la ruta `/inventario` en el router de Vue.

### Desktop (galfield-pos) — sesión cacheada

Nueva tabla local (`src-tauri/migrations/NNN_employee_session.sql`):

```sql
CREATE TABLE employee_session (
    id            INTEGER PRIMARY KEY CHECK (id = 1),  -- una sola fila
    employee_id   INTEGER NOT NULL,
    username      TEXT NOT NULL,
    role_name     TEXT NOT NULL,
    permissions   TEXT NOT NULL,   -- JSON: {"pos":true,"inventario":false,...}
    jwt           TEXT NOT NULL,
    expires_at    TEXT NOT NULL    -- ISO datetime, medianoche del día de login
);
```

`CHECK (id = 1)` fuerza una sola sesión activa por instalación — loguear un nuevo cajero (o logout) reemplaza la fila.

### Mobile — sesión

AsyncStorage, mismo patrón que `api-base-url.ts`: clave `auth.jwt`, clave `auth.employee` (JSON con `employeeId`/`username`/`roleName`/`permissions`). Sin expiración explícita del lado cliente — el JWT ya trae `exp`; una llamada que devuelva 401 fuerza logout y vuelve a `/login`.

## Implementation plan

1. Backend: agregar `spring-boot-starter-security`; migración `VN__employee_auth.sql` (tabla `terminals`, `employee_terminals`, columnas nuevas en `employee_roles`, migración de datos `invoice_numbering_ranges.terminal_code` → `terminal_id`, seed roles `Administrador`/`Cajero`). Actualizar entidades `EmployeeRole`/`InvoiceNumberingRange`, agregar entidad `Terminal`.
   Test manual: migración corre limpia contra DB dev; `\d terminals`, `\d employee_terminals`, `\d invoice_numbering_ranges` muestran el schema esperado.

2. Backend: `TerminalController`/`Service`/`Repository` — CRUD `/api/terminals` (mismo shape que categories/brands/locations).
   Test manual: crear/listar/editar/eliminar terminal vía Swagger.

3. Backend: actualizar `InvoiceNumberingRangeController`/`Service`/DTOs para usar `terminalId` en vez de `terminal_code` string; `GET /by-terminal/{terminalCode}` sigue funcionando vía join a `terminals`.
   Test manual: endpoints de numeración de facturas funcionan igual que antes contra el schema migrado.

4. Backend: `EmployeeController`/`Service`/DTOs — CRUD `/api/employees` (crear/listar/editar/desactivar), clave hasheada con `BCryptPasswordEncoder`, request incluye `roleId` + `terminalIds`.
   Test manual: crear empleado vía Swagger, confirmar `password_hash` es bcrypt, no texto plano.

5. Backend: `EmployeeRoleController`/`Service`/DTOs — CRUD `/api/employee-roles`, `permissions` como mapa booleano, más `canLoginMobile`/`canLoginDesktop`.
   Test manual: crear rol custom vía Swagger con permisos a medida.

6. Backend: `AuthController`/`AuthService` — `POST /api/auth/login`: valida usuario/clave, valida `terminalCode` contra `employee_terminals` + `canLoginDesktop` (o `canLoginMobile` si no viene `terminalCode`), emite JWT con `exp` = medianoche siguiente (América/Bogotá).
   Test manual: login vía curl con empleado sembrado, decodificar JWT y confirmar claims/`exp`.

7. Backend: `SecurityConfig` — filtro JWT (`OncePerRequestFilter`), `SecurityFilterChain` con el mapeo endpoint→autorización de la tabla del Data model, `AuthenticationEntryPoint`/`AccessDeniedHandler` con el mismo formato de error de `GlobalExceptionHandler`.
   Test manual: request sin token → 401 limpio; con token sin permiso → 403 limpio; con token+permiso → 200.

8. Mobile: `services/auth-api.ts` + reemplazar lógica demo de `contexts/auth-context.tsx` por login real contra el backend, persistido en AsyncStorage.
   Test manual: loguear con el `Administrador` sembrado, sesión sobrevive un reload de la app.

9. Mobile: Configuración → **Terminales** (`services/terminals-api.ts`, listar/crear/editar).
   Test manual: crear terminal desde mobile, aparece en la lista.

10. Mobile: Configuración → **Roles** (`services/employee-roles-api.ts`, grid de permisos booleanos + toggles `canLoginMobile`/`canLoginDesktop`).
    Test manual: crear rol, togglear permisos, confirmar que persiste.

11. Mobile: Configuración → **Empleados** (`services/employees-api.ts`, usuario/clave/rol/terminales asignadas).
    Test manual: crear un `Cajero` asignado a la terminal creada en el paso 9.

12. Mobile: helper compartido que adjunta `Authorization: Bearer` en cada llamada autenticada; 401 global fuerza logout + redirect a `/login`.
    Test manual: invalidar el token a mano, confirmar que la app desloguea limpio en vez de mostrar error crudo.

13. Desktop: módulo Rust `auth.rs` (`login`/`get_session`/`logout` commands) + migración `NNN_employee_session.sql`.
    Test manual: `cargo test` compila; `login` invocado a mano escribe la fila esperada en `employee_session`.

14. Desktop: pantalla de login (Vue) mostrada al arrancar si no hay sesión válida; Configuración queda fuera del gate.
    Test manual: arranque limpio muestra login; Configuración accesible sin loguear; tras loguear, POS/Inventario/Reportes/Sync quedan alcanzables.

15. Desktop: `useEmployeeSession.ts` + guards de router que ocultan/bloquean rutas según permisos del rol logueado; acción "Cerrar sesión" en el sidebar.
    Test manual: loguear con rol `reportes:false`, confirmar que la ruta/ítem de Reportes desaparece o bloquea.

16. Desktop: adjuntar `Authorization: Bearer <jwt>` (de `employee_session`) en cada llamada de `http_client.rs` (sync, push de ventas, pull de numeración).
    Test manual: ciclo completo offline→online de sync/venta sigue funcionando con sesión activa; sin sesión (o vencida), falla gracioso y el retry loop sigue reintentando sin crashear.

17. Desktop: expiración a medianoche — cada arranque/foreground compara `employee_session.expires_at` contra la hora local; limpia sesión y fuerza login si ya venció.
    Test manual: adelantar `expires_at` a mano en la DB local, relanzar app, confirmar que vuelve a pedir login.

## Acceptance criteria

- [ ] `POST /api/auth/login` con usuario/clave válidos de un empleado `canLoginMobile=true` (sin `terminalCode`) devuelve 200 + JWT.
- [ ] `POST /api/auth/login` con `terminalCode` no asignado al empleado devuelve 401/403, sin JWT.
- [ ] Un JWT emitido a las 20:00 trae `exp` = 00:00 del día siguiente (no +24h fijas).
- [ ] Llamar cualquier endpoint protegido sin `Authorization` header devuelve 401 en el mismo formato JSON que `GlobalExceptionHandler`.
- [ ] Llamar un endpoint con JWT válido pero sin el permiso/rol requerido devuelve 403 en el mismo formato.
- [ ] `POST /api/employees` guarda `password_hash` como bcrypt, nunca texto plano.
- [ ] La migración crea automáticamente una fila `terminals` por cada `terminal_code` distinto ya existente en `invoice_numbering_ranges`, y cada rango queda con el `terminal_id` correcto.
- [ ] `GET /api/invoice-numbering-ranges/by-terminal/{terminalCode}` sigue devolviendo el rango correcto tras la migración.
- [ ] Mobile: loguear con un empleado `canLoginMobile=false` (ej. rol Cajero) es rechazado.
- [ ] Mobile: Configuración → Empleados crea un empleado con rol y N terminales asignadas.
- [ ] Mobile: Configuración → Roles crea un rol y permite togglear sus 4 permisos + `canLoginMobile`/`canLoginDesktop`.
- [ ] Mobile: Configuración → Terminales crea/edita/elimina una terminal.
- [ ] Desktop: sin sesión válida al arrancar, se muestra login; Configuración es accesible sin loguear.
- [ ] Desktop: un `Cajero` válido para la terminal local configurada puede loguear y entrar a POS.
- [ ] Desktop: loguear con una terminal no asignada al empleado es rechazado con mensaje claro.
- [ ] Desktop: cerrar y reabrir la app el mismo día no vuelve a pedir login (sesión cacheada sigue vigente).
- [ ] Desktop: pasada la medianoche, abrir la app vuelve a pedir login.
- [ ] Desktop: un rol con `reportes=false` no ve ni puede entrar al módulo Reportes.
- [ ] Desktop: un rol con `reportes=true` que entra a Reportes igual debe pasar el código de acceso existente (ambas capas coexisten).
- [ ] Desktop: "Cerrar sesión" permite que otro cajero loguee en la misma terminal el mismo día.
- [ ] Desktop: sync de catálogo y envío de ventas siguen funcionando sin re-pedir clave mientras la sesión esté vigente.

## Decisions

- **Yes:** tabla nueva `terminals`, con FK desde `invoice_numbering_ranges`. Unifica el concepto de terminal en un solo lugar en vez de dos strings paralelos que alguien tendría que mantener sincronizados a mano.
- **No:** dejar `invoice_numbering_ranges.terminal_code` como string suelto sin relación. Hubiera dejado dos fuentes de verdad para lo mismo.
- **Yes:** JWT stateless, sin tabla de sesiones en backend. Más simple, y no rompe el patrón "sin auth pesado" que ya tiene el resto del backend.
- **No:** sesión persistida server-side con tabla invalidable. Overhead innecesario para un negocio de un solo local.
- **Yes:** `spring-boot-starter-security` en vez de filtro hecho a mano. Mecanismo probado, calza con el `SecurityFilterChain` centralizado que pidió el usuario.
- **Yes:** permisos como mapa booleano por módulo, sin granularidad por acción. Alcanza para el caso de uso actual; roles dinámicos ya permiten nuevas combinaciones sin tocar código.
- **No:** permiso `configuracion`. Configuración quedó fuera del login-gate por completo, ese permiso no bloquearía nada.
- **Yes:** `canLoginMobile`/`canLoginDesktop` como flags en `employee_roles`, en vez de hardcodear "Administrador" por nombre. Mantiene el sistema dinámico — un rol nuevo puede habilitarse para cualquiera de los dos lados sin tocar código.
- **Yes:** sesión de empleado en galfield-pos vive todo el día calendario en caché local, sin exigir red para sync/ventas. Respeta el diseño offline-first ya existente de la app.
- **No:** credencial de máquina separada por terminal para el sync en background. El usuario prefirió que la sesión de empleado sea la única credencial; el retry loop ya tolera fallos y se recupera solo cuando alguien loguea.
- **No:** atribución real de vendedor en `POST /api/sales` en este spec. Se deja para spec aparte, evita mezclar "login" con "reporting de ventas".
- **Yes:** código de acceso a Reportes existente coexiste con el nuevo permiso `reportes`, no se reemplaza. Mantiene la capa de seguridad ya construida sin regresión.

## Risks

| Risk | Mitigation |
|---|---|
| Migración de `invoice_numbering_ranges.terminal_code` a FK pierde datos o falla contra la DB de producción (ya baselineada a mano, ver nota de Flyway en `backend/pos`) | Probar la migración completa contra un dump real de producción antes de aplicar; backfill idempotente, verificado con `\d`/`SELECT` antes de dropear la columna vieja |
| Proteger endpoints ya existentes con JWT rompe clientes en producción que hoy los llaman sin token | Desplegar backend con el filtro activo solo cuando mobile y desktop ya tengan el login integrado y probado; coordinar el despliegue de los 3 componentes en el mismo ciclo |
| Zona horaria mal configurada hace que "medianoche" no coincida con la hora real de Colombia | Fijar explícitamente `America/Bogota` al calcular `exp`, no depender del timezone por defecto del servidor |
| `SecurityFilterChain` mal configurado deja sin protección un endpoint sensible por accidente | La tabla endpoint→autorización de este spec como referencia única; agregar test de integración que golpee cada endpoint sin token y espere 401 |

## What is **not** in this spec

- Atribución real del vendedor en `POST /api/sales` (sigue el placeholder `pos-terminal`) — spec aparte.
- Recuperación/reset de clave olvidada.
- Múltiples roles por empleado.
- Permisos finos por acción dentro de un módulo (solo booleano por módulo completo).
- Wizard de "primer arranque sin login" (Configuración ya queda abierta, no hace falta).

Cada uno de estos, si se hace, va en su propio spec.
