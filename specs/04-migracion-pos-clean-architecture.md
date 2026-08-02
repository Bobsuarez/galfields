# SPEC 04 — Migración de `backend/pos` a `backend/pos_transactions` (Clean Architecture Bancolombia)

> **Status:** Approved
> **Depends on:** SPEC 01, SPEC 02, SPEC 03
> **Date:** 2026-08-02
> **Objective:** Spec único que define plan de migración completa de `backend/pos` (arquitectura por capas) a `backend/pos_transactions` (scaffold Clean Architecture de Bancolombia), en 10 fases por módulo ejecutadas en un solo flujo continuo (mismo spec, mismo ciclo `/spec-impl`, sin specs hijos ni pausas de aprobación entre fases) y desplegadas en corte único cuando `pos_transactions` alcance paridad 100%.

## Scope

**In:**

- Definir orden y alcance de las 10 fases (una por módulo), listadas en el plan de implementación, ejecutadas como un único flujo continuo dentro de este mismo spec — sin specs hijos ni pausas de aprobación entre fases.
- Fijar convenciones compartidas por todas las fases: package base `co.com.galfields.pos_transactions`, estructura de módulos Gradle del scaffold (`domain/model`, `domain/usecase`, `infrastructure/entry-points/api-rest`, `infrastructure/driven-adapters`, `infrastructure/helpers`, `applications/app-service`), y regla de nomenclatura de paquetes dentro de cada capa (dominio por caso de uso, ej. `usecase.sale`, `model.sale`).
- Estrategia de Flyway: `pos_transactions` continúa la numeración existente de `backend/pos` (`V11__` en adelante) contra el mismo Postgres/mismo schema; `backend/pos` sigue siendo el único que corre migraciones hasta el corte final (Módulo 10), momento en que `pos_transactions` hereda la propiedad de `flyway_schema_history`.
- Estrategia de despliegue: `pos_transactions` no se despliega a producción hasta que el Módulo 10 (corte final) esté implementado; hasta entonces corre solo local/dev contra copia del schema.
- Estrategia de verificación: cada fase valida su módulo con tests JUnit (mismo framework que `backend/pos`) que confirman comportamiento equivalente al endpoint original, en verde antes de avanzar a la siguiente fase.
- Reemplazo final: el Módulo 10 borra el código de `backend/pos` del repo, crea el workflow CI/CD nuevo para `pos_transactions` (espejo de `deploy-pos-backend.yml`) apuntando al mismo servicio/namespace k8s, y retira el workflow viejo.

**Out of scope (for future specs):**

- Implementación de código de cualquier módulo — vive en la fase correspondiente de este mismo spec, ejecutada vía `/spec-impl` como parte del flujo único.
- Cambios de comportamiento/contrato de API respecto a `backend/pos` — esta migración es estructural (capas → clean architecture), no funcional; cualquier cambio de contrato detectado durante una fase se documenta en la sección Decisions de este spec como decisión explícita, no se asume aquí.
- Multi-tenant, multi-base de datos, o cualquier cambio de infraestructura de datos — se reutiliza el mismo Postgres/MinIO tal cual existen hoy.

## Data model

Este spec no introduce estructuras de datos nuevas. El schema de Postgres (`doc/data_base.sql`) y los buckets de MinIO se reutilizan sin cambios — es una migración estructural de código (capas → clean architecture), no de datos. Cada fase documenta, si aplica, los DTOs/modelos de dominio (`domain/model`) que le correspondan a su módulo, dentro de este mismo documento; la convención de paquetes que todas las fases deben seguir es:

```
co.com.galfields.pos_transactions
├── model.<módulo>       # domain/model — entidades de dominio puras, sin anotaciones JPA
├── usecase.<módulo>     # domain/usecase — casos de uso, orquestan model
├── api.<módulo>         # infrastructure/entry-points/api-rest — controllers/DTOs de entrada/salida
├── config               # applications/app-service — wiring, @ComponentScan, MainApplication
└── <adapter>.<módulo>   # infrastructure/driven-adapters — un módulo Gradle por tecnología (jpa, minio, etc.), creado por la primera fase que lo necesite
```

`<módulo>` = nombre del módulo del plan de implementación (ej. `sale`, `inventory`, `catalog`, `employee`).

## Implementation plan

