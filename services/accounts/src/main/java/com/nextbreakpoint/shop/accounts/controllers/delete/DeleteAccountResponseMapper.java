package com.nextbreakpoint.shop.accounts.controllers.delete;

import com.nextbreakpoint.shop.accounts.model.DeleteAccountResponse;
import com.nextbreakpoint.shop.common.model.Mapper;
import io.vertx.core.json.JsonObject;

public class DeleteAccountResponseMapper implements Mapper<DeleteAccountResponse, String> {
    @Override
    public String transform(DeleteAccountResponse response) {
        final String json = new JsonObject()
                .put("uuid", response.getUuid().toString())
                .encode();

        return json;
    }
}
