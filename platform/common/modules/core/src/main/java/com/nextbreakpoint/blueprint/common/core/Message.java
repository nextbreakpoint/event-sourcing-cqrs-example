package com.nextbreakpoint.blueprint.common.core;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Objects;

public class Message {
    private String messageId;
    private String messageType;
    private String messageBody;
    private String messageSource;
    private String partitionKey;
    private Long timestamp;

    @JsonCreator
    public Message(@JsonProperty("messageId") String messageId,
                   @JsonProperty("messageType") String messageType,
                   @JsonProperty("messageBody") String messageBody,
                   @JsonProperty("messageSource") String messageSource,
                   @JsonProperty("partitionKey") String partitionKey,
                   @JsonProperty("timestamp") Long timestamp) {
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

    @Override
    public String toString() {
        return "Message{" +
                "messageId='" + messageId + '\'' +
                ", messageType='" + messageType + '\'' +
                ", messageBody='" + messageBody + '\'' +
                ", messageSource='" + messageSource + '\'' +
                ", partitionKey='" + partitionKey + '\'' +
                ", timestamp=" + timestamp +
                '}';
    }
}