Un solo flujo `/spec-impl` sobre este mismo spec, corrido de punta a punta en secuencia. No se crean specs hijos ni archivos nuevos en `specs/` por módulo; las 10 fases de abajo son etapas del mismo flujo, no documentos separados. Cada fase arranca apenas la anterior tiene sus tests JUnit en verde — sin pausa de aprobación entre fases ni re-invocación de `/spec`. "Módulo N" abajo nombra la fase, no un número de spec.

1. **Fase 1 — Base/fundación.** Datasource Postgres (mismo esquema `DB_URL_PRIMARY`/`DB_URL_REPLICA` + `RoutingDataSource`), config MinIO, package base `co.com.galfields.pos_transactions`, healthcheck, arranque de `MainApplication` contra BD/MinIO de dev.
2. **Fase 2 — Ventas/Transacciones.** `POST /api/sales`, cancelación (`.../cancel`, `.../by-client-event/{id}/cancel`), driven adapter JPA para `sales_transactions`/`sale_items`/`payments`. Depende de la Fase 1.
3. **Fase 3 — Inventario.** `POST /api/inventory/adjustments`, driven adapter JPA para `inventory`/`stock_adjustments`. Depende de la Fase 1; coordinar con Fase 2 (comparten tabla `inventory`).
4. **Fase 4 — Catálogo.** Productos/variantes/categorías/marcas/ubicaciones/métodos de pago/unidades de venta (`/api/products`, `/api/categories`, `/api/brands`, `/api/locations`, `/api/payment-methods`, `/api/product-variants/{id}/units`), incluye compresión/subida de imágenes a MinIO.
5. **Fase 5 — Empleados/Roles/Terminales + Auth.** `/api/employees`, `/api/employee-roles`, `/api/terminals`, `/api/auth/login`, `SecurityFilterChain`/JWT completo, tabla de autorización endpoint→permiso reconstruida 1:1.
6. **Fase 6 — Numeración de facturas.** `/api/invoice-numbering-ranges` (CRUD + `by-terminal`).
7. **Fase 7 — Código de acceso a reportes.** `/api/reports-access-code` (generar + validar).
8. **Fase 8 — Reportes.** `/api/reports/*` (sales-summary, sales-by-payment-method, invoices, inventory, low-stock).
9. **Fase 9 — Transversales.** `ImageCompressor` y wiring Swagger/OpenAPI a la nueva estructura, cierra utilidades compartidas sueltas de las fases 4/8.
10. **Fase 10 — Corte final.** (a) workflow CI/CD nuevo para `pos_transactions` (espejo de `deploy-pos-backend.yml`, mismo servicio/namespace k8s), (b) transferencia de propiedad de Flyway, (c) borrado de `backend/pos` y su workflow viejo, (d) actualización de `CLAUDE.md` raíz y de `backend/pos_transactions`.

## Acceptance criteria

- [ ] Las 10 fases del plan se ejecutan en orden dentro de un único flujo `/spec-impl` sobre este spec — sin specs hijos separados ni pausa de aprobación entre fases; cada fase tiene sus tests JUnit en verde antes de que arranque la siguiente.
- [ ] `pos_transactions` tiene test JUnit por endpoint migrado, validando comportamiento equivalente al de `backend/pos`.
- [ ] `pos_transactions` no está desplegado en producción en ningún momento antes de que el Módulo 10 esté implementado.
- [ ] Tras el Módulo 10, `backend/pos` ya no existe en el repo y su workflow de CI/CD (`deploy-pos-backend.yml`) fue eliminado.
- [ ] El workflow de CI/CD de `pos_transactions` despliega al mismo servicio/namespace k8s que antes usaba `backend/pos`.
- [ ] `pos_transactions` corre Flyway contra el mismo Postgres, continuando la numeración desde donde `backend/pos` la dejó; `backend/pos` ya no tiene configuración de Flyway activa.
- [ ] `CLAUDE.md` raíz y `backend/pos_transactions/CLAUDE.md` reflejan el estado final (arquitectura, comandos, convenciones) sin referencias obsoletas a `backend/pos`.

## Decisions

