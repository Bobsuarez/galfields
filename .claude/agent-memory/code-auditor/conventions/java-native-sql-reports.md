---
name: java-native-sql-reports
description: jpa.report usa @Query(nativeQuery=true) con proyecciones de interfaz en vez de JPQL — deliberado. Pero siempre verificar que use parámetros vinculados.
metadata:
  type: convention
  scope: java
  created: 2026-08-02
---

Los 4 repositorios de `jpa.report` usan SQL nativo (8 `nativeQuery` en total) con proyecciones basadas en interfaz (`InvoiceSummaryProjection`, `InventoryRowProjection`, etc.) en vez de JPQL.

**Por qué:** consecuencia directa de [[java-flat-jpa-entities]]. Sin relaciones JPA no existe la navegación por path que usaba el `backend/pos` anterior (`t.employee.firstName`, `p.transaction.transactionDate`), así que los joins de los reportes tienen que escribirse en SQL. Y como una query nativa no admite la sintaxis de expresión constructora de JPQL (`SELECT new pkg.Dto(...)`), las proyecciones de interfaz son el equivalente para un ResultSet crudo: los nombres de los getters mapean a los alias de columna del SQL.

**Cómo aplicar:** no reportar el SQL nativo aquí como antipatrón, como "saltarse el ORM", ni proponer convertirlo a JPQL — no es convertible mientras las entidades sean planas.

**Excepción que sí debe reportarse siempre, y es crítica:** verificar que toda query nativa use **parámetros vinculados** (`:from`, `:to`, `:threshold`) y nunca concatenación ni `String.format` para meter valores. Hoy están todas correctas (verificado: sin concatenación en los repositorios). Una query nativa construida por concatenación es inyección SQL y se reporta como crítico sin importar esta memoria.
