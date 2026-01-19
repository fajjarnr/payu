-- Create Databases
CREATE DATABASE payu_account;
CREATE DATABASE payu_auth;
CREATE DATABASE payu_transaction;
CREATE DATABASE payu_wallet;
CREATE DATABASE payu_notification;
CREATE DATABASE payu_billing;
CREATE DATABASE keycloak;

-- Create Users (Simplified for dev)
-- User 'payu' is created via POSTGRES_USER env var

GRANT ALL PRIVILEGES ON DATABASE payu_account TO payu;
GRANT ALL PRIVILEGES ON DATABASE payu_auth TO payu;
GRANT ALL PRIVILEGES ON DATABASE payu_transaction TO payu;
GRANT ALL PRIVILEGES ON DATABASE payu_wallet TO payu;
GRANT ALL PRIVILEGES ON DATABASE payu_notification TO payu;
GRANT ALL PRIVILEGES ON DATABASE payu_billing TO payu;
GRANT ALL PRIVILEGES ON DATABASE keycloak TO payu;
