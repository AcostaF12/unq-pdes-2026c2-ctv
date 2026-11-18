FROM node:24.22-alpine AS build
WORKDIR /app
COPY package.json package-lock.json ./
RUN CYPRESS_INSTALL_BINARY=0 npm ci
# Declared after npm ci so a different API URL keeps the dependency layer cached.
ARG VITE_API_BASE_URL
ENV VITE_API_BASE_URL=$VITE_API_BASE_URL
COPY . .
RUN npm run build

# nginx-unprivileged runs as the nginx user (uid 101) and listens on 8080.
FROM nginxinc/nginx-unprivileged:1.31-alpine AS runtime
COPY --from=build /app/dist /usr/share/nginx/html
COPY nginx.conf /etc/nginx/conf.d/default.conf
EXPOSE 8080
