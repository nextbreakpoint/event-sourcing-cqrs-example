package com.nextbreakpoint.shop.accounts.delete;

import com.nextbreakpoint.shop.common.ResponseMapper;
import com.nextbreakpoint.shop.common.Result;
import io.vertx.core.json.JsonObject;

public class DeleteAccountResponseMapper implements ResponseMapper<DeleteAccountResponse> {
    @Override
    public Result apply(DeleteAccountResponse response) {
        final String json = new JsonObject()
                .put("uuid", response.getUuid().toString())
                .encode();

        return new Result(json);
    }
}
