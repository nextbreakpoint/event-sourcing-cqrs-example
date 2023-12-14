package com.nextbreakpoint.blueprint.designs.common;

import com.nextbreakpoint.nextfractal.core.common.Bundle;
import com.nextbreakpoint.nextfractal.core.common.SourceError;
import com.nextbreakpoint.nextfractal.mandelbrot.dsl.DSLCompiler;
import com.nextbreakpoint.nextfractal.mandelbrot.dsl.DSLParser;
import com.nextbreakpoint.nextfractal.mandelbrot.dsl.ParserResult;

import java.util.List;

public class BundleValidator {
    public void parseAndCompile(String manifest, String metadata, String script) throws BundleValidatorException {
        try {
            final Bundle bundle = BundleUtils.createBundle(manifest, metadata, script).orThrow();

            final String packageName = DSLParser.class.getPackage().getName() + ".generated";
            final String className = "Compile" + System.nanoTime();
            final DSLParser parser = new DSLParser(packageName, className);

            final ParserResult result = parser.parse(bundle.getSession().getScript());

            if (result.getErrors().isEmpty()) {
                DSLCompiler compiler = new DSLCompiler();
                compiler.compileOrbit(result).create();
                compiler.compileColor(result).create();
            } else {
                final List<String> errors = result.getErrors().stream().map(this::formatError).toList();
                throw new BundleValidatorException("Bundle not valid", errors);
            }
        } catch (Exception e) {
            throw new BundleValidatorException("Bundle not valid", List.of(e.getMessage()), e);
        }
    }

    private String formatError(SourceError error) {
        return "[%d:%d] %s".formatted(error.getLine(), error.getCharPositionInLine(), error.getMessage());
    }
}
