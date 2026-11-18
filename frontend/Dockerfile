# ==============================================================================
# Stage 1: Build the Angular production bundle on Node.js 24
# ==============================================================================
FROM node:24-alpine AS build
WORKDIR /workspace

# 1) Install dependencies in a cached layer when package*.json has not changed
COPY package.json package-lock.json ./
RUN npm ci

# 2) Copy Angular project files and build the production bundle
#    (@angular/build:application outputs browser files to dist/frontend/browser)
COPY angular.json tsconfig.json tsconfig.app.json .postcssrc.json ./
COPY public/ public/
COPY src/ src/
RUN npm run build -- --configuration production

# ==============================================================================
# Stage 2: Serve static bundle and reverse-proxy /api/ via lightweight Nginx
# ==============================================================================
FROM nginx:1.27-alpine AS runtime

COPY nginx.conf /etc/nginx/conf.d/default.conf
COPY --from=build /workspace/dist/frontend/browser/ /usr/share/nginx/html/

EXPOSE 80
CMD ["nginx", "-g", "daemon off;"]
