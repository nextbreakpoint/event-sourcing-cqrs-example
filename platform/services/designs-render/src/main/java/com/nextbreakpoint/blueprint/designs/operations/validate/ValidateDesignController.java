package com.nextbreakpoint.blueprint.designs.operations.validate;

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
    public ValidateDesignController() {}

    @Override
    public Single<ValidateDesignResponse> onNext(ValidateDesignRequest request) {
        return Single.just(request).flatMap(this::onRequest);
    }

    private Single<ValidateDesignResponse> onRequest(ValidateDesignRequest request) {
        try {
            Bundle bundle = BundleUtils.createBundle(request.getManifest(), request.getMetadata(), request.getScript());

            DSLParser parser = new DSLParser(DSLParser.class.getPackage().getName() + ".generated", "Compile" + System.nanoTime());
            ParserResult result = parser.parse(bundle.getSession().getScript());

            if (result.getErrors().isEmpty()) {
                DSLCompiler compiler = new DSLCompiler();
                compiler.compileOrbit(result).create();
                compiler.compileColor(result).create();
            }

            List<String> errors = result.getErrors().stream()
                    .map(sourceError -> String.format("[%d:%d] %s", sourceError.getLine(), sourceError.getCharPositionInLine(), sourceError.getMessage()))
                    .collect(Collectors.toList());

            return Single.just(new ValidateDesignResponse(errors.isEmpty() ? ValidationStatus.ACCEPTED : ValidationStatus.REJECTED, errors));
        } catch (Exception e) {
            return Single.just(new ValidateDesignResponse(ValidationStatus.REJECTED, List.of(e.getMessage())));
        }
    }
}
