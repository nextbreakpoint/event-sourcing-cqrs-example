#!/bin/sh

#export KEYSTORE_SECRET=secret
#export JAEGER_SERVICE_NAME=designs-aggregate-fetcher
#export JAEGER_AGENT_HOST=localhost
#export JAEGER_TAGS=env\=dev
#export JAEGER_SAMPLER_TYPE=const
#export JAEGER_SAMPLER_PARAM=1
#export JAEGER_REPORTER_LOG_SPANS=true
#export AWS_ACCESS_KEY_ID=admin
#export AWS_SECRET_ACCESS_KEY=password
#export MINIO_HOST=localhost
#export MINIO_PORT=9000
#export ELASTICSEARCH_INDEX=test_designs_query_events
#export BUCKET_NAME=tiles
#export ELASTICSEARCH_HOST=localhost
#export ELASTICSEARCH_PORT=9200
#export KAFKA_HOST=localhost
#export KAFKA_PORT=9093
#export EVENTS_TOPIC=test-designs-query-events
#export DEBUG_OPTS="-agentlib:jdwp=transport=dt_socket,server=y,suspend=y,address=*:35120"

#    --module-path target/libs:target/service.jar \

java \
    -XX:MinHeapFreeRatio=5 \
    -XX:MaxHeapFreeRatio=10 \
    -XX:GCTimeRatio=4 \
    -XX:AdaptiveSizePolicyWeight=90 \
    -XX:MaxRAMPercentage=70 \
    --module-path /libs \
    --add-modules ALL-MODULE-PATH \
    --add-opens java.base/java.nio=ALL-UNNAMED \
    --add-opens java.base/sun.nio.ch=ALL-UNNAMED \
    --add-opens java.base/sun.net.dns=ALL-UNNAMED \
    -Dvertx.logger-delegate-factory-class-name=io.vertx.core.logging.SLF4JLogDelegateFactory \
    $DEBUG_OPTS com.nextbreakpoint.blueprint.designs.Verticle $@
