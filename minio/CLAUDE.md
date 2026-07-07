# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Propósito

Módulo de object storage S3-compatible para el monorepo KindredWorks. Expone dos endpoints:
- Puerto **9000** — S3 API (usado por servicios internos)
- Puerto **9001** — Consola web MinIO

## Compose: Dev vs Prod

| Archivo | Propósito |
|---|---|
| `compose.yaml` | Producción — sin puertos expuestos al host |
| `compose.dev.yml` | Overlay dev — expone 9000 y 9001 al host |

```bash
# Dev
podman-compose -f compose.yaml -f compose.dev.yml up -d

# Prod
podman-compose up -d
```

## Variables de Entorno

| Variable | Default | Descripción |
|---|---|---|
| `MINIO_ACCESS_KEY_VARIABLE_GIT` | `minioadmin` | Root user (Access Key) |
| `MINIO_SECRET_KEY_SECRET_GIT` | `minioadmin` | Root password (Secret Key) |

En producción, estas variables deben estar en el entorno del servidor o en un `.env`. Los defaults solo sirven para desarrollo local.

## Redes

El contenedor está en **ambas** redes externas:
- `kinForge-net` — comunicación interna entre servicios del monorepo
- `caddy_network` — para que Caddy pueda hacer proxy a la consola web

## Recursos

Límites configurados en `compose.yaml`:
- CPU: 0.50 cores (reserva 0.25)
- RAM: 512 MB (reserva 256 MB)

Datos persistidos en el volumen `minio_data`.
