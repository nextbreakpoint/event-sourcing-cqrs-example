package com.nextbreakpoint.shop.common;

import io.vertx.core.json.JsonObject;
import liquibase.Liquibase;
import liquibase.database.Database;
import liquibase.database.DatabaseFactory;
import liquibase.database.jvm.JdbcConnection;
import liquibase.resource.ClassLoaderResourceAccessor;

import java.sql.Driver;
import java.util.Properties;

public class LiquibaseMigrationManager {
    private LiquibaseMigrationManager() {}

    public static void migrateDatabase(JsonObject config) throws Exception {
        final String jdbcUrl = config.getString("jdbc_url");
        final String jdbcDriver = config.getString("jdbc_driver");
        final String jdbcUsername = config.getString("jdbc_liquibase_username");
        final String jdbcPassword = config.getString("jdbc_liquibase_password");
        final Driver driver = (Driver)Class.forName(jdbcDriver).newInstance();
        final Properties info = new Properties();
        info.setProperty("user", jdbcUsername);
        info.setProperty("password", jdbcPassword);
        final Database database = DatabaseFactory.getInstance().findCorrectDatabaseImplementation(new JdbcConnection(driver.connect(jdbcUrl, info)));
        Liquibase liquibase = new Liquibase("changelog.json", new ClassLoaderResourceAccessor(), database);
        liquibase.update("");
    }
}
