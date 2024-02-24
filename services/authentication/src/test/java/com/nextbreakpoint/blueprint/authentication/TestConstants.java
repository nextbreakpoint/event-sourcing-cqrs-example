package com.nextbreakpoint.blueprint.authentication;

import java.util.UUID;

public interface TestConstants {
    String OAUTH_TOKEN_PATH = "/login/oauth/access_token";
    String OAUTH_USER_PATH = "/user";

    String ACCOUNTS_PATH = "/v1/accounts";

    UUID ACCOUNT_UUID = new UUID(1L, 1L);
}
