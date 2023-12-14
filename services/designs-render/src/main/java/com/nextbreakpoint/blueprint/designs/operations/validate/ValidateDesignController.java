package com.nextbreakpoint.blueprint.designs.operations.validate;

import com.nextbreakpoint.Try;
import com.nextbreakpoint.blueprint.common.core.Controller;
import com.nextbreakpoint.blueprint.common.core.ValidationStatus;
import com.nextbreakpoint.blueprint.designs.common.BundleUtils;
import com.nextbreakpoint.nextfractal.core.common.Bundle;
import com.nextbreakpoint.nextfractal.mandelbrot.dsl.DSLCompiler;
import com.nextbreakpoint.nextfractal.mandelbrot.dsl.DSLParser;
import com.nextbreakpoint.nextfractal.mandelbrot.dsl.ParserResult;
import rx.Single;

import java.util.List;
import java.util.stream.Collectors;

public class ValidateDesignController implements Controller<ValidateDesignRequest, ValidateDesignResponse> {
    @Override
    public Single<ValidateDesignResponse> onNext(ValidateDesignRequest request) {
        try {
            final Try<Bundle, Exception> bundle = BundleUtils.createBundle(request.getManifest(), request.getMetadata(), request.getScript());

            final DSLParser parser = new DSLParser(DSLParser.class.getPackage().getName() + ".generated", "Compile" + System.nanoTime());

            final ParserResult result = parser.parse(bundle.orThrow().getSession().getScript());

            if (result.getErrors().isEmpty()) {
                DSLCompiler compiler = new DSLCompiler();
                compiler.compileOrbit(result).create();
                compiler.compileColor(result).create();
            }

            final List<String> errors = result.getErrors().stream()
                    .map(sourceError -> String.format("[%d:%d] %s", sourceError.getLine(), sourceError.getCharPositionInLine(), sourceError.getMessage()))
                    .collect(Collectors.toList());

            final ValidateDesignResponse response = ValidateDesignResponse.builder()
                    .withStatus(errors.isEmpty() ? ValidationStatus.ACCEPTED : ValidationStatus.REJECTED)
                    .withErrors(errors)
                    .build();

            return Single.just(response);
        } catch (Exception e) {
            final ValidateDesignResponse response = ValidateDesignResponse.builder()
                    .withStatus(ValidationStatus.REJECTED)
                    .withErrors(List.of(e.getMessage()))
                    .build();

            return Single.just(response);
        }
    }
}
