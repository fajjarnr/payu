-- PayU Test Database Initialization
-- Creates isolated test databases with test-specific credentials

-- Create Test Databases
CREATE DATABASE keycloak_test;
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

-- Grant Privileges to Test User
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

-- Enable required extensions for testing
\c payu_test_account
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
CREATE EXTENSION IF NOT EXISTS "pgcrypto";

\c payu_test_auth
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
CREATE EXTENSION IF NOT EXISTS "pgcrypto";

\c payu_test_transaction
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
CREATE EXTENSION IF NOT EXISTS "pgcrypto";

\c payu_test_wallet
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
CREATE EXTENSION IF NOT EXISTS "pgcrypto";

\c payu_test_notification
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

\c payu_test_billing
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

\c payu_test_kyc
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
CREATE EXTENSION IF NOT EXISTS "pgcrypto";

\c payu_test_compliance
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

\c payu_test_investment
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
CREATE EXTENSION IF NOT EXISTS "pgcrypto";

\c payu_test_lending
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
CREATE EXTENSION IF NOT EXISTS "pgcrypto";

\c payu_test_statement
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
