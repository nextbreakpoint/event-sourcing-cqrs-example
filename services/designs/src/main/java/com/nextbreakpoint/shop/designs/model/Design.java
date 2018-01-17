package com.nextbreakpoint.shop.designs.model;

public class Design {
    private String uuid;
    private String json;
    private String created;
    private String updated;

    public Design(String uuid, String json, String created, String updated) {
        this.uuid = uuid;
        this.json = json;
        this.created = created;
        this.updated = updated;
    }

    public String getUuid() {
        return uuid;
    }

    public String getJson() {
        return json;
    }

    public String getCreated() {
        return created;
    }

    public String getUpdated() {
        return updated;
    }
}
