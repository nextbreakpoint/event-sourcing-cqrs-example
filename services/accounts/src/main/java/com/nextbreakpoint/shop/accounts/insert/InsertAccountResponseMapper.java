package com.nextbreakpoint.shop.accounts.insert;

import com.nextbreakpoint.shop.common.ResponseMapper;
import com.nextbreakpoint.shop.common.Result;
import io.vertx.core.json.JsonObject;

public class InsertAccountResponseMapper implements ResponseMapper<InsertAccountResponse> {
    @Override
    public Result apply(InsertAccountResponse response) {
        final String json = new JsonObject()
                .put("uuid", response.getUuid().toString())
                .encode();

        return new Result(json);
    }
}
