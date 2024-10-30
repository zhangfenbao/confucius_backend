<img alt="open-sesame" width="480px" height="auto" src="./docs/sesame-banner.png">

Open Source multi-modal LLM environment. Host your own web and mobile chat interface, powered by real-time bots that provide best-in-class voice AI functionality with an emphasis on:

- Using any model for any conversation thread
- Data ownership
- Great voice integration
- Open source and extensibility

## Table of contents

- [Quickstart](#quickstart)
- [CLI commands](#the-open-sesame-cli)
- [Overview](#overview)
  - [Database setup](#database-setup)
  - [Webapp and workspaces](#run-the-webapp-server-and-create-a-workspace)
- [Create your first workspace](#create-your-first-workspace)
- [Run a client app](#run-a-client-app)
- [Deployment](#deployment)
  - [Modal.com](#deploy-server-to-modalcom)
- [Core technologies](#core-technologies)

## Quickstart

To run Open Sesame locally, you will need:

- Python 3.10 or higher.
- Database that supports async sessions.
- Service provider API keys for a large-language, text-to-speech, and speech-to-text provider.

### 1. Install project dependencies

```shell
python -m venv venv
source venv/bin/activate # ... or OS specific activation
pip install -r sesame/dev-requirements.txt
```

**The main project files are found in the `/sesame` subdirectory. Please navigate to that folder to continue project setup**

```shell
cd sesame/
```

### 2. Create your enviroment `.env`

Run the Open Sesame CLI `init` command:

```shell
python sesame.py init
```

Follow the prompts for creating your `.env` file and optionally configuring your database.

This command will create a new .env file in the project folder. You can do this manually by copying the `sesame/env.example` to `sesame/.env` and configuring each of the environment variables.

### 3. Configure your database

If you chose to skip the database configuration step as part of the `sesame.py init` command, you step through this with:

```shell
python sesame.py init-db
```

Ensure you have set the following in your `.env` file:

```shell
SESAME_DATABASE_ADMIN_USER="postgres"
SESAME_DATABASE_ADMIN_PASSWORD=""
SESAME_DATABASE_NAME="postgres"
SESAME_DATABASE_HOST="localhost"
# Use a session port (typically 5432)
SESAME_DATABASE_PORT="5432"
# Public database role credentials
SESAME_DATABASE_USER="sesame"
SESAME_DATABASE_PASSWORD="some-strong-password"
```

You can test your database credentials:

```shell
python sesame test-db --admin
# Note: --admin specifies to test using the admi credentials. The efault user role has not yet been created
```

#### Run the schema

Schema files for various database engines can be found in the `schema` folder at the root of this project. 

Let's run the PostgreSQL schema:

```shell
python sesame.py run-schema
```

This command will test your database admin credentials first and create the public user role. If you'd like to run the schema manually (not using the CLI), be sure to replace `%%USER%%` and `%%PASSWORD%%` in the schema file to align to your `SESAME_DATABASE_USER` and `SESAME_DATABASE_PASSWORD` respectively.

You should now have a public user role. You can test this with:

```shell
python sesame.py test-db
```

### 4. Create a user

The Open Sesame schema defaults to having row level security enabled, meaning you must have at least one user created in the database.

```shell
python sesame.py create-user
```

You can use this command to create new user accounts as you need them.


### 5. Run the app!

Your Open Sesame instance is now configured.

Run the application:

```shell
python sesame.py run
```

#### Something went wrong?

- Check all the necessary settings are configured in your `.env`
- Ensure the database credentials are accurate: `python sesame.py test-db`
- Run the included tests for more details: `PYTHONPATH=. pytest tests/ -s -v`


To manually run the FastAPI server:

```shell
python -m uvicorn webapp.main:app --reload
```

---

## Open Sesame CLI

`sesame.py` provides convenience when configuring and running your instance. Here are the currently supported commands:

```shell
# Show available commands
python sesame.py --help 

# Create .env file from template
python sesame.py init
# Configure database credentials in .env
python sesame.py init-db

# Test admin database credentials
python sesame.py test-db  --admin   # Admin role
python sesame.py test-db            # User role

# Run required schema
python sesame.py run-schema

# Create user
python sesame.py create-user
python sesame.py create-user -u user -p pass

# Run the FastaPI app
python sesame.py run
```

---

#### 1. Install project dependencies

```shell
python -m venv venv
source venv/bin/activate # ... or OS specific activation
pip install -r sesame/dev-requirements.txt
```

#### 2. Create a database

Sesame uses data storage for users, workspace settings and conversation history. The current default schema assumes Postgres.

Create a new local database: `psql -U postgres -c "CREATE DATABASE sesame;"`

... or alternatively, use a hosted database provider such as [Render](www.render.com) or [Supabase](www.supabase.com).


#### 3. Create a local .env

```shell
cp sesame/env.example sesame/.env
```

You must set the following:

```bash
SESAME_APP_SECRET # For salting secrets during encryption
SESAME_DATABASE_ADMIN_USER # Privileged database user name
SESAME_DATABASE_ADMIN_PASSWORD # Privileged user password
SESAME_DATABASE_NAME # Name of database, e.g. sesame
SESAME_DATABASE_HOST # Database host address, e.g. localhost
SESAME_DATABASE_PORT # Database port E.g. 5432 (should allow asynchronous session pooling)
```

_Note: Your database must support asyncpg or equivalent asychnronous driver._

> 🛑 Ensure you are using a database that accepts session mode typically available on port `5432`. If you are using Supabase, the URL provided in the settings panel defaults to "transaction mode". See [Database setup](#database-setup) for details.

#### 4. Create database roles and schema

From the root of the project, run the schema script found in [scripts/run_shema.sh](./scripts/run_schema.sh)

```shell
bash scripts/run_schema.sh
```

Note: the `run_schema.sh` script requires Postgres to run. Install the necessary package for your system (e.g. `brew install postgresql` for MacOS).

If the schema runs correctly, the script will print out a non-superuser user and password.

Edit `SESAME_DATABASE_USER` and `SESAME_DATABASE_PASSWORD` in `sesame/.env` with the output of the script.

For more information about database configuration, read [here](#database-setup)

#### 5. Create a user

From the root of the project still, create a user account and password from [scripts/create_user.sh](./scripts/create_user.sh).

```shell
bash scripts/create_user.sh
```

Running this script will create a user account in your database. Make a note of your username and password; the password will be encrypted and not recoverable later.

#### 6. Run the Sesame webapp and generate access token

```shell
cd sesame/
uvicorn webapp.main:app --reload
```

You should see a URL in your terminal window to visit, for example `http://127.0.0.1:8000`. Navigating to this URL should reveal the Sesame dashboard, or an error message if something went wrong.

<img alt="open-sesame-dashboard" width="280px" height="auto" src="./docs/sesame-dashboard.png">

Log in with the user name and password you set in step 5.

Now, create a new access token to authenticate web requests in any of the Open Sesame clients. For more information, see [authentication](./docs/authentication.md).

#### 7. Run the tests to check your configuration

```bash
cd sesame/
PYTHONPATH=. pytest tests/ -s -v
```

#### 8. Create your first workspace

Follow the [workspace creation steps](#create-your-first-workspace), and run a [client](#run-a-client-app) of your choosing.


## Overview

### Project requirements

- Python 3.10 or higher.
- Database that supports async sessions (we recommend [Supabase](https://supabase.com/).)
- Deployment target that can run Python processes (we recommend [Modal](https://www.modal.com).)
- API keys for support transport, llm, tts, and stt services.

_Note: Sesame bots are configured to use [Daily](https://www.daily.co) as a transport layer when using realtime voice mode. You must provide a valid Daily API key to your workspace configuration in order to use voice mode_

### Database setup

Sesame requires a Postgres database (support for other database types, such as SQLite, coming soon). You may need to install additional extensions specified in [database/schema.sql](./database/schema.sql).

#### 1. Update your .env

Update the non-optional `SESAME_DATABASE_*` variables in `sesame/.env`.

For Supabase, for example, you can find credentials URL here: https://supabase.com/dashboard/project/[YOUR_PROJECT]/settings/database. **Note: be sure to use the URI for `Mode: session` to get the correct port for session mode.**

![](./docs/supabaseurl.png)

Your database credentials should look something like this:

```bash
SESAME_DATABASE_ADMIN_USER="postgres"
SESAME_DATABASE_ADMIN_PASSWORD="password"
SESAME_DATABASE_NAME="postgres"
SESAME_DATABASE_HOST="region.pooler.supabase.com"
# - Use a session port (typically 5432)
SESAME_DATABASE_PORT=5432
```

#### 2. Apply database schema

Run the schema `bash scripts/run_schema.sh` (or copy and paste into your SQL Editor of choice).

This script will create the necessary tables, functions and triggers, as well echo the public database password to the terminal.

#### 3. Update your .env with the public database URL

Update`SESAME_DATABASE_USER` and `SESAME_DATABASE_PASSWORD` (public) in `sesame/.env` with the randomly generated password created by the `run_schema` script.

Alternatively, you can set this yourself in `database/schema.sql` by changing the `%%REPLACED%%` input near the bottom.

Optional: Test your connection by running `PYTHONPATH=.  pytest tests/ -s -v`

## Run the webapp server

#### Run the webapp

```
cd sesame
uvicorn webapp.main:app --reload
```

> 💡 Swagger docs are available at http://127.0.0.1:8000/docs to assist in crafting the curl requests.

#### Authentication

You must authenticate all of your web requests with a valid access token which you can generate via the REST API or from the Sesame Dashboard.

For more information, read the [authentication docs](./docs/authentication.md).

## Create your first workspace

A workspace is a container for services, API keys and default conversations settings. A user can have multiple workspaces, but you need at least one to start talking to your bot.

You can create a workspace using the API, for example:

```bash
curl -X 'POST' \
'http://127.0.0.1:8000/api/workspaces' \
-H 'accept: application/json' \
-H "Authorization: Bearer $ACCESS_TOKEN" \
-H 'Content-Type: application/json' \
-d '{
   "title": "My first workspace",
   "config": {
      "api_keys": {...},
      "config": [...],
      "services": {...}
      "default_llm_context": [
         {
            "role": "system",
            "content": {...}
         },
      ]
   }
}'
```

Here is an overview of the configuration options:

`api_keys:{}` - Map of API keys for chosen service providers. Settings a key here will take precedence over anything you have set in your deployment environment.

`config:[]` - A valid [RTVI](https://github.com/rtvi-ai#services) config array to configure your bot pipeline.

`services:{}` - Map of services to providers.

`default_llm_context:{}` - Initial LLM prompt for each new conversation.

> 💡 You can find a full example workspace config [here](./scripts/example_workspace_config.json)

#### 2. Check your workspace was created:

```bash
curl -X 'GET' \
'http://127.0.0.1:8000/api/workspaces' \
-H 'accept: application/json' \
-H "Authorization: Bearer $ACCESS_TOKEN"
```

## Run a client app

Open Sesame includes the following client user interfaces:

- [web](./client/web/README.md)
- [android](./client/android/README.md)
- [ios](./client/ios/README.md)

Coming soon:
cmdline

Mobile clients can be quickly authenticated by scanning a QR code for a session token via the Open Sesame dashboard.

## Deployment

### Deploy Server to Modal.com

This project is configured by default to deploy to [Modal](https://www.modal.com).

The entry point for configuring your deployment can be found at [`server/app.py`](./server/app.py). Whilst the included http and voice bots do not run any compute heavy processes, you can modify your deployment to run local inference should you wish.

Modal.com provides fast process spawning for voice sessions, meaning you app is not bottlenecked by the processing capabilities of the web api.

#### Setup Modal:

Copy the `modal_app.py` from the `deployment/modal` directory to your `sesame` app folder.

```bash
pip install modal
python -m modal setup

cd sesame
modal serve modal_app.py

=>
...
└── 🔨 Created web function api => https://YOUR_DEPLOY_NAME.modal.run
...
```

This serves the app as a shell process. Optional: navigate to `https://YOUR_DEPLOY_NAME.modal.run/docs` to test.

#### Deploy to production:

```bash
modal deploy modal_app.py

=>
...
└── 🔨 Created web function api => https://YOUR_DEPLOY_NAME.modal.run
...
```

For more information regarding Modal, please [go here](https://modal.com/docs/guide)

## Core Technologies

Open Sesame is built on the following open source technologies:

- [Pipecat](https://www.pipecat.ai) - Realtime bot pipelines (http and WebRTC)
- [RTVI](https://github.com/pipecat-ai/rtvi-client-web) - Client <> Bot interface standard
