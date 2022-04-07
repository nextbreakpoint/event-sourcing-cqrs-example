package com.nextbreakpoint.blueprint.designs.operations.validate;

import com.nextbreakpoint.blueprint.common.core.ValidationStatus;
import lombok.Builder;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Data
@Builder(setterPrefix = "with")
public class ValidateDesignResponse {
    private final ValidationStatus status;
    private final List<String> errors;

    public ValidateDesignResponse(ValidationStatus status, List<String> errors) {
        this.status = Objects.requireNonNull(status);
        this.errors = Objects.requireNonNull(errors);
    }

    public List<String> getErrors() {
        return new ArrayList<>(errors);
    }
}
