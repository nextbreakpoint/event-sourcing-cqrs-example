SELECT 'CREATE USER pactbrokeruser WITH PASSWORD ''password''' WHERE NOT EXISTS (SELECT FROM pg_user WHERE usename = 'pactbrokeruser')\gexec
SELECT 'CREATE DATABASE pactbroker WITH OWNER pactbrokeruser' WHERE NOT EXISTS (SELECT FROM pg_database WHERE datname = 'pactbroker')\gexec
GRANT ALL PRIVILEGES ON DATABASE pactbroker TO pactbrokeruser;
