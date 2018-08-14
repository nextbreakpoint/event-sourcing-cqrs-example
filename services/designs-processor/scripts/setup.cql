CREATE KEYSPACE IF NOT EXISTS designs WITH REPLICATION = { 'class' : 'SimpleStrategy', 'replication_factor' : 1 };

CREATE ROLE IF NOT EXISTS admin WITH PASSWORD = 'password' AND LOGIN = true;

CREATE ROLE IF NOT EXISTS designs_admin;

GRANT ALL PERMISSIONS on KEYSPACE designs TO designs_admin;

GRANT designs_admin TO admin;

USE designs;

CREATE TABLE IF NOT EXISTS DESIGNS (
  UUID UUID PRIMARY KEY,
  JSON TEXT,
  CREATED TIMESTAMP,
  UPDATED TIMESTAMP,
);

