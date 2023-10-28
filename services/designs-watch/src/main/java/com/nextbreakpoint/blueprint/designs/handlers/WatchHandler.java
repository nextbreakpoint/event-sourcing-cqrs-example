package com.nextbreakpoint.blueprint.designs.handlers;

import com.nextbreakpoint.blueprint.common.core.Json;
import com.nextbreakpoint.blueprint.common.vertx.Failure;
import com.nextbreakpoint.blueprint.designs.model.DesignChangedNotification;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonObject;
import io.vertx.rxjava.core.Vertx;
import io.vertx.rxjava.core.eventbus.Message;
import io.vertx.rxjava.core.eventbus.MessageConsumer;
import io.vertx.rxjava.ext.web.RoutingContext;
import lombok.extern.log4j.Log4j2;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Log4j2
public class WatchHandler implements Handler<RoutingContext> {
    private static final String REVISION_NULL = "0000000000000000-0000000000000000";

    private Map<String, Set<Watcher>> watcherMap = new HashMap<>();

    private final Vertx vertx;

    protected WatchHandler(Vertx vertx) {
        this.vertx = vertx;

        vertx.eventBus().consumer("notifications", this::handleMessage);
    }

    public static WatchHandler create(Vertx vertx) {
        return new WatchHandler(vertx);
    }

    public void handle(RoutingContext routingContext) {
        try {
            createWatcher(routingContext);
        } catch (Exception e) {
            routingContext.fail(Failure.requestFailed(e));
        }
    }

    private void handleMessage(Message<Object> message) {
        try {
            dispatchNotification(Json.decodeValue((String) message.body(), DesignChangedNotification.class));
        } catch (Exception e) {
            log.error("Failed to process event", e);
        }
    }

    protected void createWatcher(RoutingContext routingContext) {
        final long eventId = getLastMessageId(routingContext);

        final String revision = getRevision(routingContext);

        final String watchKey = getWatchKey(routingContext);

        final String sessionId = UUID.randomUUID().toString();

        final Watcher watcher = new Watcher(watchKey, sessionId, eventId);

        final Set<Watcher> watchers = watcherMap.getOrDefault(watchKey, new HashSet<>());

        watchers.add(watcher);

        watcherMap.put(watchKey, watchers);

        log.info("Session created (session = {}, eventId = {})", sessionId, eventId);

        routingContext.response().setChunked(true);

        routingContext.response().headers().add("Content-Type", "text/event-stream;charset=UTF-8");

        routingContext.response().headers().add("Connection", "keep-alive");

        final JsonObject openData = new JsonObject();

        openData.put("session", sessionId);
        openData.put("revision", revision);

        routingContext.response().write(makeEvent("open", watcher.getEventId(), openData.encode()));

        final MessageConsumer<JsonObject> consumer = vertx.eventBus().consumer("notifications." + sessionId, msg -> {
            try {
                final JsonObject message = msg.body();

                final JsonObject updateData = new JsonObject();

                final String newRevision = (String) message.getValue("revision");

                watcher.setRevision(newRevision);
                watcher.setEventId(watcher.getEventId() + 1);

                updateData.put("uuid", watcher.getWatchKey());
                updateData.put("session", watcher.getSessionId());
                updateData.put("revision", watcher.getRevision());

                log.info("Send update notification (session = {}, revision = {})", watcher.getSessionId(), watcher.getRevision());

                routingContext.response().write(makeEvent("update", watcher.getEventId(), updateData.encode()));
            } catch (Exception e) {
                log.warn("Cannot write message (session = {})", sessionId, e);
            }
        });

        routingContext.response().closeHandler(nothing -> {
            try {
                log.info("Session closed (session = {})", sessionId);

                destroyWatcher(watcher);

                consumer.unregister();
            } catch (Exception e) {
                log.warn("Cannot close session (session = {})", sessionId, e);
            }
        });
    }

    private void destroyWatcher(Watcher watcher) {
        final Set<Watcher> watchers = watcherMap.get(watcher.getWatchKey());

        watchers.remove(watcher);

        if (watchers.size() == 0) {
            watcherMap.remove(watcher.getWatchKey());
        }
    }

    private String makeEvent(String name, long timestamp, String data) {
        return "event: " + name + "\nid: " + timestamp + "\ndata: " + data + "\n\n";
    }

    private void notifyWatcher(Watcher watcher, String revision) {
        log.info("Notify watcher for session {}", watcher.sessionId);

        final JsonObject message = makeMessageData(revision);

        vertx.eventBus().publish("notifications." + watcher.getSessionId(), message);
    }

    private JsonObject makeMessageData(String revision) {
        final JsonObject message = new JsonObject();

        message.put("revision", revision);

        return message;
    }

    private String getWatchKey(RoutingContext routingContext) {
        return routingContext.queryParam("designId").stream().findFirst().orElse("*");
    }

    private String getRevision(RoutingContext routingContext) {
        return routingContext.queryParam("revision").stream().findFirst().orElse(REVISION_NULL);
    }

    private long getLastMessageId(RoutingContext routingContext) {
        final String lastEventId = routingContext.request().headers().get("Last-Message-ID");

        if (lastEventId != null) {
            return Long.parseLong(lastEventId);
        }

        return 0L;
    }

    private void dispatchNotification(DesignChangedNotification notification) {
        final String watchKey = notification.getKey();
        final String revision = notification.getRevision();

        log.info("Processing event {} (revision = {})", watchKey, revision);

        final Set<Watcher> watchers = watcherMap.get(watchKey);

        if (watchers != null && watchers.size() > 0) {
            watchers.forEach(watcher -> notifyWatcher(watcher, revision));
        } else {
            log.info("No watchers found for resource {}", watchKey);
        }

        final Set<Watcher> otherWatchers = watcherMap.get("*");

        if (otherWatchers != null && otherWatchers.size() > 0) {
            otherWatchers.forEach(watcher -> notifyWatcher(watcher, revision));
        } else {
            log.info("No watchers found for all resources");
        }
    }

    private static class Watcher {
        private final String sessionId;
        private final String watchKey;
        private String revision;
        private long eventId;

        public Watcher(String watchKey, String sessionId, long eventId) {
            this.sessionId = sessionId;
            this.watchKey = watchKey;
            this.eventId = eventId;
        }

        public String getSessionId() {
            return sessionId;
        }

        public String getWatchKey() {
            return watchKey;
        }

        public String getRevision() {
            return revision;
        }

        public void setRevision(String revision) {
            this.revision = revision;
        }

        public long getEventId() {
            return eventId;
        }

        public void setEventId(long eventId) {
            this.eventId = eventId;
        }
    }
}
