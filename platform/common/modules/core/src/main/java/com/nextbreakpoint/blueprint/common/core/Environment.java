package com.nextbreakpoint.blueprint.common.core;

import java.util.Optional;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Environment {
    private static final String PATTERN = "\\$\\{([a-zA-Z]+[a-zA-Z0-9_]*)}";
    private final Pattern pattern = Pattern.compile(PATTERN);
    private final Function<String, String> getEnv;
    private final Function<String, String> getProperty;
    private final static Environment defaultEnvironment = new Environment(System::getenv, System::getProperty);

    public static Environment getDefaultEnvironment() {
        return defaultEnvironment;
    }

    public static Environment createEnvironment(Function<String, String> getEnv, Function<String, String> getProperty) {
        return new Environment(getEnv, getProperty);
    }

    private Environment(Function<String, String> getEnv, Function<String, String> getProperty) {
        this.getEnv = getEnv;
        this.getProperty = getProperty;
    }

    public String resolve(String input) {
        return Optional.ofNullable(input)
                .map(text -> pattern.matcher(text))
                .filter(Matcher::matches)
                .map(matcher -> matcher.group(1))
                .map(this::evaluate)
                .orElse(input);
    }

    private String evaluate(String expression) {
        final String value = getEnv.apply(expression.toUpperCase());
        return value != null ? value : getProperty.apply(expression.toLowerCase());
    }
}
