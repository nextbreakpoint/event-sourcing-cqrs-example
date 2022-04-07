package com.nextbreakpoint.blueprint.accounts.operations.load;

import com.nextbreakpoint.blueprint.accounts.model.Account;
import com.nextbreakpoint.blueprint.common.core.Mapper;
import io.vertx.core.json.JsonObject;

import java.util.Optional;

public class LoadAccountResponseMapper implements Mapper<LoadAccountResponse, Optional<String>> {
    @Override
    public Optional<String> transform(LoadAccountResponse response) {
        return response.getAccount().map(account -> makeAccount(account).encode());
    }

    private JsonObject makeAccount(Account account) {
        return new JsonObject()
                        .put("uuid", account.getUuid())
                        .put("name", account.getName())
                        .put("role", account.getAuthorities());
    }
}
