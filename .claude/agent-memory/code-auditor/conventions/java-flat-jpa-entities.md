---
name: java-flat-jpa-entities
description: Las entidades JPA usan FKs planas (Long xId) en vez de @ManyToOne/@OneToMany — decisión deliberada, no modelo anémico.
metadata:
  type: convention
  scope: java
  created: 2026-08-02
---

Ninguna entidad de `backend/pos_transactions` usa relaciones JPA. Cero `@ManyToOne`, `@OneToMany` o `@ManyToMany` en todo el módulo (verificado). Cada entidad declara la FK como una columna plana: `@Column(name = "variant_id") private Long variantId;`.

**Por qué:** decisión explícita tomada durante la migración a Clean Architecture (spec `04-migracion-pos-clean-architecture`, Fase 2). El `backend/pos` anterior sí usaba relaciones y chocó repetidamente con dos problemas de Hibernate: el orden de flush (un `deleteAll()` seguido de `save()` insertaba antes de borrar y reventaba constraints únicos) y la complejidad de `orphanRemoval`. Las entidades planas eliminan esa clase entera de bugs a cambio de resolver los joins a mano en el adaptador.

**Cómo aplicar:** no reportar la ausencia de relaciones JPA como modelo anémico, mal diseño de dominio, ni violación de SOLID. Tampoco reportar como problema que un adaptador haga varias llamadas al repositorio para armar un agregado — es la consecuencia esperada de esta decisión. Ver [[java-native-sql-reports]], que es la otra consecuencia directa. Sí sigue siendo válido reportar un N+1 real y medible dentro de un bucle caliente.
