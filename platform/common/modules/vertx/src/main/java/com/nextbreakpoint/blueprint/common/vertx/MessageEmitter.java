package com.nextbreakpoint.blueprint.common.vertx;

import com.nextbreakpoint.blueprint.common.core.OutputMessage;
import rx.Single;

public interface MessageEmitter {
    Single<Void> send(OutputMessage message);

    Single<Void> send(OutputMessage message, String topicName);

    String getTopicName();
}
