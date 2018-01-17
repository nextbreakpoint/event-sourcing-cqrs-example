package com.nextbreakpoint.shop.accounts.load;

import com.nextbreakpoint.shop.common.ResponseMapper;
import com.nextbreakpoint.shop.common.Result;
import io.vertx.core.json.JsonObject;

public class LoadAccountResponseMapper implements ResponseMapper<LoadAccountResponse> {
    @Override
    public Result apply(LoadAccountResponse response) {
        final String json = response.getAccount()
                .map(Account -> new JsonObject()
                        .put("uuid", Account.getUuid())
                        .put("name", Account.getName())
                        .put("role", Account.getRole())
                        .encode())
                .orElse(null);

        return new Result(json);
    }
}
