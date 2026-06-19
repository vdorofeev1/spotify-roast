#!/bin/sh
set -e

# Backend is co-located; ignore any external BACKEND_ORIGIN
export BACKEND_ORIGIN="http://localhost:9090"
export PORT="${PORT:-8080}"

# Substitute only our two variables — leave nginx's $host, $remote_addr etc. untouched
envsubst '${PORT} ${BACKEND_ORIGIN}' \
  < /etc/nginx/conf.d/default.conf.template \
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
  -Dserver.port=9090 \
  -jar /app/app.jar &

# Wait for Spring Boot to be ready before accepting traffic via nginx
echo "Waiting for backend to be ready..."
until curl -sf http://localhost:9090/api/ping > /dev/null 2>&1; do sleep 1; done
echo "Backend ready."

# Hand off to nginx in the foreground (PID 1 after exec)
exec nginx -g "daemon off;"
