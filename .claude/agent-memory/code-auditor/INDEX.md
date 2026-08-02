# Memoria de `code-auditor` — Galfields

Índice de lo que el auditor ya sabe de este repositorio. Una línea por memoria, nunca el contenido.
El agente lee este archivo al inicio de cada auditoría y carga las entradas relevantes al alcance.

Tipos: `convention` (decisión deliberada — suprimir hallazgos que la toquen) · `dismissed` (falso positivo ya rechazado por el usuario) · `pattern` (problema real recurrente — subir sensibilidad) · `risk` (riesgo conocido y aceptado — reportar solo bajo las condiciones que indique la entrada).

## Convenciones

- [Entidades JPA planas, sin relaciones](conventions/java-flat-jpa-entities.md) — cero `@ManyToOne`/`@OneToMany` a propósito; no es modelo anémico
- [Entidades sombra sobre la misma tabla](conventions/java-shadow-entities.md) — 17 duplicados deliberados; no es violación de DRY
- [SQL nativo en los reportes](conventions/java-native-sql-reports.md) — deliberado; pero siempre verificar parámetros vinculados
- [Ubicación por defecto hardcodeada](conventions/java-default-location-hardcoded.md) — negocio de una sola sede; no es constante mágica

## Riesgos conocidos

- [Credencial admin sembrada en V9](risks/java-seeded-admin-credentials.md) — documentada; reportar solo si sigue sin rotar en producción
