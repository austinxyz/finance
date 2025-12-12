# Multi-stage build for Vue.js frontend

# Stage 1: Build stage
FROM node:18-alpine AS build
WORKDIR /app

# Copy package files and install dependencies (cache this layer)
COPY package*.json ./
RUN npm ci --only=production=false

# Copy source code and build
COPY . .
RUN npm run build

# Stage 2: Runtime stage with nginx
FROM nginx:1.25-alpine
WORKDIR /usr/share/nginx/html

# Remove default nginx static files
RUN rm -rf ./*

# Copy built files from build stage
COPY --from=build /app/dist .

# Copy custom nginx configuration
COPY nginx.conf /etc/nginx/conf.d/default.conf

# Expose port 80
EXPOSE 80

# Health check
HEALTHCHECK --interval=30s --timeout=3s --start-period=10s --retries=3 \
  CMD wget --no-verbose --tries=1 --spider http://localhost/ || exit 1

# Start nginx
CMD ["nginx", "-g", "daemon off;"]
