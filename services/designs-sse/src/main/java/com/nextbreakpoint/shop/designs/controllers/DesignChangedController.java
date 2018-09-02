package com.nextbreakpoint.shop.designs.controllers;

import com.nextbreakpoint.shop.common.model.Controller;
import com.nextbreakpoint.shop.common.model.events.DesignChangedEvent;
import rx.Single;

public class DesignChangedController implements Controller<DesignChangedEvent, DesignChangedEvent> {
    @Override
    public Single<DesignChangedEvent> onNext(DesignChangedEvent event) {
        return Single.just(event);
    }
}
