package com.nextbreakpoint.blueprint.designs.common;

import java.util.ArrayList;
import java.util.List;

public class BundleValidatorException extends Exception {
    private final List<String> errors;

    public BundleValidatorException(String message, List<String> errors) {
        super(message);
        this.errors = errors;
    }

    public BundleValidatorException(String message, List<String> errors, Throwable cause) {
        super(message, cause);
        this.errors = errors;
    }

    public List<String> getErrors() {
        return new ArrayList<>(errors);
    }
}
