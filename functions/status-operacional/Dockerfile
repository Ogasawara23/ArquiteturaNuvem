# Azure Function (Node 20, modelo v4) em conteiner, para o docker-compose local.
# Na nuvem a mesma function e publicada no Azure Functions (plano Consumption) com `func azure functionapp publish`.
FROM node:22-alpine AS build
WORKDIR /src
COPY package*.json ./
RUN npm ci
COPY tsconfig.json host.json ./
COPY src ./src
RUN npm run build && npm prune --omit=dev

FROM mcr.microsoft.com/azure-functions/node:4-node22
ENV AzureWebJobsScriptRoot=/home/site/wwwroot \
    AzureFunctionsJobHost__Logging__Console__IsEnabled=true \
    FUNCTIONS_WORKER_RUNTIME=node
COPY --from=build /src/host.json /src/package.json /home/site/wwwroot/
COPY --from=build /src/dist /home/site/wwwroot/dist
COPY --from=build /src/node_modules /home/site/wwwroot/node_modules
EXPOSE 80
