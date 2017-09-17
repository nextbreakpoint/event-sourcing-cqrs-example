FROM ubuntu:16.04

RUN apt-get update && \
    apt-get install -y nginx vim

RUN apt-get update && \
    apt-get -y install python curl unzip && cd /tmp && \
    curl "https://s3.amazonaws.com/aws-cli/awscli-bundle.zip" \
    -o "awscli-bundle.zip" && \
    unzip awscli-bundle.zip && \
    ./awscli-bundle/install -i /usr/local/aws -b /usr/local/bin/aws && \
    rm awscli-bundle.zip && rm -rf awscli-bundle

COPY entrypoint.sh /entrypoint.sh

RUN curl -L -o /tmp/consul.zip https://releases.hashicorp.com/consul/0.9.3/consul_0.9.3_linux_amd64.zip && \
  unzip -d /tmp /tmp/consul.zip >/dev/null && \
  chmod +x /tmp/consul && \
  mv /tmp/consul /usr/local/bin/consul && \
  mkdir -p /etc/consul.d && \
  mkdir -p /etc/service && \
  mkdir -p /mnt/consul && \
  mkdir -p /var/consul && \
  chmod +rwx /mnt/consul && \
  chmod +rwx /var/consul && \
  rm /tmp/consul.zip

ENV DNS_RESOLVES consul
ENV DNS_PORT 8600

RUN chmod u+x /entrypoint.sh

ENV SECRETS_BUCKET_NAME nextbreakpoint-services
ENV ENVIRONMENT production

EXPOSE 443

ENTRYPOINT ["/entrypoint.sh"]
