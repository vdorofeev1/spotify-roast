# Combined Dockerfile Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Create a single `Dockerfile` at the repo root that builds and runs both the React frontend (nginx) and Spring Boot backend (JVM) in one container, targeting Google Cloud Run.

**Architecture:** 3-stage multi-stage build — `frontend-builder` (Node 20) produces static assets, `backend-builder` (JDK 17) produces `app.jar`, `runtime` (JRE 17 + nginx) runs both. A shell script entrypoint starts Spring Boot on internal port `9090`, then execs nginx on `${PORT}` (Cloud Run default: `8080`). nginx proxies `/api/`, `/login/`, `/oauth2/`, `/logout` to `localhost:9090`.

**Tech Stack:** Docker multi-stage build, Eclipse Temurin 17 JRE (Ubuntu-based), nginx, gettext-base (envsubst), Node 20 Alpine, Gradle 17

---

## File Map

| Action | Path | Responsibility |
|---|---|---|
| Create | `docker-entrypoint.sh` | Start JVM in background, exec nginx in foreground |
| Create | `Dockerfile` | 3-stage build: frontend → backend → runtime |
| Modify | `Makefile` | Add `build-combined` and `run-combined` targets |

---

### Task 1: Create docker-entrypoint.sh

**Files:**
- Create: `docker-entrypoint.sh`

- [ ] **Step 1: Write the entrypoint script**

Create `/Users/valentindorofeev/workspace/projects/spotify-roast/docker-entrypoint.sh` with this exact content:

```sh
#!/bin/sh
set -e

# Backend is always co-located; ignore any external BACKEND_ORIGIN
export BACKEND_ORIGIN="http://localhost:9090"
export PORT="${PORT:-8080}"

# Substitute only our two variables — leave nginx's $host, $remote_addr etc. untouched
envsubst '${PORT} ${BACKEND_ORIGIN}' \
  < /etc/nginx/templates/default.conf.template \
  > /etc/nginx/conf.d/default.conf

# Frontend reads this at runtime to know the backend URL.
# Empty string = same origin (relative paths). See frontend/src/config.js.
cat > /usr/share/nginx/html/runtime-config.js <<'JSEOF'
window.__SPOTIFY_ROAST_CONFIG__ = {
  BACKEND_URL: ""
};
JSEOF

# Start Spring Boot on internal port 9090 in the background
java \
  -Dspring.profiles.active=prod \
  -Dserver.port=9090 \
  -jar /app/app.jar &

# Hand off to nginx in the foreground (PID 1 after exec)
exec nginx -g "daemon off;"
```

- [ ] **Step 2: Verify the script has no syntax errors**

```bash
sh -n docker-entrypoint.sh
```

Expected output: nothing (no errors).

- [ ] **Step 3: Commit**

```bash
git add docker-entrypoint.sh
git commit -m "feat: add combined container entrypoint script"
```

---

### Task 2: Create Dockerfile at repo root

**Files:**
- Create: `Dockerfile`

- [ ] **Step 1: Write the Dockerfile**

Create `/Users/valentindorofeev/workspace/projects/spotify-roast/Dockerfile` with this exact content:

```dockerfile
# ── Stage 1: Build frontend ──────────────────────────────────────────────────
FROM node:20-alpine AS frontend-builder
WORKDIR /app

COPY frontend/package*.json ./
RUN npm ci

COPY frontend/ .
RUN npm run build

# ── Stage 2: Build backend ───────────────────────────────────────────────────
FROM eclipse-temurin:17-jdk AS backend-builder
WORKDIR /app

COPY backend/gradlew backend/gradlew.bat ./
COPY backend/gradle/ gradle/
COPY backend/build.gradle.kts backend/settings.gradle.kts ./
RUN ./gradlew dependencies --no-daemon

COPY backend/src/ src/
RUN ./gradlew build -x test --no-daemon

# ── Stage 3: Runtime ─────────────────────────────────────────────────────────
FROM eclipse-temurin:17-jre

# Install nginx and envsubst (from gettext-base)
RUN apt-get update \
 && apt-get install -y --no-install-recommends nginx gettext-base \
 && rm -rf /var/lib/apt/lists/* \
 && rm -f /etc/nginx/sites-enabled/default

# Frontend static files
COPY --from=frontend-builder /app/dist /usr/share/nginx/html

# Nginx config template (processed at runtime by entrypoint)
COPY frontend/nginx.conf /etc/nginx/templates/default.conf.template

# Backend JAR (named app.jar via tasks.bootJar in build.gradle.kts)
COPY --from=backend-builder /app/build/libs/app.jar /app/app.jar

# Entrypoint script
COPY docker-entrypoint.sh /entrypoint.sh
RUN chmod +x /entrypoint.sh

EXPOSE 8080
ENTRYPOINT ["/entrypoint.sh"]
```

