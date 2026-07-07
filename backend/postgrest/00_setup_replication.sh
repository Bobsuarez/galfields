#!/bin/bash
# Script de inicialización del primario para streaming replication.
# Se ejecuta automáticamente en el primer arranque, antes que los .sql de initdb.d.
set -e

# 1. Crear usuario dedicado a replicación
psql -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" --dbname postgres <<-EOSQL
    CREATE USER replicator WITH REPLICATION ENCRYPTED PASSWORD 'repl_secret';
EOSQL

# 2. Agregar configuración WAL al postgresql.conf generado por initdb
cat >> "$PGDATA/postgresql.conf" <<-EOF

# --- Streaming replication ---
wal_level            = replica
max_wal_senders      = 5
wal_keep_size        = 1GB
EOF

# 3. Permitir conexiones de replicación desde cualquier IP de la red interna
echo "host    replication     replicator      0.0.0.0/0       md5" >> "$PGDATA/pg_hba.conf"
