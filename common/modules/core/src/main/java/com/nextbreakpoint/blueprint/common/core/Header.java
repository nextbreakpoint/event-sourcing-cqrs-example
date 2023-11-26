package com.nextbreakpoint.blueprint.common.core;

import lombok.Builder;
import lombok.Data;

@Data
@Builder(setterPrefix = "with")
public class Header {
    private final String key;
    private final String value;
}
