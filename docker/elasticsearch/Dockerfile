ARG version
FROM elasticsearch:${version}
COPY elasticsearch-entrypoint.sh /elasticsearch-entrypoint.sh
COPY elasticsearch-initdb.sh /elasticsearch-initdb.sh
USER root
RUN chmod a+x /elasticsearch-entrypoint.sh
RUN chmod a+x /elasticsearch-initdb.sh
USER elasticsearch:root
ENTRYPOINT ["/elasticsearch-entrypoint.sh"]
