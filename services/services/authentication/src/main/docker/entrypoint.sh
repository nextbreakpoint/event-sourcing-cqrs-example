#!/bin/bash

java \
    -XX:+UnlockExperimentalVMOptions \
    -XX:+UseParallelGC \
    -XX:MinHeapFreeRatio=5 \
    -XX:MaxHeapFreeRatio=10 \
    -XX:GCTimeRatio=4 \
    -XX:AdaptiveSizePolicyWeight=90 \
    -XX:MaxRAMPercentage=70 \
    --module-path /maven \
    --add-modules ALL-MODULE-PATH \
    --add-opens java.base/java.nio=ALL-UNNAMED \
    --add-opens java.base/sun.nio.ch=ALL-UNNAMED \
    --add-opens java.base/sun.net.dns=ALL-UNNAMED \
    -Dnetworkaddress.cache.ttl=1 \
    -Dnetworkaddress.cache.negative.ttl=1 \
    -Dvertx.logger-delegate-factory-class-name=io.vertx.core.logging.SLF4JLogDelegateFactory \
    com.nextbreakpoint.shop.authentication.Verticle \
    /config/$CONFIG_NAME
