package com.nextbreakpoint.shop.accounts.insert;

import com.nextbreakpoint.shop.common.Content;
import com.nextbreakpoint.shop.common.Mapper;
import io.vertx.core.json.JsonObject;

public class InsertAccountResponseMapper implements Mapper<InsertAccountResponse, Content> {
    @Override
    public Content transform(InsertAccountResponse response) {
        final String json = new JsonObject()
                .put("uuid", response.getUuid().toString())
                .put("role", response.getRole())
                .encode();

        return new Content(json);
    }
}
