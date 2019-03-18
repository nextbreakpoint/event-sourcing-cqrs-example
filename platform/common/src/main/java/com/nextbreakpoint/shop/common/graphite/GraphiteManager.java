package com.nextbreakpoint.shop.common.graphite;

import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.graphite.Graphite;
import com.codahale.metrics.graphite.GraphiteReporter;
import io.vertx.core.json.JsonObject;

import java.net.InetSocketAddress;
import java.util.concurrent.TimeUnit;

public class GraphiteManager {
    private GraphiteManager() {}

    public static void configureMetrics(JsonObject config) {
        if (config.getBoolean("graphite_reporter_enabled", false)) {
            final String graphiteHost = config.getString("graphite_host", "localhost");
            final int graphitePort = config.getInteger("graphite_port", 2003);
            final MetricRegistry metricRegistry = new MetricRegistry();
            final Graphite graphite = new Graphite(new InetSocketAddress(graphiteHost, graphitePort));
            final GraphiteReporter reporter = GraphiteReporter.forRegistry(metricRegistry)
                    .prefixedWith(graphiteHost)
                    .convertRatesTo(TimeUnit.SECONDS)
                    .convertDurationsTo(TimeUnit.MILLISECONDS)
                    .filter((name, metric) -> true)
                    .build(graphite);
            reporter.start(10, TimeUnit.SECONDS);
        }
    }
}
