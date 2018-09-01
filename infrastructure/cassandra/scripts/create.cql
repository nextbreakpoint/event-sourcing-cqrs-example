ALTER KEYSPACE "system_auth" WITH REPLICATION = { 'class' : 'NetworkTopologyStrategy', 'datacenter1' : 3 };

CREATE KEYSPACE IF NOT EXISTS designs WITH REPLICATION = { 'class' : 'NetworkTopologyStrategy', 'datacenter1' : 3 };
CREATE KEYSPACE IF NOT EXISTS accounts WITH REPLICATION = { 'class' : 'NetworkTopologyStrategy', 'datacenter1' : 3 };

CREATE ROLE IF NOT EXISTS shop WITH PASSWORD = 'changeme' AND LOGIN = true;

CREATE ROLE IF NOT EXISTS designs_admin;
CREATE ROLE IF NOT EXISTS accounts_admin;

GRANT ALL PERMISSIONS on KEYSPACE designs TO designs_admin;
GRANT ALL PERMISSIONS on KEYSPACE accounts TO accounts_admin;

GRANT designs_admin TO shop;
GRANT accounts_admin TO shop;

USE designs;

CREATE TABLE IF NOT EXISTS DESIGNS (
  UUID UUID PRIMARY KEY,
  JSON TEXT,
  CREATED TIMESTAMP,
  UPDATED TIMESTAMP,
);

USE accounts;

CREATE TABLE IF NOT EXISTS ACCOUNTS (
  UUID UUID PRIMARY KEY,
  NAME TEXT,
  ROLE TEXT,
  EMAIL TEXT,
  CREATED TIMESTAMP
);

CREATE INDEX IF NOT EXISTS accounts_email_idx ON accounts.ACCOUNTS ( EMAIL );
