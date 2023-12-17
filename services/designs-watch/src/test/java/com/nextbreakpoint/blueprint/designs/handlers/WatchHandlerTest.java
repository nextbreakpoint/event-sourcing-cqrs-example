package com.nextbreakpoint.blueprint.designs.handlers;

import com.nextbreakpoint.blueprint.designs.common.EventBusAdapter;
import org.junit.jupiter.api.Test;

import static org.mockito.Mockito.mock;

class WatchHandlerTest {
    private final EventBusAdapter adapter = mock();
    private final WatchHandler handler = new WatchHandler(adapter);

    @Test
    public void shouldHandleNotification() {
        //TODO implement WatchHandlerTest
    }
}