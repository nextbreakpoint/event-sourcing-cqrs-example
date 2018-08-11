package com.nextbreakpoint.shop.common;

import io.vertx.core.json.JsonObject;
import org.cognitor.cassandra.migration.Database;
import org.cognitor.cassandra.migration.MigrationRepository;
import org.cognitor.cassandra.migration.MigrationTask;

public class CassandraMigrationManager {
    private CassandraMigrationManager() {}

    public static void migrateDatabase(JsonObject config) throws Exception {
        final String keyspaceName = config.getString("cassandra_keyspace");
        Database database = new Database(CassandraClusterFactory.create(config), keyspaceName);
        MigrationTask migration = new MigrationTask(database, new MigrationRepository("database"));
        migration.migrate();
    }
}
