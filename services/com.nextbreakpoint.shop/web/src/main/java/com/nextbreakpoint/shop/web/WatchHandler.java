package com.nextbreakpoint.shop.web;

import com.nextbreakpoint.shop.common.Authentication;
import com.nextbreakpoint.shop.common.Authority;
import com.nextbreakpoint.shop.common.Failure;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.rxjava.core.Vertx;
import io.vertx.rxjava.core.buffer.Buffer;
import io.vertx.rxjava.core.eventbus.MessageConsumer;
import io.vertx.rxjava.ext.auth.jwt.JWTAuth;
import io.vertx.rxjava.ext.web.RoutingContext;
import io.vertx.rxjava.ext.web.client.HttpResponse;
import io.vertx.rxjava.ext.web.client.WebClient;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static com.nextbreakpoint.shop.common.Authentication.NULL_USER_UUID;
import static java.util.Arrays.asList;

public abstract class WatchHandler implements Handler<RoutingContext> {
    private Map<String, Set<Watcher>> watcherMap = new HashMap<>();

    private final Vertx vertx;
    private final JWTAuth jwtProvider;
    private final WebClient client;

    private int bucketCount;

    protected WatchHandler(Vertx vertx, JWTAuth jwtProvider, WebClient client, JsonObject config) {
        this.vertx = vertx;
        this.jwtProvider = jwtProvider;
        this.client = client;

        poll();
    }

    public void handle(RoutingContext routingContext) {
        try {
            createWatcher(routingContext);
        } catch (Exception e) {
            routingContext.fail(Failure.requestFailed(e));
        }
    }

    protected void createWatcher(RoutingContext routingContext) {
        Long offset = getOffset(routingContext);

        final String watchKey = getWatchKey(routingContext);

        final String sessionId = UUID.randomUUID().toString();

        final String lastEventId = routingContext.request().headers().get("Last-Event-ID");

        if (lastEventId != null) {
            offset = Long.parseLong(lastEventId);
        }

        final Watcher watcher = new Watcher(watchKey, sessionId);

        watcher.setOffset(offset);

        final Set<Watcher> watchers = watcherMap.getOrDefault(watchKey, new HashSet<>());

        watchers.add(watcher);

        watcherMap.put(watchKey, watchers);

        routingContext.response().setChunked(true);

        routingContext.response().headers().add("Content-Type", "text/event-stream;charset=UTF-8");

        routingContext.response().headers().add("Connection", "keep-alive");

        final MessageConsumer consumer = vertx.eventBus().consumer("events." + sessionId, msg -> {
            routingContext.response().write(makeEvent("message", ((JsonObject) msg.body()).getLong("offset"), ((JsonObject) msg.body()).encode()));
        });

        routingContext.response().closeHandler(x -> {
            destroyWatcher(watcher);

            consumer.unregister();
        });
    }

    private void destroyWatcher(Watcher watcher) {
        final Set<Watcher> set = watcherMap.get(watcher.getWatchKey());

        set.remove(watcher);

        if (set.size() == 0) {
            watcherMap.remove(watcher.getWatchKey());
        }
    }

    private void poll() {
        if (watcherMap.size() == 0) {
            vertx.setTimer(1000, timer -> poll());
        } else {
            final List<String> keys = watcherMap.keySet().stream().collect(Collectors.toList());

            bucketCount = keys.size() / 100 + (keys.size() % 100 == 0 ? 0 : 1);

            final List<List<String>> buckets = new ArrayList(bucketCount);

            IntStream.range(0, bucketCount).forEach(i -> buckets.add(new ArrayList<>()));

            IntStream.range(0, keys.size()).forEach(i -> buckets.get(i % bucketCount).add(keys.get(i)));

            buckets.stream().forEach(bucket -> pollBucket(bucket));
        }
    }

    private String makeEvent(String name, Long id, String data) {
        return "event: " + name + "\nid: " + id + "\ndata: " + data + "\n\n";
    }

    private void processWatcher(Watcher watcher, Long lastModified) {
        watcher.setOffset(lastModified);

        final JsonObject message = makeMessageData(lastModified);

        vertx.eventBus().publish("events." + watcher.getSessionId(), message);
    }

    private JsonObject makeMessageData(Long lastModified) {
        final JsonObject message = new JsonObject();

        message.put("offset", lastModified);

        return message;
    }

    protected abstract void pollBucket(List<String> bucket);

    protected abstract String getWatchKey(RoutingContext routingContext);

    protected abstract Long getOffset(RoutingContext routingContext);

    protected String makeAccessToken() {
        return Authentication.generateToken(jwtProvider, NULL_USER_UUID, asList(Authority.PLATFORM));
    }

    protected void handleState(HttpResponse<Buffer> response) {
        if (response.statusCode() == 200) {
            final JsonArray result = response.bodyAsJsonArray();

            IntStream.range(0, result.size()).forEach(i -> {
                final JsonObject object = result.getJsonObject(i);

                final String watchKey = object.getString("watch_key");

                final Long lastModified = object.getLong("last_modified");

                final Set<Watcher> watchers = watcherMap.get(watchKey);

                if (watchers != null && watchers.size() > 0) {
                    watchers.stream().forEach(watcher -> {
                        if (watcher.getOffset() < lastModified) {
                            processWatcher(watcher, lastModified);
                        }
                    });
                }
            });
        }

        bucketCount -= 1;

        if (bucketCount == 0) {
            vertx.setTimer(1000, timer -> poll());
        }
    }

    protected void handleFailure(Throwable e) {
        vertx.setTimer(1000, timer -> poll());
    }

    protected WebClient getClient() {
        return client;
    }

    private class Watcher {
        private final String sessionId;
        private final String watchKey;
        private Long offset;

        public Watcher(String watchKey, String sessionId) {
            this.sessionId = sessionId;
            this.watchKey = watchKey;
        }

        public String getSessionId() {
            return sessionId;
        }

        public String getWatchKey() {
            return watchKey;
        }

        public Long getOffset() {
            return offset;
        }

        public void setOffset(Long offset) {
            this.offset = offset;
        }
    }
}