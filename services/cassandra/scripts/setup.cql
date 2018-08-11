CREATE KEYSPACE IF NOT EXISTS designs WITH REPLICATION = { 'class' : 'SimpleStrategy', 'replication_factor' : 1 };
CREATE KEYSPACE IF NOT EXISTS accounts WITH REPLICATION = { 'class' : 'SimpleStrategy', 'replication_factor' : 1 };

CREATE ROLE IF NOT EXISTS admin WITH PASSWORD = 'password' AND LOGIN = true;

CREATE ROLE IF NOT EXISTS designs_admin;
CREATE ROLE IF NOT EXISTS accounts_admin;

GRANT ALL PERMISSIONS on KEYSPACE designs TO designs_admin;
GRANT ALL PERMISSIONS on KEYSPACE accounts TO accounts_admin;

GRANT designs_admin TO admin;
GRANT accounts_admin TO admin;

DROP ROLE IF EXISTS cassandra;
