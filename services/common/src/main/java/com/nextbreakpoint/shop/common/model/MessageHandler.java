package com.nextbreakpoint.shop.common.model;

import com.nextbreakpoint.shop.common.model.Message;

@FunctionalInterface
public interface MessageHandler {
    void onNext(Message message);
}
