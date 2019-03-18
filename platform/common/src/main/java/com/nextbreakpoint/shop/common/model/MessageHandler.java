package com.nextbreakpoint.shop.common.model;

@FunctionalInterface
public interface MessageHandler {
    void onNext(Message message);
}
