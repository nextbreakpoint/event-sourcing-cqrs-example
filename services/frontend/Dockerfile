FROM node:21.5.0-alpine3.19 AS build
ARG npm_registry
ARG npm_auth
ADD react /react
WORKDIR /react
RUN cat package.json
RUN rm -fR node_modules
RUN rm -fR package-lock.json
RUN npm config set registry $npm_registry
RUN npm config set $npm_auth
RUN npm -dd --fetch-timeout=600000 install
RUN npm run build
RUN ls -al

FROM node:21.5.0-alpine3.19
ARG npm_registry
ARG npm_auth
COPY /app /app
WORKDIR /app
RUN cat package.json
RUN rm -fR node_modules
RUN rm -fR package-lock.json
RUN npm config set registry $npm_registry
RUN npm config set $npm_auth
RUN npm -dd --fetch-timeout=600000 install
COPY --from=build /react/dist/* /app/public/js/
EXPOSE 8080
ENTRYPOINT ["node"]
CMD ["/app/bin/www"]
