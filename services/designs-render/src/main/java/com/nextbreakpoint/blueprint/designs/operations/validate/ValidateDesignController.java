package com.nextbreakpoint.blueprint.designs.operations.validate;

import com.nextbreakpoint.blueprint.common.core.Controller;
import com.nextbreakpoint.blueprint.common.core.ValidationStatus;
import com.nextbreakpoint.blueprint.designs.common.BundleValidator;
import com.nextbreakpoint.blueprint.designs.common.ValidationException;
import lombok.extern.log4j.Log4j2;
import rx.Single;

import java.util.List;
import java.util.Objects;

@Log4j2
public class ValidateDesignController implements Controller<ValidateDesignRequest, ValidateDesignResponse> {
    private final BundleValidator validator;

    public ValidateDesignController(BundleValidator validator) {
        this.validator = Objects.requireNonNull(validator);
    }

    @Override
    public Single<ValidateDesignResponse> onNext(ValidateDesignRequest request) {
        try {
            validator.parseAndCompile(request.getManifest(), request.getMetadata(), request.getScript());

            final ValidateDesignResponse response = ValidateDesignResponse.builder()
                    .withStatus(ValidationStatus.ACCEPTED)
                    .withErrors(List.of())
                    .build();

            return Single.just(response);
        } catch (ValidationException e) {
            log.warn("Data not valid", e);

            final ValidateDesignResponse response = ValidateDesignResponse.builder()
                    .withStatus(ValidationStatus.REJECTED)
                    .withErrors(e.getErrors())
                    .build();

            return Single.just(response);
        }
    }
}
