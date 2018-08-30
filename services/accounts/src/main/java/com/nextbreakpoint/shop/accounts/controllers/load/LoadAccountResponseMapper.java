package com.nextbreakpoint.shop.accounts.controllers.load;

import com.nextbreakpoint.shop.accounts.model.LoadAccountResponse;
import com.nextbreakpoint.shop.common.model.Mapper;
import io.vertx.core.json.JsonObject;

import java.util.Optional;

public class LoadAccountResponseMapper implements Mapper<LoadAccountResponse, Optional<String>> {
    @Override
    public Optional<String> transform(LoadAccountResponse response) {
        final Optional<String> json = response.getAccount()
                .map(Account -> new JsonObject()
                        .put("uuid", Account.getUuid())
                        .put("name", Account.getName())
                        .put("role", Account.getRole())
                        .encode());

        return json;
    }
}
