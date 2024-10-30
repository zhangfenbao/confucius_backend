# Authentication

Out-of-the-box, Open Sesame provides basic username / password authentication. Whilst it adheres to security best practices, we recommend developers implement their own authentication layer in production environments.

Sesame's database schema creates a public `sesame` role with RLS enabled, meaning the database is set up for multi-tenant usage if desirable.

All API requests are authenticated by a user specific token.


### Creating a user

You can create a new user account in the database either directly or by the CLI

```shell
python sesame.py create-user
```

When creating a user manually, we recommend:

- `user_id` should be a secure 32 string. You can generate one like so: `openssl rand -base64 32`

- Encrypting your password with `argon2` or similar. This bash script assumes you have installed the webapp requirements.txt, where `argon2-cffi` is included.

- Password hashes are one way, meaning there is no out-of-the-box recovery path for passwords. You can, of course, reset the user's password by manually editing the argon hash in the database.

### Row-level security

Workspaces and associated objects are secured against a user id. All API routes require a `user_id` set, which can be achieved by passing a valid user session token.

Querying the API without a bearer token or valid authentication key will return an `Unauthorized` response:

```bash
curl -X 'GET' \
'http://127.0.0.1:8000/api/workspaces' \
-H 'accept: application/json' \

>> 403 Error: Forbidden 
>> {
>>  "detail": "Not authenticated"
>> }
```

#### Logging in

After running the Open Sesame app (`python sesame.py run`), you can navigate to the URL shown in the terminal. 

Logging in with your username and password will generate an auth token (if none exist already.)

#### Creating a new token

```bash
curl -X 'POST' \
  'http://localhost:8000/api/users/create_token' \
  -H 'accept: application/json' \
  -H 'Content-Type: application/json' \
  -d '{
  "username": "your-username",
  "password": "your-password"
}'

>>> 200
>>> {
>>>     "success": true,
>>>     "token": "abc..."
>>> }
```

#### Revoking tokens

Tokens for a user can be revoked via the API. You can revoke either all or individual tokens by passing an optional `session_token` property as part of the request payload:

```bash
curl -X 'POST' \
  'http://localhost:8000/api/users/revoke_token' \
  -H 'accept: application/json' \
  -H 'Content-Type: application/json' \
  -d '{
  "user": {
    "username": "your-username",
    "password": "your-password"
  },
  "token": "token-uuid" # Optional (remove to revoke all)
}'

>>> 200
>>> {
>>>   "success": true,
>>>   "message": "All tokens revoked successfully."
>>> }
```
