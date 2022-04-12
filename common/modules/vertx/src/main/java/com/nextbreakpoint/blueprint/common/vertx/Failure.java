package com.nextbreakpoint.blueprint.common.vertx;

import lombok.extern.log4j.Log4j2;

@Log4j2
public class Failure extends RuntimeException {
    private final int statusCode;

    public Failure(int statusCode, String message) {
        super(message);
        this.statusCode = statusCode;
    }

    public int getStatusCode() {
        return statusCode;
    }

    public static Failure accessDenied(String reason) {
        return new Failure(403, "Access denied: " + reason);
    }

    public static Failure badRequest() {
        return new Failure(400, "Bad request");
    }

    public static Failure notFound() {
        return new Failure(404, "Not found");
    }

    public static Failure authenticationError(Throwable err) {
        log.error("An error occurred while authenticating user", err);

        return new Failure(500, "Failed to authenticate user");
    }

    public static Failure databaseError(Throwable err) {
        log.error("An error occurred while accessing database", err);

        return new Failure(500, "Failed to execute statement");
    }

    public static Failure requestFailed(Throwable err) {
        log.error("An error occurred while processing request", err);

        return new Failure(500, "Failed to process request");
    }
}
