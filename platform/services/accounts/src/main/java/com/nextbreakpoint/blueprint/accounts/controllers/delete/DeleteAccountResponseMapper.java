package com.nextbreakpoint.blueprint.accounts.controllers.delete;

import com.nextbreakpoint.blueprint.accounts.model.DeleteAccountResponse;
import com.nextbreakpoint.blueprint.common.core.Mapper;
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
