package com.nextbreakpoint.shop.common.vertx;

import io.vertx.core.Handler;
import io.vertx.rxjava.ext.web.RoutingContext;
import org.slf4j.MDC;

import static com.nextbreakpoint.shop.common.model.Headers.X_TRACE_ID;

public class MDCHandler implements Handler<RoutingContext> {
    public static MDCHandler create() {
        return new MDCHandler();
    }

    @Override
    public void handle(RoutingContext context) {
        try {
            final String traceId = context.request().getHeader(X_TRACE_ID);
            if (traceId != null) {
                context.put("request-trace-id", traceId);
                MDC.put("request-trace-id", traceId);
            } else {
                final String contextTraceId = context.get("request-trace-id");
                if (contextTraceId != null) {
                    MDC.put("request-trace-id", contextTraceId);
                } else {
                    MDC.put("request-trace-id", "n/a");
                }
            }
            context.next();
        } finally {
            MDC.clear();
        }
    }
}
