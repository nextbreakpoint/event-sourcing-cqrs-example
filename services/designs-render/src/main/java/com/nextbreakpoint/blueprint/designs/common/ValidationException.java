package com.nextbreakpoint.blueprint.designs.common;

import java.util.ArrayList;
import java.util.List;

public class ValidationException extends Exception {
    private final List<String> errors;

    public ValidationException(String message, List<String> errors) {
        super(message);
        this.errors = errors;
    }

    public ValidationException(String message, List<String> errors, Throwable cause) {
        super(message, cause);
        this.errors = errors;
    }

    public List<String> getErrors() {
        return new ArrayList<>(errors);
    }

    @Override
    public String getMessage() {
        return super.getMessage() + ": " + String.join(", ", errors);
    }
}
