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
 && apt-get install -y --no-install-recommends nginx gettext-base curl \
 && rm -rf /var/lib/apt/lists/* \
 && rm -f /etc/nginx/sites-enabled/default

# Frontend static files
COPY --from=frontend-builder /app/dist /usr/share/nginx/html

# Create a minimal 50x.html so nginx can serve it on backend errors
RUN echo '<html><body><h1>Service Unavailable</h1></body></html>' \
    > /usr/share/nginx/html/50x.html

# Nginx config template (processed at runtime by entrypoint)
COPY frontend/nginx.conf /etc/nginx/conf.d/default.conf.template

# Backend JAR (named app.jar via tasks.bootJar in build.gradle.kts)
COPY --from=backend-builder /app/build/libs/app.jar /app/app.jar

# Entrypoint script
COPY docker-entrypoint.sh /entrypoint.sh
RUN chmod +x /entrypoint.sh

EXPOSE 8080
ENTRYPOINT ["/entrypoint.sh"]
