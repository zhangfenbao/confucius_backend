#!/bin/bash

# Load environment variables from .env file
ENV_FILE="sesame/.env"
SCHEMA_FILE="schema/postgres.sql"

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

DEFAULT_USER="sesame"
SESAME_DATABASE_USER=${SESAME_DATABASE_USER:-$DEFAULT_USER}
SESAME_USER_ROLE=$SESAME_DATABASE_USER


# Create a database URL 
SESAME_DATABASE_ADMIN_URL="$SESAME_DATABASE_PROTOCOL://$SESAME_DATABASE_ADMIN_USER:$SESAME_DATABASE_ADMIN_PASSWORD@$SESAME_DATABASE_HOST:$SESAME_DATABASE_PORT/$SESAME_DATABASE_NAME"

# Query for the actual role name that was created
CREATED_ROLE_USERNAME=$(psql "$SESAME_DATABASE_ADMIN_URL" -t -c "SELECT rolname FROM pg_roles WHERE rolname LIKE '${SESAME_USER_ROLE}%' ORDER BY rolname DESC LIMIT 1;")
echo $CREATED_ROLE_USERNAME

CREATED_ROLE_USERNAME=$(echo $ACTUAL_ROLE | xargs)  # Trim whitespace

if [ -z "$CREATED_ROLE_USERNAME" ]; then
  echo "Warning: Could not find the created role."
  CREATED_ROLE_USERNAME=$SESAME_USER_ROLE
fi

echo "--------------------------------------------------"
echo -e "\033[32mUpdate your sesame/.env to include:"
echo -e "SESAME_DATABASE_USER=\"${CREATED_ROLE_USERNAME}\""
echo -e "SESAME_DATABASE_PASSWORD=\"${ANON_USER_PASSWORD}\"\033[0m"
echo "--------------------------------------------------"
