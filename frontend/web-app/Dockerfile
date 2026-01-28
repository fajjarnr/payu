####
# Base Image - Red Hat UBI9 Node.js 20
####
FROM registry.access.redhat.com/ubi9/nodejs-20:1-20 AS base

# Metadata labels
LABEL maintainer="frontend-team@payu.id"
LABEL description="PayU Web App - Digital Banking Next.js Frontend"
LABEL version="1.0.0"
LABEL org.opencontainers.image.title="payu-web-app"
LABEL org.opencontainers.image.description="PayU Digital Banking Web Application - Next.js 15 with React 18, TypeScript, and Tailwind CSS"
LABEL org.opencontainers.image.version="1.0.0"
LABEL org.opencontainers.image.vendor="PayU Indonesia"
LABEL org.opencontainers.image.url="https://payu.id"
LABEL org.opencontainers.image.source="https://github.com/payu/frontend/web-app"
LABEL org.opencontainers.image.licenses="Proprietary"
LABEL org.opencontainers.image.authors="PayU Frontend Team"

####
# Stage 1: Dependencies
####
FROM base AS deps
USER root
WORKDIR /app

# Install build tools for native modules
RUN microdnf install -y python3 make gcc-c++ && microdnf clean all

# Copy package files
COPY package.json package-lock.json* ./

# Install dependencies with npm ci for reliable, reproducible builds
RUN npm ci --ignore-scripts

####
# Stage 2: Builder
####
FROM base AS builder
WORKDIR /app

# Copy dependencies from deps stage
COPY --from=deps /app/node_modules ./node_modules

# Copy source code
COPY . .

# Disable Next.js telemetry
ENV NEXT_TELEMETRY_DISABLED=1

# Build Next.js application with standalone output for minimal container size
RUN npm run build

####
# Stage 3: Runner (Production)
####
FROM registry.access.redhat.com/ubi9/nodejs-20-minimal:1-20 AS runner

WORKDIR /app

# Environment variables
ENV NODE_ENV=production
ENV NEXT_TELEMETRY_DISABLED=1
ENV PORT=3000
ENV HOSTNAME="0.0.0.0"

# Create non-root user directory and set permissions
USER root

# Copy public assets (static files)
COPY --from=builder /app/public ./public

# Create .next directory with proper permissions for OpenShift
RUN mkdir -p .next && chown -R 1001:0 .next

# Copy standalone output (optimized for container deployment)
COPY --from=builder --chown=1001:0 /app/.next/standalone ./
COPY --from=builder --chown=1001:0 /app/.next/static ./.next/static

# Switch to non-root user (1001 is the default node user in UBI9)
USER 1001

# Expose the application port
EXPOSE 3000

# Health check for Next.js application
HEALTHCHECK --interval=30s --timeout=5s --start-period=40s --retries=3 \
    CMD curl -f http://localhost:3000/api/health || exit 1

# Run the production server
CMD ["node", "server.js"]
