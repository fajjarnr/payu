-- Create Databases
-- CREATE DATABASE payu_account; -- Created via POSTGRES_DB env var
CREATE DATABASE payu_auth;
CREATE DATABASE payu_transaction;
CREATE DATABASE payu_wallet;
CREATE DATABASE payu_notification;
CREATE DATABASE payu_billing;
CREATE DATABASE keycloak;
CREATE DATABASE payu_kyc;
CREATE DATABASE payu_analytics;
CREATE DATABASE payu_compliance;
CREATE DATABASE payu_bifast;
CREATE DATABASE payu_dukcapil;
CREATE DATABASE payu_qris;

-- Create Users (Simplified for dev)
-- User 'payu' is created via POSTGRES_USER env var

GRANT ALL PRIVILEGES ON DATABASE payu_account TO payu;
GRANT ALL PRIVILEGES ON DATABASE payu_auth TO payu;
GRANT ALL PRIVILEGES ON DATABASE payu_transaction TO payu;
GRANT ALL PRIVILEGES ON DATABASE payu_wallet TO payu;
GRANT ALL PRIVILEGES ON DATABASE payu_notification TO payu;
GRANT ALL PRIVILEGES ON DATABASE payu_billing TO payu;
GRANT ALL PRIVILEGES ON DATABASE keycloak TO payu;
GRANT ALL PRIVILEGES ON DATABASE payu_kyc TO payu;
GRANT ALL PRIVILEGES ON DATABASE payu_analytics TO payu;
GRANT ALL PRIVILEGES ON DATABASE payu_compliance TO payu;
GRANT ALL PRIVILEGES ON DATABASE payu_bifast TO payu;
GRANT ALL PRIVILEGES ON DATABASE payu_dukcapil TO payu;
GRANT ALL PRIVILEGES ON DATABASE payu_qris TO payu;
