---
name: java-shadow-entities
description: Varias entidades JPA mapean la misma tabla desde paquetes distintos (entidades sombra) — deliberado, no es violación de DRY.
metadata:
  type: convention
  scope: java
  created: 2026-08-02
---

Hay 17 entidades "sombra" (`*ShadowEntity`, `*RefEntity`) que mapean tablas ya mapeadas por la entidad real de otro módulo. Ejemplos: `jpa.sale.shadow.EmployeeShadowEntity` y `jpa.employee.EmployeeEntity` sobre `employees`; `jpa.employee.AttachFileRefEntity` y `jpa.catalog.AttachFileEntity` sobre `attach_files`.

**Por qué:** los módulos se construyeron en orden fijo (Ventas → Inventario → Catálogo → Empleados → ...). Cuando un módulo temprano necesitaba leer una tabla cuyo módulo dueño aún no existía, creaba una entidad privada mínima con solo las columnas que necesitaba. Conviven sin problema porque `ddl-auto=validate` valida únicamente las columnas que cada entidad declara, no si otra entidad mapea la misma tabla.

**Cómo aplicar:** no reportar estas duplicaciones como violación de DRY. No proponer "unificar en una sola entidad" — acoplaría módulos que están deliberadamente separados. La regla real del repo es distinta y **sí** vale reportarla si se incumple: *una fase posterior debe reutilizar el gateway de dominio real de un módulo ya construido, en vez de crear una sombra nueva*. Una sombra **nueva** de una tabla cuyo módulo dueño **ya existe** sí es un hallazgo legítimo. Ojo con el choque de nombres de bean: dos adaptadores con el mismo nombre de clase simple en paquetes distintos lanzan `ConflictingBeanDefinitionException` — por eso los nombres van prefijados por módulo (`SaleLocationReferenceGatewayAdapter`, `InventoryLocationReferenceGatewayAdapter`).
