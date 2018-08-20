package com.nextbreakpoint.shop.common.model;

import java.util.Objects;

public class Message {
    private String messageId;
    private String messageType;
    private String messageBody;
    private String messageSource;
    private String partitionKey;
    private Long timestamp;

    public Message(String messageId, String messageType, String messageBody, String messageSource, String partitionKey, Long timestamp) {
        this.messageId = Objects.requireNonNull(messageId);
        this.messageType = Objects.requireNonNull(messageType);
        this.messageBody = Objects.requireNonNull(messageBody);
        this.messageSource = Objects.requireNonNull(messageSource);
        this.partitionKey = Objects.requireNonNull(partitionKey);
        this.timestamp = Objects.requireNonNull(timestamp);
    }

    public String getMessageId() {
        return messageId;
    }

    public String getMessageType() {
        return messageType;
    }

    public String getMessageBody() {
        return messageBody;
    }

    public String getMessageSource() {
        return messageSource;
    }

    public String getPartitionKey() {
        return partitionKey;
    }

    public Long getTimestamp() {
        return timestamp;
    }
}
