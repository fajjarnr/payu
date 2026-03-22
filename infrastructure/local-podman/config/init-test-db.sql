-- PayU Test Database Initialization
-- Creates isolated test databases for all microservices
-- Note: This script runs via Docker entrypoint (not psql), so no \c commands

-- Create All Test Databases
CREATE DATABASE keycloak_test;
CREATE DATABASE payu_test_account;
CREATE DATABASE payu_test_auth;
CREATE DATABASE payu_test_transaction;
CREATE DATABASE payu_test_wallet;
CREATE DATABASE payu_test_notification;
CREATE DATABASE payu_test_billing;
CREATE DATABASE payu_test_kyc;
CREATE DATABASE payu_test_compliance;
CREATE DATABASE payu_test_bifast;
CREATE DATABASE payu_test_dukcapil;
CREATE DATABASE payu_test_qris;
CREATE DATABASE payu_test_investment;
CREATE DATABASE payu_test_lending;
CREATE DATABASE payu_test_backoffice;
CREATE DATABASE payu_test_partner;
CREATE DATABASE payu_test_promotion;
CREATE DATABASE payu_test_support;
CREATE DATABASE payu_test_statement;
CREATE DATABASE payu_test_fx;
CREATE DATABASE payu_test_cms;
CREATE DATABASE payu_test_ab_testing;
CREATE DATABASE payu_test_api_portal;
CREATE DATABASE payu_test_gateway;

-- Grant All Privileges to Test User (payu_test)
GRANT ALL PRIVILEGES ON DATABASE keycloak_test TO payu_test;
GRANT ALL PRIVILEGES ON DATABASE payu_test_account TO payu_test;
GRANT ALL PRIVILEGES ON DATABASE payu_test_auth TO payu_test;
GRANT ALL PRIVILEGES ON DATABASE payu_test_transaction TO payu_test;
GRANT ALL PRIVILEGES ON DATABASE payu_test_wallet TO payu_test;
GRANT ALL PRIVILEGES ON DATABASE payu_test_notification TO payu_test;
GRANT ALL PRIVILEGES ON DATABASE payu_test_billing TO payu_test;
GRANT ALL PRIVILEGES ON DATABASE payu_test_kyc TO payu_test;
GRANT ALL PRIVILEGES ON DATABASE payu_test_compliance TO payu_test;
GRANT ALL PRIVILEGES ON DATABASE payu_test_bifast TO payu_test;
GRANT ALL PRIVILEGES ON DATABASE payu_test_dukcapil TO payu_test;
GRANT ALL PRIVILEGES ON DATABASE payu_test_qris TO payu_test;
GRANT ALL PRIVILEGES ON DATABASE payu_test_investment TO payu_test;
GRANT ALL PRIVILEGES ON DATABASE payu_test_lending TO payu_test;
GRANT ALL PRIVILEGES ON DATABASE payu_test_backoffice TO payu_test;
GRANT ALL PRIVILEGES ON DATABASE payu_test_partner TO payu_test;
GRANT ALL PRIVILEGES ON DATABASE payu_test_promotion TO payu_test;
GRANT ALL PRIVILEGES ON DATABASE payu_test_support TO payu_test;
GRANT ALL PRIVILEGES ON DATABASE payu_test_statement TO payu_test;
GRANT ALL PRIVILEGES ON DATABASE payu_test_fx TO payu_test;
GRANT ALL PRIVILEGES ON DATABASE payu_test_cms TO payu_test;
GRANT ALL PRIVILEGES ON DATABASE payu_test_ab_testing TO payu_test;
GRANT ALL PRIVILEGES ON DATABASE payu_test_api_portal TO payu_test;
GRANT ALL PRIVILEGES ON DATABASE payu_test_gateway TO payu_test;
