#!/bin/bash

# Load environment variables from .env file
ENV_FILE="server/.env"
SCHEMA_FILE="database/schema.sql"

# Ensure we have a .env file
if [ -f "$ENV_FILE" ]; then
  export $(grep -v '^#' "$ENV_FILE" | xargs)
else
  echo ".env file not found at $ENV_FILE. Please create it with the necessary DATABASE_URL."
  exit 1
fi

# Check if SESAME_DATABASE_ADMIN_URL (for anon_user) is set
if [ -z "$SESAME_DATABASE_ADMIN_URL" ]; then
  echo "SESAME_DATABASE_ADMIN_URL is not set in .env file. Please set it."
  exit 1
fi

# Generate a random password for the anon user
ANON_USER_PASSWORD=$(openssl rand -base64 16 | tr -dc 'a-zA-Z0-9')

if [ -z "$ANON_USER_PASSWORD" ]; then
  echo "Failed to generate password for anon_user from SESAME_DATABASE_URL."
  exit 1
fi

# Create the SESAME_DATABASE_URL using the new role and password
SESAME_DATABASE_URL=$(echo "$SESAME_DATABASE_ADMIN_URL" | sed -E "s|(postgresql://)[^.]+(\..+)?(:[^@]+)@|\1$SESAME_DATABASE_USER\2:$ANON_USER_PASSWORD@|")

# Run schema with password substitution directly, passing the result to psql
echo "Running schema on $SESAME_DATABASE_ADMIN_URL..."

# Replace the placeholder in the schema file with the generated password
sed "s/%%PASSWORD%%/$ANON_USER_PASSWORD/g; s/%%USER%%/$SESAME_DATABASE_USER/g" "$SCHEMA_FILE" | psql "$SESAME_DATABASE_ADMIN_URL"

# Check the result of the command
if [ $? -eq 0 ]; then
  echo "Schema successfully applied to the database!"
else
  echo "Error applying schema to the database."
  exit 1
fi

echo "--------------------------------------------------"
echo -e "\033[32mUpdate your server/.env to include:"
echo -e "SESAME_DATABASE_URL=${SESAME_DATABASE_URL}\033[0m"
echo "--------------------------------------------------"
