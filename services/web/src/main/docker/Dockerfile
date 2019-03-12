FROM node:11-alpine

COPY maven/ /maven
COPY entrypoint.sh /entrypoint.sh

RUN chmod u+x /entrypoint.sh

EXPOSE ${container.port}

ENV SECRETS_PATH /secrets
ENV CONFIG_PATH /config
ENV PORT ${container.port}

RUN cd maven && npm install

ENTRYPOINT ["/entrypoint.sh"]
