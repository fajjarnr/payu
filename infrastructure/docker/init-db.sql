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
CREATE DATABASE payu_investment;
CREATE DATABASE payu_lending;
CREATE DATABASE payu_backoffice;
CREATE DATABASE payu_partner;
CREATE DATABASE payu_promotion;
CREATE DATABASE payu_support;
CREATE DATABASE payu_statement;

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
GRANT ALL PRIVILEGES ON DATABASE payu_investment TO payu;
GRANT ALL PRIVILEGES ON DATABASE payu_lending TO payu;
GRANT ALL PRIVILEGES ON DATABASE payu_backoffice TO payu;
GRANT ALL PRIVILEGES ON DATABASE payu_partner TO payu;
GRANT ALL PRIVILEGES ON DATABASE payu_promotion TO payu;
GRANT ALL PRIVILEGES ON DATABASE payu_support TO payu;
GRANT ALL PRIVILEGES ON DATABASE payu_statement TO payu;
