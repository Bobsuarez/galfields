use rusqlite::Connection;
use std::fs;
use std::path::PathBuf;

pub struct Database {
    pub conn: Connection,
    pub path: PathBuf,
}

impl Database {
    /// Abre (o crea) la base de datos en `data_dir/galfield.db`.
    /// Crea el directorio si no existe, aplica pragmas y corre migraciones.
    pub fn init(data_dir: PathBuf) -> Result<Self, DatabaseError> {
        fs::create_dir_all(&data_dir)
            .map_err(|e| DatabaseError::Io(format!("No se pudo crear el directorio {:?}: {}", data_dir, e)))?;

        let db_path = data_dir.join("galfield.db");
        let conn = Connection::open(&db_path)
            .map_err(|e| DatabaseError::Sqlite(e.to_string()))?;

        let db = Self { conn, path: db_path };
        db.run_migrations()?;

        Ok(db)
    }

    /// Runs each migration file at most once, tracked by filename in
    /// `schema_migrations`. Needed because `001_initial.sql` only uses
    /// idempotent DDL (`CREATE TABLE IF NOT EXISTS`, `INSERT OR IGNORE`) and
    /// re-runs harmlessly on every launch, but later migrations that alter
    /// existing tables (e.g. `ALTER TABLE ... ADD COLUMN`) are not idempotent
    /// and would error on a second run without this ledger.
    fn run_migrations(&self) -> Result<(), DatabaseError> {
        self.conn
            .execute_batch(
                "CREATE TABLE IF NOT EXISTS schema_migrations (
                    version    TEXT NOT NULL UNIQUE,
                    applied_at TEXT NOT NULL DEFAULT (datetime('now', 'localtime'))
                );",
            )
            .map_err(|e| DatabaseError::Sqlite(format!("Error creando schema_migrations: {}", e)))?;

        self.apply_migration("001_initial", include_str!("../../migrations/001_initial.sql"))?;
        self.apply_migration("002_payment_methods", include_str!("../../migrations/002_payment_methods.sql"))?;
        self.apply_migration("003_product_remote_link", include_str!("../../migrations/003_product_remote_link.sql"))?;
        self.apply_migration("004_sales_sync", include_str!("../../migrations/004_sales_sync.sql"))?;
        self.apply_migration("005_payment_method_active", include_str!("../../migrations/005_payment_method_active.sql"))?;
        Ok(())
    }

    fn apply_migration(&self, version: &str, sql: &str) -> Result<(), DatabaseError> {
        let already_applied: bool = self
            .conn
            .query_row(
                "SELECT EXISTS(SELECT 1 FROM schema_migrations WHERE version = ?1)",
                [version],
                |row| row.get(0),
            )
            .map_err(|e| DatabaseError::Sqlite(format!("Error consultando schema_migrations: {}", e)))?;

        if already_applied {
            return Ok(());
        }

        self.conn
            .execute_batch(sql)
            .map_err(|e| DatabaseError::Sqlite(format!("Error en migración {}: {}", version, e)))?;

        self.conn
            .execute("INSERT INTO schema_migrations (version) VALUES (?1)", [version])
            .map_err(|e| DatabaseError::Sqlite(format!("Error registrando migración {}: {}", version, e)))?;

        Ok(())
    }
}

#[derive(Debug)]
pub enum DatabaseError {
    Io(String),
    Sqlite(String),
}

impl std::fmt::Display for DatabaseError {
    fn fmt(&self, f: &mut std::fmt::Formatter<'_>) -> std::fmt::Result {
        match self {
            DatabaseError::Io(msg) => write!(f, "IO error: {}", msg),
            DatabaseError::Sqlite(msg) => write!(f, "SQLite error: {}", msg),
        }
    }
}

impl std::error::Error for DatabaseError {}
