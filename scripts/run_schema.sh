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

# Check if necessary database vars (for admin user) is set
if [ -z "$SESAME_DATABASE_ADMIN_USER" ] || [ -z "$SESAME_DATABASE_ADMIN_PASSWORD" ] || [ -z "$SESAME_DATABASE_NAME" ] || [ -z "$SESAME_DATABASE_HOST" ] || [ -z "$SESAME_DATABASE_PORT" ]; then
  echo "One or more required environment variables are not set in .env file. Please set them."
  exit 1
fi

SESAME_USER_ROLE=${SESAME_DATABASE_USER:-"sesame"}

# Create a database URL 
SESAME_DATABASE_ADMIN_URL="$SESAME_DATABASE_PROTOCOL://$SESAME_DATABASE_ADMIN_USER:$SESAME_DATABASE_ADMIN_PASSWORD@$SESAME_DATABASE_HOST:$SESAME_DATABASE_PORT/$SESAME_DATABASE_NAME"

# Generate a random password for the anon user
ANON_USER_PASSWORD=$(openssl rand -base64 16 | tr -dc 'a-zA-Z0-9')

if [ -z "$ANON_USER_PASSWORD" ]; then
  echo "Failed to generate password for anon_user."
  exit 1
fi

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
echo -e "SESAME_DATABASE_USER=\"${SESAME_USER_ROLE}\""
echo -e "SESAME_DATABASE_PASSWORD=\"${ANON_USER_PASSWORD}\"\033[0m"
echo "--------------------------------------------------"
