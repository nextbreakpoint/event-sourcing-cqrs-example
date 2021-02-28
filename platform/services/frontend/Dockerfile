FROM node:15.10.0-alpine3.12 AS build
ADD react /react
WORKDIR /react
RUN rm -fR node_modules
RUN npm install
RUN npm run build
RUN ls -al
FROM node:15.10.0-alpine3.12
COPY /app /app
WORKDIR /app
RUN apk add --no-cache python2 make g++
RUN rm -fR node_modules
RUN npm install
COPY --from=build /react/public/js/* /app/public/js/
EXPOSE 8080
ENTRYPOINT ["node"]
CMD ["/app/bin/www"]
