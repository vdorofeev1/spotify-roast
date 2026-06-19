# Combined Dockerfile Design

**Date:** 2026-06-19
**Goal:** Replace two separate Cloud Run services (frontend + backend) with a single Cloud Run service running both nginx and Spring Boot in one container.

---

## Context

Currently the project deploys as two Cloud Run services:
- `pet-project-frontend` — nginx serving static React build, proxying API calls to the backend service
- `pet-project` (backend) — Spring Boot JAR

The goal is a single Cloud Run service with one URL, one deployment pipeline, and one Dockerfile at the repo root.

---

## Architecture

A 3-stage multi-stage `Dockerfile` at the repo root:

| Stage | Base image | Purpose | Output |
|---|---|---|---|
| `frontend-builder` | `node:20-alpine` | `npm ci && npm run build` in `frontend/` | `/app/dist` |
| `backend-builder` | `eclipse-temurin:17-jdk` | `./gradlew build -x test` in `backend/` | `build/libs/app.jar` |
| `runtime` | `eclipse-temurin:17-jre` | nginx + JRE, runs both processes | serves on `${PORT}` |

---

## Port Strategy

Cloud Run injects a `PORT` env var (default `8080`) — the container must listen on this port. Spring Boot also defaults to `8080`, so they must be separated:

- **nginx** listens on `${PORT}` (externally visible, set by Cloud Run)
- **Spring Boot** listens on `9090` (internal only, never exposed)
- nginx proxies `/api/`, `/login/`, `/oauth2/`, `/logout` → `http://localhost:9090`
- `BACKEND_ORIGIN` in `nginx.conf` is hardcoded to `http://localhost:9090` in the entrypoint script — it is no longer a runtime env var

---

## Process Management

Shell script entrypoint (`docker-entrypoint.sh` at repo root):

1. Run `envsubst` on `frontend/nginx.conf` template → `/etc/nginx/conf.d/default.conf` (substitutes `${PORT}` and `${BACKEND_ORIGIN}`)
2. Write `/usr/share/nginx/html/runtime-config.js` with `BACKEND_URL: ""` so the frontend uses relative paths (same origin)
3. Start Spring Boot in background: `java -Dspring.profiles.active=prod -Dserver.port=9090 -jar /app/app.jar &`
4. Exec nginx in foreground: `exec nginx -g "daemon off;"`

If Spring Boot crashes, Cloud Run's health check detects an unhealthy container and restarts it. No per-process supervisor is needed.

---

## Frontend Config

`frontend/src/config.js` already handles `BACKEND_URL: ""` correctly — `buildBackendUrl` returns relative paths (e.g., `/api/roast`), which resolve to the same origin. No frontend code changes required.

---

## Secrets & Environment Variables

No secrets are baked into the image. All values are injected at Cloud Run deploy time:

| Variable | Purpose |
|---|---|
| `SPOTIFY_CLIENT_ID` | Spotify OAuth2 client |
| `SPOTIFY_CLIENT_SECRET` | Spotify OAuth2 secret |
| `LLM_API_KEY` | Gemini API key |
| `LLM_MODEL` | Gemini model name |
| `ENCRYPTION_KEY` | AES-256-GCM key for token encryption at rest |
| `FRONTEND_URL` | Post-login redirect URL (single Cloud Run service URL) |
| `ALLOWED_ORIGINS` | CORS allowed origins |
| `DB_URL` | PostgreSQL JDBC URL |
| `DB_USER` | DB username |
| `DB_PASSWORD` | DB password |

`.env` is never copied into the image. Spring Boot reads env vars directly in the `prod` profile.

---

## Files Changed

**New files:**
- `Dockerfile` (repo root) — 3-stage combined build
- `docker-entrypoint.sh` (repo root) — process entrypoint

**Unchanged files:**
- `frontend/nginx.conf` — reused as nginx template
- `frontend/Dockerfile` — kept for standalone frontend builds
- `backend/Dockerfile` — kept for standalone backend builds
- `frontend/src/config.js` — no changes needed
- `docker-compose.yml` — local dev flow unchanged

---

## Cloud Run Migration Notes

- Go from 2 Cloud Run services → 1 service
- New service exposes port `8080` (Cloud Run default)
- Update Spotify OAuth2 callback URL in Spotify Developer Dashboard to the new single service URL
- Update `FRONTEND_URL` and `ALLOWED_ORIGINS` env vars to the new single service URL
- The "Continuously deploy from a repository" trigger should point to the repo root and use `Dockerfile` (default)
