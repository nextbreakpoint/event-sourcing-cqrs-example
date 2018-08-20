package com.nextbreakpoint.shop.accounts.controllers.load;

import com.nextbreakpoint.shop.accounts.model.LoadAccountResponse;
import com.nextbreakpoint.shop.common.model.Content;
import com.nextbreakpoint.shop.common.model.Mapper;
import io.vertx.core.json.JsonObject;

public class LoadAccountResponseMapper implements Mapper<LoadAccountResponse, Content> {
    @Override
    public Content transform(LoadAccountResponse response) {
        final String json = response.getAccount()
                .map(Account -> new JsonObject()
                        .put("uuid", Account.getUuid())
                        .put("name", Account.getName())
                        .put("role", Account.getRole())
                        .encode())
                .orElse(null);

        return new Content(json);
    }
}
