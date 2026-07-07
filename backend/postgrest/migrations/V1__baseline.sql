-- =============================================================================
-- V1 — BASELINE (esquema campaign_engine V4)
-- =============================================================================
-- Este archivo es el marcador de baseline para Flyway.
--
-- EN BASES DE DATOS EXISTENTES: Flyway registra V1 como "ya aplicado" sin
-- ejecutar este SQL (gracias a baselineOnMigrate=true). Solo se aplica V2+.
--
-- EN BASES DE DATOS NUEVAS: el schema lo crea docker-entrypoint-initdb.d
-- (campaign_engine.sql). Flyway luego registra V1 como baseline y aplica V2+.
--
-- REFERENCIA COMPLETA DEL SCHEMA: ../campaign_engine.sql
-- =============================================================================

-- Sentencia válida requerida por Flyway para validar el archivo.
SELECT 1;
