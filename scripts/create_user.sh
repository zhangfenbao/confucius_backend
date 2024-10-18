#!/bin/bash

# Load environment variables from .env file
ENV_FILE="server/.env"

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


# Prompt for username
while true; do
    read -p "Enter a username (more than 3 characters): " username
    if [[ ${#username} -gt 2 ]]; then
        break
    else
        echo "Username must be more than 3 characters. Please try again."
    fi
done

# Prompt for password
while true; do
    read -s -p "Enter a password (more than 8 characters): " password
    echo
    if [[ ${#password} -gt 7 ]]; then
        break
    else
        echo "Password must be more than 8 characters. Please try again."
    fi
done

# Confirm the password
while true; do
    read -s -p "Confirm your password: " password_confirm
    echo
    if [ "$password" == "$password_confirm" ]; then
        break
    else
        echo "Passwords do not match. Please try again."
    fi
done

user_id=$(openssl rand -base64 32 | tr -dc 'a-zA-Z0-9')
password_hash=$(python3 -c "import sys; from argon2 import PasswordHasher; ph = PasswordHasher(); print(ph.hash(sys.argv[1]))" "$password")

# Execute SQL command to create a new user
psql "$SESAME_DATABASE_ADMIN_URL" <<EOF
INSERT INTO users (user_id, username, password_hash)
VALUES ('$user_id', '$username', '$password_hash');
EOF

# Check the result of the command
if [ $? -eq 0 ]; then
    echo "User successfully created!"
else
    echo "Error creating user."
    exit 1
fi

echo "--------------------------------------------------"
echo -e "\033[32mYour user credentials:"
echo -e "Username: ${username}"
echo -e "Password: ${password}"
echo "--------------------------------------------------"
echo -e "\033[31mPlease make sure to keep these credentials safe. You will not be able to recover them later.\033[0m"