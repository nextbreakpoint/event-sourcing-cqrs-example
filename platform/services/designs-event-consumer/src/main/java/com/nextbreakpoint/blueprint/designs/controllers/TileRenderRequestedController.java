package com.nextbreakpoint.blueprint.designs.controllers;

import com.nextbreakpoint.blueprint.common.core.Message;
import com.nextbreakpoint.blueprint.common.vertx.Controller;
import rx.Single;

public class TileRenderRequestedController implements Controller<Message, Void> {
    @Override
    public Single<Void> onNext(Message message) {
        return Single.just(message).map(result -> null);
    }
}
