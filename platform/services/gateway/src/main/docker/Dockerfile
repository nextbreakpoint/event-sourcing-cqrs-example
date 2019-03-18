FROM ${java.image}

#RUN apt-get update -y && apt-get install -y openssl python bash wget

#RUN wget https://bootstrap.pypa.io/get-pip.py && python get-pip.py && pip install awscli

COPY maven/ /maven
COPY entrypoint.sh /entrypoint.sh

RUN chmod u+x /entrypoint.sh

EXPOSE ${container.port}

ENV CONFIG_NAME ${config.name}
ENV SERVICE_JAR ${service.jar}

ENTRYPOINT ["sh"]
CMD ["/entrypoint.sh"]
