CREATE DATABASE IF NOT EXISTS accounts;

CREATE USER IF NOT EXISTS admin IDENTIFIED BY 'password' PASSWORD EXPIRE NEVER;
CREATE USER IF NOT EXISTS verticle IDENTIFIED BY 'password' PASSWORD EXPIRE NEVER;
GRANT SELECT, INSERT, UPDATE, DELETE ON accounts.* TO admin;
GRANT SELECT, INSERT, UPDATE, DELETE ON accounts.* TO verticle;

USE accounts;

CREATE TABLE IF NOT EXISTS ACCOUNT (
    ACCOUNT_UUID VARCHAR(36) PRIMARY KEY,
    ACCOUNT_NAME VARCHAR(1024) NOT NULL,
    ACCOUNT_LOGIN VARCHAR(1024) NOT NULL,
    ACCOUNT_AUTHORITIES VARCHAR(128) NOT NULL,
    ACCOUNT_CREATED TIMESTAMP NOT NULL
);

FLUSH PRIVILEGES;

CREATE DATABASE IF NOT EXISTS test_accounts;

CREATE USER IF NOT EXISTS admin IDENTIFIED BY 'password' PASSWORD EXPIRE NEVER;
CREATE USER IF NOT EXISTS verticle IDENTIFIED BY 'password' PASSWORD EXPIRE NEVER;
GRANT SELECT, INSERT, UPDATE, DELETE ON test_accounts.* TO admin;
GRANT SELECT, INSERT, UPDATE, DELETE ON test_accounts.* TO verticle;

USE test_accounts;

CREATE TABLE IF NOT EXISTS ACCOUNT (
    ACCOUNT_UUID VARCHAR(36) PRIMARY KEY,
    ACCOUNT_NAME VARCHAR(1024) NOT NULL,
    ACCOUNT_LOGIN VARCHAR(1024) NOT NULL,
    ACCOUNT_AUTHORITIES VARCHAR(128) NOT NULL,
    ACCOUNT_CREATED TIMESTAMP NOT NULL
);

FLUSH PRIVILEGES;
