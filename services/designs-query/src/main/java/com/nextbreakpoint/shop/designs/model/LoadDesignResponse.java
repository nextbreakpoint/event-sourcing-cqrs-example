package com.nextbreakpoint.shop.designs.model;

import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

public class LoadDesignResponse {
    private final UUID uuid;
    private final DesignDocument designDocument;

    public LoadDesignResponse(UUID uuid, DesignDocument designDocument) {
        this.uuid = Objects.requireNonNull(uuid);
        this.designDocument = designDocument;
    }

    public UUID getUuid() {
        return uuid;
    }

    public Optional<DesignDocument> getDesignDocument() {
        return Optional.ofNullable(designDocument);
    }
}
