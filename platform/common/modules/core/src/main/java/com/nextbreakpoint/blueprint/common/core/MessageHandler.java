package com.nextbreakpoint.blueprint.common.core;

@FunctionalInterface
public interface MessageHandler {
    void onNext(Message message);
}
