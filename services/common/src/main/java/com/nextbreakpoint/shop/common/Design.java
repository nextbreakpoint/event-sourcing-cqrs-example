package com.nextbreakpoint.shop.common;

import com.fasterxml.jackson.annotation.JsonCreator;

public class Design {
    private final String uuid;
    private final String location;
    private final String imageSrc;
    private final String created;
    private final String modified;
    private final String manifest;
    private final String metadata;
    private final String script;

    @JsonCreator
    public Design(String uuid, String location, String imageSrc, String created, String modified, String manifest, String metadata, String script) {
        this.uuid = uuid;
        this.location = location;
        this.imageSrc = imageSrc;
        this.created = created;
        this.modified = modified;
        this.manifest = manifest;
        this.metadata = metadata;
        this.script = script;
    }

    public String getUuid() {
        return uuid;
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

    public String getCreated() {
        return created;
    }

    public String getModified() {
        return modified;
    }
}
