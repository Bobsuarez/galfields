# Caddy — KindredWorks Reverse Proxy

Custom Caddy build that serves as the single entry point for all KindredWorks services. Built with `xcaddy` to include the Coraza WAF and rate-limiting plugins; OWASP CRS v4.0.0 rules are baked into the image.

## Architecture

```
Internet → Caddy (ports 80/443)
             ├── kinforgeworks.com/api/*     → em-leads:8080
             ├── kinforgeworks.com/*         → em-web:80
             ├── cdn.kinforgeworks.com       → minio:9000
             └── n8n.kinforgeworks.com       → n8n:5678
```

Caddy sits in two networks: `caddy_network` (internal frontend traffic) and `kinForge-net` (monorepo-wide service mesh). No other service exposes ports to the host in production.

## Security Layers

Every public site goes through, in order:
1. **IP blocklist** — hardcoded known-bad IPs (add to `bloquear_ips` snippet)
2. **Scanner abort** — closes connection on exploit paths without response
3. **Security headers** — HSTS, CSP, X-Frame-Options, Referrer-Policy, no-cache
4. **WAF** — Coraza + OWASP CRS (SQLi, XSS, etc.)
5. **Rate limits** — different zones per route type (login: 5/min, API: 500/min, tracking: 200/10min)

## Running Locally

```bash
# 1. Create shared network (once)
podman network create kinForge-net

# 2. Start Caddy (builds image with xcaddy + OWASP CRS)
podman-compose -f compose.dev.yml up -d --build

# 3. Trust the self-signed dev cert (once per machine)
podman exec kindredworks-dev-caddy-1 caddy trust
```

Add to `/etc/hosts`:
```
127.0.0.1  web.localhost n8n.localhost leads.localhost leadsprocessor.localhost leadsdelivery.localhost
```

Services are then available at:
- https://web.localhost:8443 — Frontend (Astro)
- https://leads.localhost:8443 — Leads API
- https://leadsprocessor.localhost:8443 — Leads Processor API
- https://leadsdelivery.localhost:8443 — Leads Delivery API
- https://n8n.localhost:8443 — n8n automation

## Deploying Changes

### Caddyfile only (routes, rate limits, headers)
Push to `master`. The `deploy-caddy-config.yml` workflow reloads the running config with zero downtime — no container restart, no dropped connections.

### Dockerfile.caddy (new plugin, updated OWASP CRS)
Push to `master`. The `deploy-caddy-image.yml` workflow:
1. Builds the new image with `xcaddy`
2. Pushes it to GHCR (`ghcr.io/bobsuarez/kindredworks/caddy:<sha>`)
3. SSHes into the VPS and recreates the container (~2s downtime)

### Force local build in production
```bash
podman-compose -f compose.yaml -f compose.build.yml up -d --build
```

## Pending

- Replace `remote_ip 127.0.0.1` in `n8n.kinforgeworks.com` with the real static IP