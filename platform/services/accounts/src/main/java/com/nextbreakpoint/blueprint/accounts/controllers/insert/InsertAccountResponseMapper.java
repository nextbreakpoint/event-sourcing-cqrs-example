package com.nextbreakpoint.blueprint.accounts.controllers.insert;

import com.nextbreakpoint.blueprint.accounts.model.InsertAccountResponse;
import com.nextbreakpoint.blueprint.common.core.Mapper;
import io.vertx.core.json.JsonObject;

public class InsertAccountResponseMapper implements Mapper<InsertAccountResponse, String> {
    @Override
    public String transform(InsertAccountResponse response) {
        final String json = new JsonObject()
                .put("uuid", response.getUuid().toString())
                .put("role", response.getAuthorities())
                .encode();

        return json;
    }
}
