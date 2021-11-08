package com.nextbreakpoint.blueprint.common.core;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Objects;

public class Message {
    private String uuid;
    private String type;
    private String body;
    private String source;
    private String partitionKey;
    private Long timestamp;

    @JsonCreator
    public Message(@JsonProperty("uuid") String uuid,
                   @JsonProperty("type") String type,
                   @JsonProperty("body") String body,
                   @JsonProperty("source") String source,
                   @JsonProperty("partitionKey") String partitionKey,
                   @JsonProperty("timestamp") Long timestamp) {
        this.uuid = Objects.requireNonNull(uuid);
        this.type = Objects.requireNonNull(type);
        this.body = Objects.requireNonNull(body);
        this.source = Objects.requireNonNull(source);
        this.partitionKey = Objects.requireNonNull(partitionKey);
        this.timestamp = Objects.requireNonNull(timestamp);
    }

    public String getUuid() {
        return uuid;
    }

    public String getType() {
        return type;
    }

    public String getBody() {
        return body;
    }

    public String getSource() {
        return source;
    }

    public String getPartitionKey() {
        return partitionKey;
    }

    public Long getTimestamp() {
        return timestamp;
    }

    @Override
    public String toString() {
        return "[ " +
                "uuid='" + uuid + '\'' +
                ", partitionKey='" + partitionKey + '\'' +
                ", timestamp=" + timestamp +
                ", type='" + type + '\'' +
                ", source='" + source + '\'' +
                " ]";
    }
}
