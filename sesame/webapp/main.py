import os
import sys
from contextlib import asynccontextmanager

from cachetools import TTLCache
from common.database import DatabaseSessionFactory
from common.models import Base
from dotenv import load_dotenv
from fastapi import FastAPI, HTTPException, Request
from fastapi.middleware.cors import CORSMiddleware
from fastapi.responses import HTMLResponse
from fastapi.templating import Jinja2Templates
from common.utils.logger import get_webapp_logger
from sqlalchemy import text
import logging
from .api import router as api_router

load_dotenv(override=True)

logger = get_webapp_logger()

for logger_name in logging.root.manager.loggerDict.keys():
    if logger_name.startswith('sqlalchemy'):
        logging.getLogger(logger_name).setLevel(logging.ERROR)

default_session_factory = DatabaseSessionFactory()

# ========================
# FastAPI App
# ========================


@asynccontextmanager
async def lifespan(app: FastAPI):
    app.state.cache = TTLCache(maxsize=100, ttl=300)

    try:
        async with default_session_factory.engine.connect() as session:
            if bool(int(os.getenv("SESAME_DATABASE_USE_REFLECTION", "0"))):
                await session.run_sync(Base.metadata.reflect)
    except Exception:
        logger.error(
            "Database connection failed. Have you set valid SESAME_DATABASE_* credentials in your .env?"
        )
        os._exit(1)
    yield
    await default_session_factory.engine.dispose()


app = FastAPI(
    title="Open Sesame",
    docs_url="/docs",
    openapi_tags=[{"name": "Authentication", "description": "API protected by Bearer tokens"}],
    lifespan=lifespan,
)

app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

app.include_router(api_router, prefix="/api")

templates = Jinja2Templates(directory="webapp/dashboard")


# ========================
# Quickstart route
# ========================


@app.get("/", response_class=HTMLResponse)
async def home(request: Request):
    if int(os.getenv("SESAME_SHOW_WEB_UI", "0") or 0) != 1:
        raise HTTPException(status_code=404, detail="Page not found")

    try:
        async with default_session_factory() as session:
            await session.execute(text("SELECT 1"))
    except Exception:
        return templates.TemplateResponse(
            request,
            "error.html",
            {
                "message": "Database connection failed. Have you set valid SESAME_DATABASE_* credentials in your .env?"
            },
        )

    try:
        async with default_session_factory() as session:
            result = await session.execute(
                text("SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = 'public'")
            )
            count = result.scalar()
            if count == 0:
                raise
    except Exception:
        return templates.TemplateResponse(
            request,
            "error.html",
            {
                "message": "Sesame schema not found in database. Have you run bash scripts/run_schema.sh ?",
            },
        )

    return templates.TemplateResponse(
        request,
        "index.html",
        {
            "message": "Sesame running. Visit /docs for API documentation.",
        },
    )
