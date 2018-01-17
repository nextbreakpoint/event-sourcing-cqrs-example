FROM openjdk:9-jdk-slim

RUN apt-get update -y && apt-get install -y openssl python bash wget

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
