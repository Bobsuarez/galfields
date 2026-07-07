# Migraciones de base de datos — Flyway

Flyway aplica automáticamente los cambios de schema en cada deploy sin bajar la base de datos.

## Cómo crear una nueva migración

1. Crea un archivo con el nombre: `V{siguiente_número}__{descripción_snake_case}.sql`

```
V1__baseline.sql          ← ya existe (baseline)
V2__add_user_last_login.sql
V3__add_lead_priority_column.sql
V4__create_notifications_table.sql
```

2. Escribe el SQL del cambio:

```sql
-- V2__add_user_last_login.sql
ALTER TABLE app_users ADD COLUMN IF NOT EXISTS last_login_at TIMESTAMP;
```

3. Haz commit y push a `master`. El workflow `deploy-em-db.yml` aplica la migración automáticamente.

## Reglas

| Regla | Por qué |
|---|---|
| Nunca modificar un archivo ya aplicado (`V1`, `V2`...) | Flyway valida el checksum y falla si cambia |
| Usar `IF NOT EXISTS` / `IF EXISTS` cuando sea posible | Hace el SQL idempotente y más seguro |
| Un cambio por archivo | Facilita rollback y revisión |
| Nombres descriptivos en snake_case | `V3__add_lead_priority_column` es legible en el historial |

## Ver estado de migraciones en producción

```bash
# En el VPS
cd /home/bob/projects/KindredWorks
export DB_NAME=campaign_engine DB_USERNAME=postgres DB_PASSWORD=tu_pass

podman run --rm \
  --network=kinForge-net \
  -e FLYWAY_URL="jdbc:postgresql://em-db-primary:5432/${DB_NAME}" \
  -e FLYWAY_USER="${DB_USERNAME}" \
  -e FLYWAY_PASSWORD="${DB_PASSWORD}" \
  -e FLYWAY_BASELINE_ON_MIGRATE=true \
  -e FLYWAY_BASELINE_VERSION=1 \
  docker.io/flyway/flyway:10-alpine info
```

Salida ejemplo:
```
+-----------+---------+---------------------+------+---------------------+---------+
| Category  | Version | Description         | Type | Installed On        | State   |
+-----------+---------+---------------------+------+---------------------+---------+
| Versioned | 1       | baseline            | SQL  |                     | Baseline|
| Versioned | 2       | add user last login | SQL  | 2026-05-10 14:32:01 | Success |
| Versioned | 3       | add lead priority   | SQL  | 2026-05-12 09:15:44 | Success |
+-----------+---------+---------------------+------+---------------------+---------+
```

## Rollback manual (si una migración falla)

Flyway no hace rollback automático (los SQL de rollback en Postgres son complicados).
Si una migración falla a mitad, el procedimiento es:

```sql
-- 1. Conectarse a la BD
psql -h localhost -p 5433 -U postgres -d campaign_engine

-- 2. Ver migraciones fallidas
SELECT * FROM flyway_schema_history ORDER BY installed_rank;

-- 3. Si el estado es 'FAILED', corregir el SQL y borrar el registro fallido
DELETE FROM flyway_schema_history WHERE success = false;

-- 4. Aplicar de nuevo desde el deploy
```
