from fastapi import APIRouter

from .conversations import router as conversations_router
from .rtvi import router as rtvi_router
from .services import router as services_router
from .users import router as users_router
from .workspaces import router as workspaces_router

router = APIRouter()

router.include_router(users_router, tags=["Users"])
router.include_router(workspaces_router, tags=["Workspaces"])
router.include_router(conversations_router, tags=["Conversations"])
router.include_router(services_router, tags=["Services"])
router.include_router(rtvi_router, tags=["RTVI"])
