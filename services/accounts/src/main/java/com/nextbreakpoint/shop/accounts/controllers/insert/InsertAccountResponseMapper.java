package com.nextbreakpoint.shop.accounts.controllers.insert;

import com.nextbreakpoint.shop.accounts.model.InsertAccountResponse;
import com.nextbreakpoint.shop.common.model.Mapper;
import io.vertx.core.json.JsonObject;

public class InsertAccountResponseMapper implements Mapper<InsertAccountResponse, String> {
    @Override
    public String transform(InsertAccountResponse response) {
        final String json = new JsonObject()
                .put("uuid", response.getUuid().toString())
                .put("role", response.getRole())
                .encode();

        return json;
    }
}
