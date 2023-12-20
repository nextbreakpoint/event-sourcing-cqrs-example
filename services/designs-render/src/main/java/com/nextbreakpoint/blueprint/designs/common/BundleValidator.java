package com.nextbreakpoint.blueprint.designs.common;

import com.nextbreakpoint.Try;
import com.nextbreakpoint.nextfractal.core.common.Bundle;
import com.nextbreakpoint.nextfractal.core.common.SourceError;
import com.nextbreakpoint.nextfractal.mandelbrot.dsl.DSLCompiler;
import com.nextbreakpoint.nextfractal.mandelbrot.dsl.DSLParser;
import com.nextbreakpoint.nextfractal.mandelbrot.dsl.ParserResult;
import lombok.extern.log4j.Log4j2;

import java.util.List;

@Log4j2
public class BundleValidator {
    public void parseAndCompile(String manifest, String metadata, String script) throws ValidationException {
        createBundle(manifest, metadata, script)
                .flatMap(BundleValidator::parseBundle)
                .flatMap(BundleValidator::checkResult)
                .flatMap(BundleValidator::compileCode)
                .mapper(BundleValidator::createException)
                .orThrow();
    }

    private static Try<Bundle, Exception> createBundle(String manifest, String metadata, String script) {
        return BundleUtils.createBundle(manifest, metadata, script)
                .mapper(e -> new Exception("Can't create bundle", e));
    }

    private static Try<ParserResult, Exception> parseBundle(Bundle bundle) {
        return Try.of(() -> {
            final String packageName = DSLParser.class.getPackage().getName() + ".generated";
            final String className = "Compile" + System.nanoTime();
            final DSLParser parser = new DSLParser(packageName, className);
            return parser.parse(bundle.getSession().getScript());
        }).mapper(e -> new Exception("Can't parse bundle", e));
    }

    private static Try<ParserResult, Exception> compileCode(ParserResult result) {
        return Try.of(() -> {
            DSLCompiler compiler = new DSLCompiler();
            compiler.compileOrbit(result).create();
            compiler.compileColor(result).create();
            return result;
        }).mapper(e -> new Exception("Can't compile bundle", e));
    }

    private static Try<ParserResult, Exception> checkResult(ParserResult result) {
        if (result.getErrors().isEmpty()) return Try.success(result);
        return Try.failure(new ValidationException("Can't parse bundle", formatErrors(result)));
    }

    private static List<String> formatErrors(ParserResult result) {
        return result.getErrors().stream().map(BundleValidator::formatError).toList();
    }

    private static String formatError(SourceError error) {
        return "[%d:%d] %s".formatted(error.getLine(), error.getCharPositionInLine(), error.getMessage());
    }

    private static ValidationException createException(Exception e) {
        if (e instanceof ValidationException) return (ValidationException) e;
        return new ValidationException(e.getMessage(), List.of(e.getMessage()), e.getCause());
    }
}
