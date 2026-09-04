# ── 의존성 스테이지 ────────────────────────────────────────────────────
FROM public.ecr.aws/docker/library/node:22-alpine@sha256:16e22a550f3863206a3f701448c45f7912c6896a62de43add43bb9c86130c3e2 AS deps
WORKDIR /app
COPY package*.json ./
RUN npm ci --ignore-scripts

# ── 빌드 스테이지 ─────────────────────────────────────────────────────
FROM public.ecr.aws/docker/library/node:22-alpine@sha256:16e22a550f3863206a3f701448c45f7912c6896a62de43add43bb9c86130c3e2 AS build
WORKDIR /app
ENV NEXT_TELEMETRY_DISABLED=1
COPY --from=deps /app/node_modules ./node_modules
COPY . .
RUN npm run build

# ── 런타임 스테이지 ───────────────────────────────────────────────────
FROM public.ecr.aws/docker/library/node:22-alpine@sha256:16e22a550f3863206a3f701448c45f7912c6896a62de43add43bb9c86130c3e2 AS run
WORKDIR /app
ENV NODE_ENV=production
ENV NEXT_TELEMETRY_DISABLED=1

COPY --chown=node:node --from=build /app/.next/standalone ./
COPY --chown=node:node --from=build /app/.next/static ./.next/static
COPY --chown=node:node --from=build /app/public ./public
RUN rm -rf node_modules/sharp node_modules/@img/sharp-* \
    && test ! -e node_modules/sharp \
    && ! find node_modules/@img -maxdepth 1 -name 'sharp-*' -print -quit | grep -q .

EXPOSE 3000
ENV PORT=3000
ENV HOSTNAME=0.0.0.0
USER node
CMD ["node", "server.js"]
