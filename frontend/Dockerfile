# Imagem generica para qualquer microfrontend (shell ou remote): build Vite + nginx estatico.
# Uso (na pasta frontend/): docker build --build-arg PROJETO=mfe-kds --build-arg VITE_API_URL=https://... -t usuario/smartkds-mfe-kds:v1 .
# Em producao os bundles sao publicados em CDN / Azure Static Web Apps; a imagem atende o docker-compose.
FROM node:22-alpine AS build
ARG PROJETO
ARG VITE_API_URL=http://localhost:8080
ARG VITE_URL_MESA=http://localhost:5100
ARG VITE_KEYCLOAK_URL=
ARG VITE_REMOTE_KDS=http://localhost:5201/assets/remoteEntry.js
ARG VITE_REMOTE_CARDAPIO=http://localhost:5202/assets/remoteEntry.js
ARG VITE_REMOTE_GESTAO=http://localhost:5203/assets/remoteEntry.js
ARG VITE_REMOTE_COMANDA=http://localhost:5101/assets/remoteEntry.js
ENV VITE_API_URL=$VITE_API_URL VITE_URL_MESA=$VITE_URL_MESA VITE_KEYCLOAK_URL=$VITE_KEYCLOAK_URL \
    VITE_REMOTE_KDS=$VITE_REMOTE_KDS VITE_REMOTE_CARDAPIO=$VITE_REMOTE_CARDAPIO \
    VITE_REMOTE_GESTAO=$VITE_REMOTE_GESTAO VITE_REMOTE_COMANDA=$VITE_REMOTE_COMANDA
WORKDIR /app
COPY ${PROJETO}/package*.json ./
RUN npm ci
COPY ${PROJETO}/ ./
RUN npx vite build

FROM nginx:1.27-alpine
COPY nginx/microfrontend.conf /etc/nginx/conf.d/default.conf
COPY --from=build /app/dist /usr/share/nginx/html
EXPOSE 80
