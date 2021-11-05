package com.nextbreakpoint.blueprint.designs.model;

import java.util.Optional;

public class Result {
    private final byte[] image;
    private final Throwable error;

    public static Result of(byte[] image, Throwable error) {
        return new Result(image, error);
    }

    private Result(byte[] image, Throwable error) {
        this.image = image;
        this.error = error;
    }

    public byte[] getImage() {
        return image;
    }

    public Optional<Throwable> getError() {
        return Optional.ofNullable(error);
    }
}
