#!/bin/sh
set -e

# Backend is co-located; ignore any external BACKEND_ORIGIN
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
