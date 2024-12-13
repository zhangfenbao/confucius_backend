from fastapi import APIRouter

from .auth import router as auth_router
from .conversations import router as conversations_router
from .rtvi import router as rtvi_router
from .services import router as services_router
from .tokens import router as tokens_router
from .workspaces import router as workspaces_router

router = APIRouter()

router.include_router(auth_router, tags=["Auth"], include_in_schema=True)
router.include_router(tokens_router, tags=["Tokens"])
router.include_router(workspaces_router, tags=["Workspaces"])
router.include_router(conversations_router, tags=["Conversations"])
router.include_router(services_router, tags=["Services"])
router.include_router(rtvi_router, tags=["RTVI"])
