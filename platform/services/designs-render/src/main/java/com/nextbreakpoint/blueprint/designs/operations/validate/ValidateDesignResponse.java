package com.nextbreakpoint.blueprint.designs.operations.validate;

import com.nextbreakpoint.blueprint.common.core.ValidationStatus;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class ValidateDesignResponse {
    private final ValidationStatus status;
    private final List<String> errors;

    public ValidateDesignResponse(ValidationStatus status, List<String> errors) {
        this.status = Objects.requireNonNull(status);
        this.errors = Objects.requireNonNull(errors);
    }

    public ValidationStatus getStatus() {
        return status;
    }

    public List<String> getErrors() {
        return new ArrayList<>(errors);
    }
}
