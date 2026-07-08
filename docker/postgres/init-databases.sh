#!/bin/bash
set -e

# Each microservice owns its own database (database-per-service pattern),
# even though they all run on a single Postgres instance for this demo.
DATABASES=("auth_db" "employee_db" "leave_db" "payroll_db" "asset_db" "helpdesk_db" "notification_db")

for db in "${DATABASES[@]}"; do
  echo "Creating database: $db"
  psql -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" --dbname postgres <<-EOSQL
    SELECT 'CREATE DATABASE $db OWNER $POSTGRES_USER'
    WHERE NOT EXISTS (SELECT FROM pg_database WHERE datname = '$db')\gexec
EOSQL
done
