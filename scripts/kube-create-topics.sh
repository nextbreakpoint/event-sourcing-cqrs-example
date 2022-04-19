#!/bin/bash

set +e

kubectl -n platform exec $(kubectl -n platform get pod -l component=kafka -o json | jq -r '.items[0].metadata.name') -- kafka-topics --bootstrap-server=localhost:9092 --create --topic events --config "retention.ms=604800000" --replication-factor=1 --partitions=16
kubectl -n platform exec $(kubectl -n platform get pod -l component=kafka -o json | jq -r '.items[0].metadata.name') -- kafka-topics --bootstrap-server=localhost:9092 --create --topic buffer --config "retention.ms=604800000" --replication-factor=1 --partitions=16

kubectl -n platform exec $(kubectl -n platform get pod -l component=kafka -o json | jq -r '.items[0].metadata.name') -- kafka-topics --bootstrap-server=localhost:9092 --create --topic render-requested-0 --config "cleanup.policy=compact" --config "delete.retention.ms=5000" --config "max.compaction.lag.ms=10000" --config "min.compaction.lag.ms=5000" --config "min.cleanable.dirty.ratio=0.1" --config "segment.ms=5000" --config "retention.ms=604800000" --replication-factor=1 --partitions=16
kubectl -n platform exec $(kubectl -n platform get pod -l component=kafka -o json | jq -r '.items[0].metadata.name') -- kafka-topics --bootstrap-server=localhost:9092 --create --topic render-requested-1 --config "cleanup.policy=compact" --config "delete.retention.ms=5000" --config "max.compaction.lag.ms=10000" --config "min.compaction.lag.ms=5000" --config "min.cleanable.dirty.ratio=0.1" --config "segment.ms=5000" --config "retention.ms=604800000" --replication-factor=1 --partitions=16
kubectl -n platform exec $(kubectl -n platform get pod -l component=kafka -o json | jq -r '.items[0].metadata.name') -- kafka-topics --bootstrap-server=localhost:9092 --create --topic render-requested-2 --config "cleanup.policy=compact" --config "delete.retention.ms=5000" --config "max.compaction.lag.ms=10000" --config "min.compaction.lag.ms=5000" --config "min.cleanable.dirty.ratio=0.1" --config "segment.ms=5000" --config "retention.ms=604800000" --replication-factor=1 --partitions=32
kubectl -n platform exec $(kubectl -n platform get pod -l component=kafka -o json | jq -r '.items[0].metadata.name') -- kafka-topics --bootstrap-server=localhost:9092 --create --topic render-requested-3 --config "cleanup.policy=compact" --config "delete.retention.ms=5000" --config "max.compaction.lag.ms=10000" --config "min.compaction.lag.ms=5000" --config "min.cleanable.dirty.ratio=0.1" --config "segment.ms=5000" --config "retention.ms=604800000" --replication-factor=1 --partitions=64

kubectl -n platform exec $(kubectl -n platform get pod -l component=kafka -o json | jq -r '.items[0].metadata.name') -- kafka-topics --bootstrap-server=localhost:9092 --create --topic render-completed-0 --config "cleanup.policy=compact" --config "delete.retention.ms=5000" --config "max.compaction.lag.ms=10000" --config "min.compaction.lag.ms=5000" --config "min.cleanable.dirty.ratio=0.1" --config "segment.ms=5000" --config "retention.ms=604800000" --replication-factor=1 --partitions=16
kubectl -n platform exec $(kubectl -n platform get pod -l component=kafka -o json | jq -r '.items[0].metadata.name') -- kafka-topics --bootstrap-server=localhost:9092 --create --topic render-completed-1 --config "cleanup.policy=compact" --config "delete.retention.ms=5000" --config "max.compaction.lag.ms=10000" --config "min.compaction.lag.ms=5000" --config "min.cleanable.dirty.ratio=0.1" --config "segment.ms=5000" --config "retention.ms=604800000" --replication-factor=1 --partitions=16
kubectl -n platform exec $(kubectl -n platform get pod -l component=kafka -o json | jq -r '.items[0].metadata.name') -- kafka-topics --bootstrap-server=localhost:9092 --create --topic render-completed-2 --config "cleanup.policy=compact" --config "delete.retention.ms=5000" --config "max.compaction.lag.ms=10000" --config "min.compaction.lag.ms=5000" --config "min.cleanable.dirty.ratio=0.1" --config "segment.ms=5000" --config "retention.ms=604800000" --replication-factor=1 --partitions=32
kubectl -n platform exec $(kubectl -n platform get pod -l component=kafka -o json | jq -r '.items[0].metadata.name') -- kafka-topics --bootstrap-server=localhost:9092 --create --topic render-completed-3 --config "cleanup.policy=compact" --config "delete.retention.ms=5000" --config "max.compaction.lag.ms=10000" --config "min.compaction.lag.ms=5000" --config "min.cleanable.dirty.ratio=0.1" --config "segment.ms=5000" --config "retention.ms=604800000" --replication-factor=1 --partitions=64
