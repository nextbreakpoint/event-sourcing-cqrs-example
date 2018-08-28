package com.nextbreakpoint.shop.designs.controllers.changed;

import com.nextbreakpoint.shop.common.model.Controller;
import com.nextbreakpoint.shop.common.model.events.DesignChangedEvent;
import rx.Single;

public class DesignChangedController implements Controller<DesignChangedEvent, DesignChangedEvent> {
    public DesignChangedController() {
    }

    @Override
    public Single<DesignChangedEvent> onNext(DesignChangedEvent event) {
        return Single.just(event);
    }
}
