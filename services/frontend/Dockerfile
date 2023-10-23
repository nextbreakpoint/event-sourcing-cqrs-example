FROM node:17-alpine3.12 AS build
ARG maven_args
ADD react /react
WORKDIR /react
RUN cat package.json
RUN rm -fR node_modules
RUN npm -dd --fetch-timeout=600000 install
RUN npm run build
RUN ls -al

FROM node:17-alpine3.12
COPY /app /app
WORKDIR /app
RUN cat package.json
RUN rm -fR node_modules
RUN npm -dd --fetch-timeout=600000 install
COPY --from=build /react/dist/* /app/public/js/
EXPOSE 8080
ENTRYPOINT ["node"]
CMD ["/app/bin/www"]
