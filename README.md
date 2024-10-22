<img alt="open-sesame" width="480px" height="auto" src="./docs/sesame-banner.png">

Open Source multi-modal LLM environment. Host your own web and mobile chat interface, powered by real-time bots that provide best-in-class voice AI functionality with an emphasis on:

- Using any model for any conversation thread
- Data ownership
- Great voice integration
- Open source and extensibility

## Table of contents

- [Quickstart](#quickstart)
- [Overview](#overview)
  - [Project requirements](#project-requirements)
  - [Database setup](#database-setup)
  - [Webapp and workspaces](#run-the-webapp-server-and-create-a-workspace)
- [Create your first workspace](#create-your-first-workspace)
- [Run a client app](#run-a-client-app)
- [Deployment](#deployment)
  - [Modal.com](#deploy-server-to-modalcom)
- [Core technologies](#core-technologies)

## Quickstart

#### 1. Install project dependencies

```shell
python -m venv venv
source venv/bin/activate # ... or OS specific activation
pip install -r server/dev-requirements.txt
```

#### 2. Create a local .env

```shell
cp server/env.example server/.env
```

You must set the following:

`SESAME_DATABASE_ADMIN_URL`

Database superuser credentials (i.e. Supabase URI on port `5432`.) _Note: Must support asyncpg or equivalent asychnronous driver._

> 🛑 Ensure you are using a database URL on a session port (typically `5432`) If you are using Supabase, the URL provided in the settings panel defaults to "transaction mode". See [Database setup](#database-setup) for details.

You database URL should look something like:

```bash
SESAME_DATABASE_ADMIN_URL="postgresql://postgres.ID:PASSWORD@aws-0-us-west-1.pooler.supabase.com:5432/postgres"
# Note port 5432, not 6543
```

#### 3. Create database roles and schema

From the root of the project, run the schema script found in [scripts/run_shema.sh](./scripts/run_schema.sh)

```shell
bash scripts/run_schema.sh
```

Note: the `run_schema.sh` script requires Postgres to run. Install the necessary package for your system (e.g. `brew install postgresql` for MacOS).

If the schema runs correctly, the script will print out a non-superuser URL to the terminal which you should add to your environment.

Add `SESAME_DATABASE_URL` in `server/.env` with the output of the script.

> 🛑 If your admin database password includes "@" characters, the bash script may fail to correctly replace the `SESAME_DATABASE_URL` password using `sed`. In this scenario, please manually adjust.

For more information about database configuration, read [here](#database-setup)

#### 4. Create a user

From the root of the project still, create a user account and password from [scripts/create_user.sh](./scripts/create_user.sh).

```shell
bash scripts/create_user.sh
```

Running this script will create a user account in your database. Make a note of your username and password; the password will be encrypted and not recoverable later.

#### 5. Run the Sesame server and generate access token

```shell
cd server/
uvicorn webapp.main:app --reload
```

You should see a URL in your terminal window to visit, for example `http://127.0.0.1:8000`. Navigating to this URL should reveal the Sesame dashboard, or an error message if something went wrong.

<img alt="open-sesame-dashboard" width="280px" height="auto" src="./docs/sesame-dashboard.png">

Log in with the user name and password you set in step 4.

Now, create a new access token to authenticate web requests in any of the Open Sesame clients. For more information, see [authentication](./docs/authentication.md).

#### 6. Create your first workspace

Follow the [workspace creation steps](#create-your-first-workspace), and run a [client](#run-a-client-app) of your choosing.

## Overview

### Project requirements

- Python 3.10 or higher.
- Database that supports async sessions (we recommend [Supabase](https://supabase.com/).)
- Deployment target that can run Python processes (we recommend [Modal](https://www.modal.com).)
- API keys for support transport, llm, tts, and stt services.

_Note: Sesame bots are configured to use [Daily](https://www.daily.co) as a transport layer when using realtime voice mode. You must provide a valid Daily API key to your workspace configuration in order to use voice mode_

### Database setup

Sesame works with most types of database. If you are using Supabase, there are no additional setup steps required. If not, you may need to install additional extensions specified in [database/schema.sql](./database/schema.sql).

#### 1. Update your .env

Update`SESAME_DATABASE_ADMIN_URL` (admin) in `server/.env`.

For Supabase can find your URL here: https://supabase.com/dashboard/project/[YOUR_PROJECT]/settings/database. **Note: be sure to use the URI for `Mode: session`.**

![](./docs/supabaseurl.png)

Your database URI should look something like this:

```bash
SESAME_DATABASE_ADMIN_URL=postgresql://postgres.user:pass@region.pooler.supabase.com:5432/postgres
```

#### 2. Apply database schema

Run the schema `bash scripts/run_schema.sh` (or copy and paste into your SQL Editor of choice).

This script will create the necessary tables, functions and triggers, as well echo the public database password to the terminal.

#### 3. Update your .env with the public database URL

Update`SESAME_DATABASE_URL` (public) in `server/.env` with the randomly generated password created by the `run_schema` script.

Alternatively, you can set this yourself in `database/schema.sql` by changing the `%%REPLACED%%` input near the bottom.

Optional: Test your connection by running `PYTHONPATH=.  pytest tests/ -s -v`

## Run the webapp server

#### Run the webapp

```
cd server
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

```bash
pip install modal
python -m modal setup

cd server
modal serve app.py

=>
...
└── 🔨 Created web function api => https://YOUR_DEPLOY_NAME.modal.run
...
```

This serves the app as a shell process. Optional: navigate to `https://YOUR_DEPLOY_NAME.modal.run/docs` to test.

#### Deploy to production:

```bash
modal deploy app.py

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
