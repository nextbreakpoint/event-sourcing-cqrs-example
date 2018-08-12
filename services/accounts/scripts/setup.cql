CREATE KEYSPACE IF NOT EXISTS accounts WITH REPLICATION = { 'class' : 'SimpleStrategy', 'replication_factor' : 1 };

CREATE ROLE IF NOT EXISTS admin WITH PASSWORD = 'password' AND LOGIN = true;

CREATE ROLE IF NOT EXISTS accounts_admin;

GRANT ALL PERMISSIONS on KEYSPACE accounts TO accounts_admin;

GRANT accounts_admin TO admin;

USE accounts;

CREATE TABLE IF NOT EXISTS ACCOUNTS (
  UUID UUID PRIMARY KEY,
  NAME TEXT,
  ROLE TEXT,
  EMAIL TEXT,
  CREATED TIMESTAMP
);

CREATE INDEX IF NOT EXISTS accounts_email_idx ON accounts.ACCOUNTS ( EMAIL );
