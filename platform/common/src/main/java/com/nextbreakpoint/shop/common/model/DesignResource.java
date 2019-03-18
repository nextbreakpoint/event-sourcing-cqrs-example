package com.nextbreakpoint.shop.common.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class DesignResource {
    private final String uuid;
    private final String checksum;
    private final String location;
    private final String imageSrc;
    private final String modified;
    private final String manifest;
    private final String metadata;
    private final String script;

    @JsonCreator
    public DesignResource(@JsonProperty("uuid") String uuid,
                          @JsonProperty("checksum") String checksum,
                          @JsonProperty("location") String location,
                          @JsonProperty("imageSrc") String imageSrc,
                          @JsonProperty("modified") String modified,
                          @JsonProperty("manifest") String manifest,
                          @JsonProperty("metadata") String metadata,
                          @JsonProperty("script") String script) {
        this.uuid = uuid;
        this.checksum = checksum;
        this.location = location;
        this.imageSrc = imageSrc;
        this.modified = modified;
        this.manifest = manifest;
        this.metadata = metadata;
        this.script = script;
    }

    public String getUuid() {
        return uuid;
    }

    public String getChecksum() {
        return checksum;
    }

    public String getLocation() {
        return location;
    }

    public String getImageSrc() {
        return imageSrc;
    }

    public String getManifest() {
        return manifest;
    }

    public String getMetadata() {
        return metadata;
    }

    public String getScript() {
        return script;
    }

    public String getModified() {
        return modified;
    }
}
