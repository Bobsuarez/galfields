# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## What This Is

Custom Caddy reverse proxy for the KindredWorks monorepo. The image is built with `xcaddy` to include two plugins not in the official image:
- `github.com/mholt/caddy-ratelimit` — per-route rate limiting
- `github.com/corazawaf/coraza-caddy` — OWASP WAF (Coraza)

OWASP CRS v4.0.0 rules are downloaded and baked into the image at build time.

## Files

| File | Purpose |
|---|---|
| `Dockerfile.caddy` | Two-stage build: xcaddy compile → runtime with OWASP CRS |
| `Caddyfile` | Production routing (kinforgeworks.com, cdn, n8n) |
| `Caddyfile.dev` | Dev routing (*.localhost with `tls internal`) |
| `compose.yaml` | Production — uses pre-built GHCR image, ports 80/443 |
| `compose.dev.yml` | Dev — builds locally, ports 8080/8443 |
| `compose.build.yml` | Override to force local build in prod compose |

## Local Dev

```bash
# Prerequisites: kinForge-net network must exist
podman network create kinForge-net   # once

# Dev (builds image locally, ports 8080/8443)
podman-compose -f compose.dev.yml up -d --build

# Trust the self-signed cert (once per machine)
podman exec kindredworks-dev-caddy-1 caddy trust

# Validate Caddyfile syntax without restarting
podman exec kindredworks-dev-caddy-1 caddy validate --config /etc/caddy/Caddyfile --adapter caddyfile

# Reload config without downtime (dev)
podman exec kindredworks-dev-caddy-1 caddy reload --config /etc/caddy/Caddyfile --adapter caddyfile
```

`/etc/hosts` entries required for dev:
```
127.0.0.1  web.localhost n8n.localhost leads.localhost leadsprocessor.localhost leadsdelivery.localhost backend.localhost
```

## Production Deploy — Two Workflows

Changes to this directory trigger different pipelines depending on what changed:

| Changed file | Workflow | What happens |
|---|---|---|
| `Caddyfile` | `deploy-caddy-config.yml` | `git pull` → `caddy reload` (zero-downtime, no container restart) |
| `Dockerfile.caddy` | `deploy-caddy-image.yml` | xcaddy rebuild → push to GHCR → `podman-compose up -d --force-recreate` |

Never merge both changes in the same commit if you want to avoid the image rebuild triggering unnecessarily.

## Caddyfile Structure

Prod `Caddyfile` uses reusable snippets defined at the top and imported per-site:

```
(bloquear_ips)       — hardcoded IP blocklist
(bloquear_scanners)  — abort on common exploit paths (wp-admin, .env, .git, etc.)
(rate_limit_login)   — 5 req/min per IP for /api/v1/auth/*
(rate_limit_api)     — 500 req/min per IP for /api/*
(rate_limit_tracking) — 200 req/10min per IP for /api/v1/t/*
(waf)                — Coraza WAF with OWASP CRS
(headers_seguridad)  — full security header set + CSP
```

Route order within `kinforgeworks.com` matters — Caddy matches first `handle` that fits:
1. `/api/v1/t/*` → em-leads:8080 (click tracking, relaxed rate limit)
2. `/api/v1/auth/*` → em-leads:8080 (strict rate limit)
3. `/api/*` → em-leads:8080 (general API)
4. default → em-web:80 (Astro frontend)

## Known Gotchas

**`SecRxPreFilter` patch:** The Dockerfile runs a find+sed to strip all `SecRxPreFilter` directives from the OWASP CRS rule files. This directive is not supported by Coraza and causes startup failure. Do not remove that `RUN grep -rli...` line.

**`coraza.conf` is hand-crafted:** The default `coraza.conf` from the OWASP CRS tarball has a problematic directive on line 41 that breaks Coraza. The Dockerfile creates `coraza.conf` from scratch with only the needed directives. Do not replace it with the bundled one.

**Disabled CRS rules in prod:** `SecRuleRemoveById 920430 920600 922100` are disabled in the `(waf)` snippet because they produce false positives on normal API traffic.

**n8n IP restriction pending:** `n8n.kinforgeworks.com` has `remote_ip 127.0.0.1` as placeholder. Must be replaced with the actual static IP before exposing n8n to traffic.

## Networks

Caddy joins two networks so it can reach all services:
- `caddy_network` — created by this compose, shared with services that need frontend exposure
- `kinForge-net` — monorepo-wide external network (must pre-exist on the host)