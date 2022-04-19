package com.nextbreakpoint.blueprint.accounts;

import java.util.UUID;

public interface TestConstants {
    String DATABASE_NAME = "test_accounts";
    String DATABASE_USERNAME = "root";
    String DATABASE_PASSWORD = "password";

    UUID ACCOUNT_UUID = new UUID(1L, 1L);
}
