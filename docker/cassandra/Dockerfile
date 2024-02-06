ARG version
FROM cassandra:${version}
COPY cassandra-entrypoint.sh /cassandra-entrypoint.sh
COPY cassandra-initdb.sh /cassandra-initdb.sh
RUN chmod a+x /cassandra-entrypoint.sh
RUN chmod a+x /cassandra-initdb.sh
RUN sed -i -e "s/org.apache.cassandra.auth.AllowAllAuthenticator/org.apache.cassandra.auth.PasswordAuthenticator/g" /etc/cassandra/cassandra.yaml
RUN sed -i -e "s/authorizer: AllowAllAuthorizer/authorizer: CassandraAuthorizer/g" /etc/cassandra/cassandra.yaml
USER cassandra
ENTRYPOINT ["/cassandra-entrypoint.sh"]
CMD ["cassandra", "-f"]
