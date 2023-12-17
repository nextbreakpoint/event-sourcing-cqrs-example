package com.nextbreakpoint.blueprint.designs.common;

import com.nextbreakpoint.blueprint.common.vertx.Failure;
import io.vertx.rxjava.ext.web.RoutingContext;
import lombok.extern.log4j.Log4j2;

import java.util.Objects;
import java.util.function.Consumer;

@Log4j2
public class RoutingContextAdapter {
    private static final String REVISION_NULL = "0000000000000000-0000000000000000";

    private final RoutingContext routingContext;

    public RoutingContextAdapter(RoutingContext routingContext) {
        this.routingContext = Objects.requireNonNull(routingContext);
    }

    public String getWatchKey() {
        return routingContext.queryParam("designId").stream().findFirst().orElse("*");
    }

    public String getRevision() {
        return routingContext.queryParam("revision").stream().findFirst().orElse(REVISION_NULL);
    }

    public long getLastMessageId() {
        final String lastEventId = routingContext.request().headers().get("Last-Message-ID");
        return lastEventId != null ? Long.parseLong(lastEventId) : 0L;
    }

    public void fail(Failure failure) {
        routingContext.fail(failure);
    }

    public void initiateEventStreamResponse() {
        routingContext.response().setChunked(true);
        routingContext.response().headers().add("Content-Type", "text/event-stream;charset=UTF-8");
        routingContext.response().headers().add("Connection", "keep-alive");
    }

    public void writeEvent(String eventName, long eventId, String eventData) {
        routingContext.response().write(makeEvent(eventName, eventId, eventData));
    }

    private String makeEvent(String eventName, long eventId, String eventData) {
        return "event: " + eventName + "\nid: " + eventId + "\ndata: " + eventData + "\n\n";
    }

    public void setResponseCloseHandler(Consumer<Void> callback) {
        routingContext.response().closeHandler(ignored -> callback.accept(null));
    }
}
