package com.nextbreakpoint.blueprint.designs.handlers;

import com.nextbreakpoint.blueprint.common.vertx.Failure;
import com.nextbreakpoint.blueprint.designs.common.EventBusAdapter;
import com.nextbreakpoint.blueprint.designs.common.MessageConsumerAdapter;
import com.nextbreakpoint.blueprint.designs.common.RoutingContextAdapter;
import com.nextbreakpoint.blueprint.designs.model.DesignChangedNotification;
import com.nextbreakpoint.blueprint.designs.model.SessionUpdatedNotification;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonObject;
import lombok.extern.log4j.Log4j2;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

@Log4j2
public class WatchHandler implements Handler<RoutingContextAdapter> {
    private final Map<String, Set<Watcher>> watcherMap = new HashMap<>();

    private final EventBusAdapter eventBusAdapter;

    protected WatchHandler(EventBusAdapter eventBusAdapter) {
        this.eventBusAdapter = Objects.requireNonNull(eventBusAdapter);
        eventBusAdapter.registerDesignChangeNotificationConsumer(this::dispatchNotification);
    }

    public static WatchHandler create(EventBusAdapter adapter) {
        return new WatchHandler(adapter);
    }

    public void handle(RoutingContextAdapter routingContext) {
        try {
            createWatcher(routingContext);
        } catch (Exception e) {
            routingContext.fail(Failure.requestFailed(e));
        }
    }

    private void createWatcher(RoutingContextAdapter routingContext) {
        final long eventId = routingContext.getLastMessageId();
        final String revision = routingContext.getRevision();
        final String watchKey = routingContext.getWatchKey();
        final String sessionId = UUID.randomUUID().toString();

        final Watcher watcher = new Watcher(watchKey, sessionId, eventId);

        final Set<Watcher> watchers = watcherMap.getOrDefault(watchKey, new HashSet<>());

        watchers.add(watcher);

        watcherMap.put(watchKey, watchers);

        log.info("Session created (session = {}, eventId = {})", sessionId, eventId);

        final JsonObject openData = new JsonObject()
            .put("session", sessionId)
            .put("revision", revision);

        routingContext.initiateEventStreamResponse();
        routingContext.writeEvent("open", watcher.getEventId(), openData.encode());

        final MessageConsumerAdapter consumer = eventBusAdapter.registerSessionUpdateNotificationConsumer(sessionId, notification -> {
            try {
                final String newRevision = notification.getRevision();

                watcher.setRevision(newRevision);
                watcher.setEventId(watcher.getEventId() + 1);

                final JsonObject updateData = new JsonObject()
                    .put("uuid", watcher.getWatchKey())
                    .put("session", watcher.getSessionId())
                    .put("revision", watcher.getRevision());

                log.info("Send update notification (session = {}, revision = {})", watcher.getSessionId(), watcher.getRevision());

                routingContext.writeEvent("update", watcher.getEventId(), updateData.encode());
            } catch (Exception e) {
                log.warn("Cannot write message (session = {})", sessionId, e);
            }
        });

        routingContext.setResponseCloseHandler(ignored -> {
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

    private void notifyWatcher(Watcher watcher, String revision) {
        log.info("Notify watcher for session {}", watcher.sessionId);
        final var notification = SessionUpdatedNotification.builder().withRevision(revision).build();
        eventBusAdapter.publishSessionUpdateNotification(watcher.getSessionId(), notification);
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
