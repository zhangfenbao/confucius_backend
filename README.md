<img alt="open-sesame" width="480px" height="auto" src="./docs/sesame-banner.png">

Open Source multi-modal LLM environment. Host your own web and mobile chat interface, powered by real-time bots that provide best-in-class voice AI functionality with an emphasis on:

- Using any model for any conversation thread
- Data ownership
- Great voice integration
- Open source and extensibility

## Table of contents

- [Quickstart](#quickstart)
- [CLI commands](#open-sesame-cli)
- [Overview and Concepts](#overview-and-concepts)
  - [Database setup guides](#database-setup-guides)
  - [Project structure](#project-structure)
  - [API](#api)
  - [Services](#run-the-webapp-server-and-create-a-workspace)
  - [Authentication](#authentication)
- [Run a client app](#run-a-client-app)
- [Deployment](#deployment)
  - [Modal.com](#deploy-server-to-modalcom)
- [Core technologies](#core-technologies)

## Quickstart

To run Open Sesame locally, you will need:

- Python 3.10 or higher.
- Database that supports async sessions.
- Service provider API keys for a large-language, text-to-speech, and speech-to-text providers.

Running an Open Sesame requires:

- Deploying the `sesame` app, containing the API and bot files.
- Connecting a client to your deployment.

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

Open Sesame uses data storage for users, services, workspace settings and conversation history. The current default schema assumes Postgres.

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
SESAME_DATABASE_PORT="5432"
# Public database role credentials
SESAME_DATABASE_USER="sesame"
SESAME_DATABASE_PASSWORD="some-strong-password"
```

> 🛑 Ensure you are using a database that accepts session mode typically available on port `5432`. If you are using Supabase, the URL provided in the settings panel defaults to "transaction mode". See [Database setup](#database-setup) for details.

You can test your database credentials:

```shell
python sesame test-db --admin
# Note: --admin specifies to test using the admi credentials. The efault user role has not yet been created
```

> 🛑 The Open Sesame CLI will not create the database for you. If the database does not exist (if you are using a local psql, for example), please ensure to run `CREATE DATABASE dbname;` where `dbname` matched your `SESAME_DATABASE_NAME`.


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

You should see a URL in your terminal window to visit, for example `http://127.0.0.1:8000`. Navigating to this URL should reveal the Open Sesame dashboard, or an error message if something went wrong.

<img alt="open-sesame-dashboard" width="280px" height="auto" src="./docs/sesame-dashboard.png">

#### Something went wrong?

- Check all the necessary settings are configured in your `.env`
- Ensure the database credentials are accurate: `python sesame.py test-db`
- Run the included tests for more details: `PYTHONPATH=. pytest tests/ -s -v`


To manually run the FastAPI server:

```shell
python -m uvicorn webapp.main:app --reload
```

### 6. Run one of the clients

[See here for getting setup with a client app](#run-a-client-app).

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
python sesame.py test-db  --admin # Admin role
python sesame.py test-db  # User role

# Run required schema
python sesame.py run-schema

# Create user
python sesame.py create-user
# ... or for no prompt
python sesame.py create-user -u user -p pass

# Run the FastaPI app
python sesame.py run

# View registered services
python sesame.py services
```

---

#### 8. Create your first workspace

Follow the [workspace creation steps](#create-your-first-workspace), and run a [client](#run-a-client-app) of your choosing.


## Overview and Concepts


### Database setup guides

Open Sesame requires a Postgres database (support for other database types, such as SQLite, coming soon). You may need to install additional extensions specified in [schema/postgresql.sql](./schema/postgresql.sql).

For simplicity, we recommend using a cloud-hosted provider, such as [Render](www.render.com) or [Supabase](www.supabase.com). You can read more about database configuration steps [here](./docs/database.md.)

### Project structure

Open Sesame has 3 core Python modules that can be deployed together or individually.

- `webapp` - FastAPI routes, mostly responsible for authenticating and communicating with your database.

- `bots` - [Pipecat](www.pipecat.ai) bot pipelines, one for single-turn inference and another for realtime voice communication.

- `common` - Shared libraries used by both the webapp API and the bots (such as database adapters, etc.)



A `Dockerfile` is included in the `./sesame` directory.

### API

The `webapp` must be running in order for your client and bots to function. 

It defines an API that exposes several HTTP routes, such as workspace or conversation creation.

```
python sesame.py run
```

Swagger docs are available at `/docs`, e.g. `http://localhost:8000/docs`. You must authenticate requests via the docs with a valid user token.

### Workspaces

Workspaces define the environment in which you interact with your bot. They contain:

- Conversations and messages
- Config options for your bot pipelines
- Optional: workspace specific services and keys (see [services](s#ervices) section below.)

Workspaces are unique per user, and you can create as many as you like to support various use-cases. All the clients in this repo support initial workspace configuration steps.

### Services

Open Sesame bots require that you configure services and their associated API keys and required config options.

Single-turn HTTP bots require:

- `llm` (e.g. OpenAI, Together AI, Anthropic etc.)

Realtime voice bots require:

- `transport` (e.g. Daily)
- `llm` (e.g. OpenAI, Together AI, Anthropic etc.)
- `stt` (e.g. Deepgram, Azure etc.)
- `tts` (e.g. Cartesia, ElevenLabs etc.)

You can configure services at either **user level** or **workspace** level. 

- Services defined at user level are accessible by all user workspaces.

- Services defined at workspace level are only accessible for that specific workspace. This is useful if you want to override a service key or config option within the context of a specific workspace.

### Authentication

You must authenticate all of your web requests with a valid user token which you can generate via the REST API or from the Open Sesame Dashboard.

For more information, read the [authentication docs](./docs/authentication.md).

## Run a client app

Open Sesame includes the following client user interfaces:

- [web](./client/web/README.md)
- [android](./client/android/README.md)
- [ios](./client/ios/README.md)

Coming soon:
cmdline

Mobile clients can be quickly authenticated by scanning a QR code for a session token via the Open Sesame dashboard.

## Deployment

### Deploy to Modal.com

This project is configured by default to deploy to [Modal](https://www.modal.com).

The entry point for configuring your deployment can be found at [`deployment/modal_app.py`](./deployment/modal_app.py). Whilst the included http and voice bots do not run any compute heavy processes, you can modify your deployment to run local inference should you wish.

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
