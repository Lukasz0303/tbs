ARG NODE_VERSION=22-alpine
ARG NGINX_VERSION=1.27-alpine

FROM node:${NODE_VERSION} AS builder

LABEL maintainer="tbswars"
LABEL description="Frontend build stage for World at War: Turn-Based Strategy"

ARG SUPABASE_URL
ARG SUPABASE_ANON_KEY
ARG API_BASE_URL

ENV NODE_ENV=production
ENV SUPABASE_URL=${SUPABASE_URL}
ENV SUPABASE_ANON_KEY=${SUPABASE_ANON_KEY}
ENV API_BASE_URL=${API_BASE_URL}

WORKDIR /app

COPY package*.json ./

RUN npm ci --include=dev && \
    npm cache clean --force

COPY . .

RUN npm run build

FROM nginx:${NGINX_VERSION} AS runtime

LABEL maintainer="tbswars"
LABEL description="Frontend runtime stage for World at War: Turn-Based Strategy"

RUN chown -R nginx:nginx /var/cache/nginx && \
    chown -R nginx:nginx /var/log/nginx && \
    chown -R nginx:nginx /etc/nginx/conf.d && \
    touch /var/run/nginx.pid && \
    chown -R nginx:nginx /var/run/nginx.pid

WORKDIR /usr/share/nginx/html

COPY --from=builder --chown=nginx:nginx /app/dist/frontend/browser .

COPY --chown=nginx:nginx nginx.conf /etc/nginx/conf.d/default.conf

USER nginx

EXPOSE 4222

HEALTHCHECK --interval=30s --timeout=3s --start-period=5s --retries=3 \
    CMD wget --no-verbose --tries=1 --spider http://localhost:4222/health || exit 1

CMD ["nginx", "-g", "daemon off;"]

