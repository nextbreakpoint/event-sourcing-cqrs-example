package com.nextbreakpoint.blueprint.accounts.operations.delete;

import com.nextbreakpoint.blueprint.common.core.Mapper;
import io.vertx.core.json.JsonObject;

public class DeleteAccountResponseMapper implements Mapper<DeleteAccountResponse, String> {
    @Override
    public String transform(DeleteAccountResponse response) {
        return new JsonObject()
                .put("uuid", response.getUuid().toString())
                .encode();
    }
}
