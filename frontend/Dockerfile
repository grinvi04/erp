# ── 의존성 스테이지 ────────────────────────────────────────────────────
FROM public.ecr.aws/docker/library/node:26-alpine@sha256:40cd011b55415bc894e45b02fdd5a5dfde056921b7d23a64c74f80497af48797 AS deps
WORKDIR /app
COPY package*.json ./
COPY scripts/apply-braces-depth.mjs ./scripts/apply-braces-depth.mjs
RUN npm ci --ignore-scripts \
    && npm run patch:dependencies \
    && node scripts/apply-braces-depth.mjs --check

# ── 빌드 스테이지 ─────────────────────────────────────────────────────
FROM public.ecr.aws/docker/library/node:26-alpine@sha256:40cd011b55415bc894e45b02fdd5a5dfde056921b7d23a64c74f80497af48797 AS build
WORKDIR /app
ENV NEXT_TELEMETRY_DISABLED=1
COPY --from=deps /app/node_modules ./node_modules
COPY . .
RUN npm run build

# ── 런타임 스테이지 ───────────────────────────────────────────────────
FROM public.ecr.aws/docker/library/node:26-alpine@sha256:40cd011b55415bc894e45b02fdd5a5dfde056921b7d23a64c74f80497af48797 AS run
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