- **Sí:** migración por fases (10 módulos) en vez de un big-bang de código sin división. Razón: >3 dominios, imposible verificar paridad de todo a la vez sin romper trazabilidad.
- **Sí:** las 10 fases corren como un solo flujo de trabajo continuo (este mismo spec, un solo ciclo `/spec-impl`) en vez de 10 specs hijos independientes con pausa de aprobación entre cada uno. Decisión del usuario, 2026-08-02 — reemplaza el enfoque original de spec padre + hijos. Razón: las fases comparten convenciones y dependen linealmente unas de otras; documentos separados con aprobación manual entre cada uno frenaban el ritmo sin aportar trazabilidad adicional sobre la que ya dan los tests JUnit por fase.
- **No:** corte progresivo (routing dividido entre `pos` y `pos_transactions` mientras dura la migración). Descartado explícitamente por el usuario — `pos_transactions` permanece sin desplegar hasta el 100%.
- **Sí:** `pos_transactions` reutiliza la misma base de datos/schema que `backend/pos` — es migración de código, no de datos.
- **Sí:** `pos_transactions` continúa la numeración de Flyway existente (`V11__` en adelante) en vez de regenerar el schema desde cero — evita reconciliar dos historiales de migración distintos en el corte.
- **Sí:** package base cambia de `com.kinforgeworks` (genérico del scaffold) a `co.com.galfields.pos_transactions`, coherente con `co.com.galfields.pos`.
- **Sí:** orden de módulos empieza por Ventas/Transacciones (Módulo 2, tras la fundación en Módulo 1) — es el núcleo del negocio y el que da nombre al proyecto.
- **No:** reordenar Auth antes de Catálogo/Inventario. El usuario confirmó el orden propuesto (fundación → ventas → inventario → catálogo → auth → ...) tal cual, aunque Auth protege esos endpoints en `backend/pos` hoy — cada módulo migrado corre sin protección real hasta que el Módulo 5 la añada, aceptable porque nada se despliega antes del corte final.

## Risks

| Riesgo | Mitigación |
|---|---|
| `backend/pos` sigue recibiendo cambios (specs nuevos, fixes) mientras dura la migración de meses, dejando a `pos_transactions` desactualizado antes de llegar al corte | Toda migración Flyway nueva agregada a `backend/pos` durante la transición debe copiarse también a `pos_transactions` (mismo número de versión); cualquier spec de feature nuevo sobre `backend/pos` debe reflejarse en el módulo hijo correspondiente antes del corte final |
| Doble Flyway apuntando a la misma base productiva si `pos_transactions` se configura por error contra la BD real antes del corte | Durante desarrollo, `pos_transactions` corre solo contra una copia local/dev del schema (sembrada desde `doc/data_base.sql`), nunca contra la BD compartida real; no existe pipeline de CI/CD para `pos_transactions` hasta el Módulo 10, así que no hay forma automatizada de desplegarlo antes de tiempo |
| Endpoints migrados en Módulos 1-4 quedan sin protección real (Auth llega en el Módulo 5) | Aceptable solo porque `pos_transactions` nunca se expone a producción antes del corte final (ver Acceptance criteria) — si esta premisa cambia, este riesgo debe reevaluarse |
| Migración de 10 fases es larga — riesgo de abandonar a mitad de camino con dos backends parcialmente vivos | Cada fase debe dejar `pos_transactions` funcional y testeado por sí sola antes de arrancar la siguiente, así que una pausa a mitad de camino nunca deja código roto, solo incompleto |
| Flujo continuo sin specs hijos separados dificulta retomar el trabajo si se interrumpe a mitad de una fase (sin documento propio que marque "dónde quedó") | Este spec es la única fuente de verdad de las 10 fases; el estado de avance se lee del código de `pos_transactions` y sus tests, no de archivos de spec adicionales — retomar es "última fase con tests en verde + 1" |

## Qué **no** está en este spec

- Código de ningún módulo se documenta como diseño detallado aparte — cada fase (Módulos 1–10 del plan) se implementa directamente vía `/spec-impl` sobre este documento, sin spec propio.
- Cambios de contrato de API/comportamiento respecto a `backend/pos` — migración estructural, no funcional.
- Despliegue progresivo/routing dividido entre `pos` y `pos_transactions` — corte único al completar el Módulo 10.
- Cambios de infraestructura de datos (multi-tenant, otra base de datos, etc.) — mismo Postgres/MinIO de siempre.

Cada uno de estos, si se necesita, va en su propio spec.
