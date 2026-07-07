# MinIO — KindredWorks Object Storage

Object storage S3-compatible para el monorepo KindredWorks. Usado para almacenar archivos como PDFs, imágenes de campañas y cualquier asset binario que los servicios necesiten compartir.

## Endpoints

| Puerto | Servicio |
|---|---|
| `9000` | S3 API — usado por servicios internos (`eu-west-1` como región, o sin región) |
| `9001` | Consola web MinIO (UI de administración) |

## Levantar

### Desarrollo local

```bash
podman-compose -f compose.yaml -f compose.dev.yml up -d
```

Acceso local:
- S3 API: `http://localhost:9000`
- Consola: `http://localhost:9001`

### Producción

```bash
podman-compose up -d
```

En producción no se exponen puertos al host. Caddy hace proxy a la consola si está configurado en el `Caddyfile`.

## Credenciales

Configuradas con variables de entorno:

```env
MINIO_ACCESS_KEY_VARIABLE_GIT=minioadmin
MINIO_SECRET_KEY_SECRET_GIT=minioadmin
```

Los valores por defecto solo sirven para desarrollo local. En producción se configuran como secrets en el servidor.

## Redes

- `kinForge-net` — red interna del monorepo (para que los servicios Java/Python/Node accedan al S3 API)
- `caddy_network` — para acceso desde Caddy a la consola web

## Datos

Los datos se persisten en el volumen Docker/Podman `minio_data`. Para hacer backup del volumen:

```bash
podman volume export minio_data | gzip > minio_backup_$(date +%Y%m%d).tar.gz
```

## Conexión desde otros servicios

Desde cualquier contenedor en `kinForge-net`, el endpoint S3 es:

```
http://minio:9000
```

Usar las mismas credenciales (`MINIO_ACCESS_KEY_VARIABLE_GIT` / `MINIO_SECRET_KEY_SECRET_GIT`) como Access Key y Secret Key en el cliente S3.
