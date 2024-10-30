<img alt="open-sesame" width="480px" height="auto" src="./docs/sesame-banner.png">

# Open Sesame

Open Source multi-modal LLM environment. Host your own web and mobile chat interface, powered by real-time bots that provide best-in-class voice AI functionality with an emphasis on:

- Using any model for any conversation thread
- Data ownership
- Great voice integration
- Open source and extensibility

## Table of Contents

- [Quickstart](#quickstart)
- [CLI Commands](#open-sesame-cli)
- [Overview and Concepts](#overview-and-concepts)
  - [Database Setup](#database-setup)
  - [Project Structure](#project-structure)
  - [API](#api)
  - [Workspaces](#workspaces)
  - [Services](#services)
  - [Authentication](#authentication)
- [Client Apps](#client-apps)
- [Deployment](#deployment)
- [Core Technologies](#core-technologies)

## Quickstart

### Prerequisites

- Python 3.10 or higher
- Database supporting async sessions
- API keys for:
  - Large Language Model provider
  - Text-to-Speech provider
  - Speech-to-Text provider

### Installation Steps

1. **Install Dependencies**
```bash
python -m venv venv
source venv/bin/activate  # Or OS-specific activation
pip install -r sesame/dev-requirements.txt
cd sesame/
```

2. **Configure Environment**
```bash
python sesame.py init
```
This creates a `.env` file. Alternatively, copy `sesame/env.example` to `sesame/.env` and configure manually.

3. **Set Up Database**
```bash
python sesame.py init-db
```

Required environment variables:
```
SESAME_DATABASE_ADMIN_USER="postgres"
SESAME_DATABASE_ADMIN_PASSWORD=""
SESAME_DATABASE_NAME="postgres"
SESAME_DATABASE_HOST="localhost"
SESAME_DATABASE_PORT="5432"
SESAME_DATABASE_USER="sesame"
SESAME_DATABASE_PASSWORD="your-strong-password"
```

4. **Test Database Connection**
```bash
python sesame.py test-db --admin  # Test admin credentials
python sesame.py run-schema      # Apply database schema
python sesame.py test-db         # Test user credentials
```

5. **Create User**
```bash
python sesame.py create-user
```

6. **Launch Application**
```bash
python sesame.py run
```
Visit the provided URL (e.g., `http://127.0.0.1:8000`) to access the dashboard.

### Troubleshooting

- Verify `.env` configuration
- Test database credentials
- Run tests: `PYTHONPATH=. pytest tests/ -s -v`

Manual server launch:
```bash
python -m uvicorn webapp.main:app --reload
```

## CLI Commands

```bash
python sesame.py --help           # Show available commands
python sesame.py init            # Create .env file
python sesame.py init-db         # Configure database
python sesame.py test-db         # Test database connection
python sesame.py run-schema      # Apply database schema
python sesame.py create-user     # Create new user
python sesame.py run             # Launch application
python sesame.py services        # List registered services
```

## Overview and Concepts

### Database Setup

Requires PostgreSQL with extensions specified in `schema/postgresql.sql`. Cloud-hosted options like Render or Supabase recommended. See [database documentation](./docs/database.md) for details.

### Project Structure

Core modules:
- `webapp`: FastAPI routes for authentication and database communication
- `bots`: Pipecat bot pipelines for inference and voice communication
- `common`: Shared libraries

### API

Access Swagger documentation at `/docs` (requires authentication).

### Workspaces

Workspaces contain:
- Conversations and messages
- Bot pipeline configurations
- Optional workspace-specific services

### Services

Required services:
- Single-turn bots: `llm`
- Voice bots: `transport`, `llm`, `stt`, `tts`

Services can be configured at user or workspace level.

### Authentication

All requests require valid user tokens. See [authentication documentation](./docs/authentication.md).

## Client Apps

Available clients:
- [Web](./client/web/README.md)
- [Android](./client/android/README.md)
- [iOS](./client/ios/README.md)

Coming soon:
- Command-line interface

## Deployment

### Modal.com Deployment

1. Copy `modal_app.py` from `deployment/modal` to `sesame` folder
2. Install and configure Modal:
```bash
pip install modal
python -m modal setup
cd sesame
modal serve modal_app.py    # Development
modal deploy modal_app.py   # Production
```

Visit `https://YOUR_DEPLOY_NAME.modal.run/docs` to verify deployment.

## Core Technologies

Built with:
- [Pipecat](https://www.pipecat.ai) - Realtime bot pipelines
- [RTVI](https://github.com/pipecat-ai/rtvi-client-web) - Client-Bot interface standard