- [ ] **Step 2: Commit**

```bash
git add Dockerfile
git commit -m "feat: add combined frontend+backend Dockerfile"
```

---

### Task 3: Build and smoke-test the combined image locally

**Files:** none (verification only)

Prerequisites: Docker running locally, `.env` file present at repo root with all required env vars.

- [ ] **Step 1: Build the image from repo root**

```bash
docker build -t spotify-roast-combined .
```

Expected: build completes with no errors. The build will take several minutes the first time (Gradle dependency resolution + npm ci). Watch for:
- `frontend-builder`: `npm run build` should end with `dist/` output summary
- `backend-builder`: `BUILD SUCCESSFUL` from Gradle
- `runtime`: layer for apt-get, COPY steps, chmod

- [ ] **Step 2: Run the container with env vars from .env**

```bash
docker run --rm -p 8080:8080 \
  --env-file .env \
  -e PORT=8080 \
  spotify-roast-combined
```

Expected output (interleaved, order may vary):
```
... nginx: starting...
... Spring Boot banner (Spotify Roast)
... Started SpotifyRoastApplication in X seconds
```

- [ ] **Step 3: Smoke-test nginx serves the frontend**

In a new terminal:

```bash
curl -s -o /dev/null -w "%{http_code}" http://localhost:8080/
```

Expected: `200`

- [ ] **Step 4: Smoke-test the backend ping endpoint via nginx**

```bash
curl -s http://localhost:8080/api/ping
```

Expected: `pong` (or whatever `/api/ping` returns — check `RoastController.kt`)

- [ ] **Step 5: Verify runtime-config.js is served correctly**

```bash
curl -s http://localhost:8080/runtime-config.js
```

Expected:
```js
window.__SPOTIFY_ROAST_CONFIG__ = {
  BACKEND_URL: ""
};
```

- [ ] **Step 6: Stop the container**

Press `Ctrl+C` in the terminal running the container.

- [ ] **Step 7: Commit a note if any fixes were needed**

If you had to adjust `Dockerfile` or `docker-entrypoint.sh` to get the smoke tests passing, commit those fixes:

```bash
git add Dockerfile docker-entrypoint.sh
git commit -m "fix: correct combined container build/runtime issues"
```

If no fixes were needed, skip this step.

---

### Task 4: Add Makefile targets for the combined image

**Files:**
- Modify: `Makefile`

- [ ] **Step 1: Read the current Makefile**

Open `Makefile` and confirm the existing targets (up, down, db, frontend, run-backend, run-frontend, dev).

- [ ] **Step 2: Add combined build and run targets**

Add these lines at the end of `Makefile` (after the existing `dev` target):

```makefile
build-combined:
	docker build -t spotify-roast-combined .

run-combined:
	docker run --rm -p 8080:8080 --env-file .env -e PORT=8080 spotify-roast-combined
```

- [ ] **Step 3: Verify the Makefile parses correctly**

```bash
make --dry-run build-combined
```

Expected:
```
docker build -t spotify-roast-combined .
```

- [ ] **Step 4: Commit**

```bash
git add Makefile
git commit -m "chore: add make targets for combined container build and run"
```

---

## Cloud Run Migration Notes (manual steps, not in this plan)

After the Dockerfile is merged and working:

1. Update the Cloud Run "Continuously deploy from a repository" trigger to use the repo root `Dockerfile`
2. Set all env vars in Cloud Run (see spec `SPOTIFY_CLIENT_ID`, `ENCRYPTION_KEY`, etc.)
3. Update `FRONTEND_URL` and `ALLOWED_ORIGINS` to the new single Cloud Run service URL
4. Update the Spotify Developer Dashboard → your app → Redirect URIs to the new URL: `https://<new-service-url>/login/oauth2/code/spotify`
5. Decommission the old separate `pet-project-frontend` Cloud Run service
