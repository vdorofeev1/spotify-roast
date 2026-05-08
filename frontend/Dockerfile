# Stage 1: Build
FROM node:20-alpine AS builder
WORKDIR /app

COPY package*.json ./
RUN npm ci

COPY . .

RUN npm run build

# Stage 2: Serve with Nginx
FROM nginx:stable-alpine

COPY --from=builder /app/dist /usr/share/nginx/html

# Copy custom nginx config template for envsubst to handle runtime config.
COPY nginx.conf /etc/nginx/templates/default.conf.template
COPY docker-entrypoint.d/05-runtime-config.envsh /docker-entrypoint.d/05-runtime-config.envsh

EXPOSE 3000
CMD ["nginx", "-g", "daemon off;"]
