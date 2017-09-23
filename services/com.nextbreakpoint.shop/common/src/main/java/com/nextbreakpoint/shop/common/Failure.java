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

    public static Failure accessDenied() {
        return new Failure(403, "Access denied");
    }

    public static Failure badRequest() {
        return new Failure(400, "Bad request");
    }

    public static Failure notFound() {
        return new Failure(404, "Not found");
    }

    public static Failure authenticationError(Throwable err) {
        err.printStackTrace();
        return new Failure(500, "Failed to authenticate user");
    }

    public static Failure databaseError(Throwable err) {
        err.printStackTrace();
        return new Failure(500, "Failed to execute statement");
    }

    public static Failure requestFailed(Throwable err) {
        err.printStackTrace();
        return new Failure(500, "Failed to process request");
    }
}
