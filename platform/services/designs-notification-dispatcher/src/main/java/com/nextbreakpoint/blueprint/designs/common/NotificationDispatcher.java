package com.nextbreakpoint.blueprint.designs.common;

import com.nextbreakpoint.blueprint.common.vertx.Failure;
import com.nextbreakpoint.blueprint.designs.model.DesignChangedNotification;
import io.vertx.core.Handler;
import io.vertx.core.impl.logging.Logger;
import io.vertx.core.impl.logging.LoggerFactory;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonObject;
import io.vertx.rxjava.core.Vertx;
import io.vertx.rxjava.core.eventbus.Message;
import io.vertx.rxjava.core.eventbus.MessageConsumer;
import io.vertx.rxjava.ext.web.RoutingContext;

import java.util.*;

public class NotificationDispatcher implements Handler<RoutingContext> {
    private final Logger logger = LoggerFactory.getLogger(NotificationDispatcher.class.getName());

    private Map<String, Set<Watcher>> watcherMap = new HashMap<>();

    private final Vertx vertx;

    protected NotificationDispatcher(Vertx vertx) {
        this.vertx = vertx;

        vertx.eventBus().consumer("notifications", this::handleMessage);
    }

    private void handleMessage(Message<Object> message) {
        try {
            dispatchNotification(Json.decodeValue((String) message.body(), DesignChangedNotification.class));
        } catch (Exception e) {
            logger.error("Failed to process event", e);
        }
    }

    public static NotificationDispatcher create(Vertx vertx) {
        return new NotificationDispatcher(vertx);
    }

    public void handle(RoutingContext routingContext) {
        try {
            createWatcher(routingContext);
        } catch (Exception e) {
            routingContext.fail(Failure.requestFailed(e));
        }
    }

    protected void createWatcher(RoutingContext routingContext) {
//        Long offset = getOffset(routingContext);

        final String watchKey = getWatchKey(routingContext);

        final String sessionId = UUID.randomUUID().toString();

//        final String lastEventId = routingContext.request().headers().get("Last-Message-ID");

//        if (lastEventId != null) {
//            offset = Long.parseLong(lastEventId);
//        }

        final Watcher watcher = new Watcher(watchKey, sessionId);

//        watcher.setOffset(offset);

        final Set<Watcher> watchers = watcherMap.getOrDefault(watchKey, new HashSet<>());

        watchers.add(watcher);

        watcherMap.put(watchKey, watchers);

        logger.info("Session created (session = " + sessionId + ")");

        routingContext.response().setChunked(true);

        routingContext.response().headers().add("Content-Type", "text/event-stream;charset=UTF-8");

        routingContext.response().headers().add("Connection", "keep-alive");

        routingContext.response().write(makeEvent("open", 0L, "{\"session\":\"" + sessionId + "\"}"));

        final MessageConsumer<JsonObject> consumer = vertx.eventBus().consumer("notifications." + sessionId, msg -> {
            try {
                final JsonObject message = msg.body();

                final JsonObject data = new JsonObject();

                data.put("session", sessionId);

                data.put("uuid", watchKey);

                logger.info("Send update notification to session " + watcher.sessionId);

                routingContext.response().write(makeEvent("update", message.getLong("timestamp"), data.encode()));
            } catch (Exception e) {
                logger.warn("Cannot write message (session = " + sessionId + ")", e);
            }
        });

        routingContext.response().closeHandler(nothing -> {
            try {
                logger.info("Session closed (session = " + sessionId + ")");

                destroyWatcher(watcher);

                consumer.unregister();
            } catch (Exception e) {
                logger.warn("Cannot close session (session = " + sessionId + ")", e);
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

    private String makeEvent(String name, Long id, String data) {
        return "event: " + name + "\nid: " + id + "\ndata: " + data + "\n\n";
    }

    private void notifyWatcher(Watcher watcher, Long timestamp) {
//        watcher.setOffset(timestamp);

        logger.info("Notify watcher for session " + watcher.sessionId);

        final JsonObject message = makeMessageData(timestamp);

        vertx.eventBus().publish("notifications." + watcher.getSessionId(), message);
    }

    private JsonObject makeMessageData(Long timestamp) {
        final JsonObject message = new JsonObject();

        message.put("timestamp", timestamp);

        return message;
    }

    private String getWatchKey(RoutingContext routingContext) {
        final String uuid = routingContext.pathParam("designId");
        if (uuid == null) {
            return "*";
        } else {
            return uuid;
        }
    }

    private Long getOffset(RoutingContext routingContext) {
        return Long.parseLong(routingContext.pathParam("offset"));
    }

    private void dispatchNotification(DesignChangedNotification notification) {
        final String watchKey = notification.getKey();
        final Long timestamp = notification.getTimestamp();

        logger.info("Processing event " + watchKey + " (timestamp = " + timestamp +  ")");

        final Set<Watcher> watchers = watcherMap.get(watchKey);

        if (watchers != null && watchers.size() > 0) {
            watchers.forEach(watcher -> notifyWatcher(watcher, System.currentTimeMillis()));
        } else {
            logger.info("No watchers found for resource " + watchKey);
        }

        final Set<Watcher> otherWatchers = watcherMap.get("*");

        if (otherWatchers != null && otherWatchers.size() > 0) {
            otherWatchers.forEach(watcher -> notifyWatcher(watcher, System.currentTimeMillis()));
        } else {
            logger.info("No watchers found for all resources");
        }
    }

    private static class Watcher {
        private final String sessionId;
        private final String watchKey;
//        private Long offset;

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
//
//        public Long getOffset() {
//            return offset;
//        }
//
//        public void setOffset(Long offset) {
//            this.offset = offset;
//        }
    }
}
