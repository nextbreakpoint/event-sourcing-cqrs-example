package com.nextbreakpoint.shop.common;

@FunctionalInterface
public interface MessageHandler {
    void onNext(Message message);
}
