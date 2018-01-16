FROM java:8-jdk-alpine

RUN apk update && apk add ca-certificates && update-ca-certificates && apk add openssl python bash

RUN wget https://bootstrap.pypa.io/get-pip.py && python get-pip.py && pip install awscli

COPY maven/ /maven
COPY entrypoint.sh /entrypoint.sh

RUN chmod u+x /entrypoint.sh

VOLUME /config
VOLUME /keystores

EXPOSE ${container.port}

ENV SERVICE_JAR ${service.jar}
ENV CONFIG_NAME ${config.name}

ENTRYPOINT ["/entrypoint.sh"]
