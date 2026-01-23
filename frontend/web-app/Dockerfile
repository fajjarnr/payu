
# Stage 1: Base Image (UBI9 + Node.js 20)
# -----------------------------------------------------------------------------
FROM registry.access.redhat.com/ubi9/nodejs-20:latest AS base

# Switch to root to install dependencies if needed (UBI images default to 'default' user)
USER root
WORKDIR /app

# -----------------------------------------------------------------------------
# Stage 2: Install Dependencies (Deps)
# -----------------------------------------------------------------------------
FROM base AS deps
# Install libc6-compat if needed OR python3/make/g++ for native modules
# UBI9 usually has glibc, but some build tools might be needed
RUN dnf install -y python3 make gcc-c++ && dnf clean all

WORKDIR /app

# Install dependencies based on the preferred package manager
COPY package.json package-lock.json* ./
RUN npm ci

# -----------------------------------------------------------------------------
# Stage 3: Builder
# -----------------------------------------------------------------------------
FROM base AS builder
WORKDIR /app
COPY --from=deps /app/node_modules ./node_modules
COPY . .

# Next.js collects completely anonymous telemetry data about general usage.
ENV NEXT_TELEMETRY_DISABLED=1

RUN npm run build

# Stage 4: Runner (Production Image)
# -----------------------------------------------------------------------------
FROM registry.access.redhat.com/ubi9/nodejs-20-minimal:latest AS runner
WORKDIR /app

ENV NODE_ENV=production
ENV NEXT_TELEMETRY_DISABLED=1
ENV PORT=3000

# Create a non-root user (1001) for security if not present
# UBI9 images often come with a default user "default" (1001), but explicit creation is safer if missing
# However, UBI9 Node.js minimal image runs as 1001 by default.
# We will ensure permissions are correct.

USER root

# Copy public assets
COPY --from=builder /app/public ./public

# Set the correct permission for prerender cache
RUN mkdir .next
RUN chown -R 1001:0 .next

# Automatically leverage output traces to reduce image size
COPY --from=builder --chown=1001:0 /app/.next/standalone ./
COPY --from=builder --chown=1001:0 /app/.next/static ./.next/static

# Switch to non-root user (1001 is standard in UBI/OpenShift)
USER 1001

EXPOSE 3000

# server.js is created by next build from the standalone output
CMD ["node", "server.js"]
