package com.nextbreakpoint.shop.common;

public class Failure extends RuntimeException {
    private final int statusCode;

    public Failure(int statusCode, String message) {
        super(message);
        this.statusCode = statusCode;
    }

    public int getStatusCode() {
        return statusCode;
    }

    public static Failure authenticationError(Throwable err) {
        return new Failure(500, "Failed to authenticate user");
    }

    public static Failure requestFailed(Throwable err) {
        return new Failure(500, "Failed to process request");
    }

    public static Failure accessDenied() {
        return new Failure(403, "Access denied");
    }

    public static Failure badRequest() {
        return new Failure(400, "Bad request");
    }

    public static Failure notFound() {
        return new Failure(404, "Not found");
    }

    public static Failure databaseError(Throwable err) {
        return new Failure(500, "Database error");
    }
}
