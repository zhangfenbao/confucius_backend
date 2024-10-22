from common.auth import Auth
from common.models import (
    Workspace,
    WorkspaceDefaultConfigModel,
    WorkspaceModel,
    WorkspaceUpdateModel,
)
from fastapi import APIRouter, Depends, HTTPException, status
from pydantic import ValidationError
from sqlalchemy import delete, select
from sqlalchemy.ext.asyncio import AsyncSession
from sqlalchemy.orm import selectinload
from webapp.deps import get_db, get_user

router = APIRouter(prefix="/workspaces")


@router.get("", response_model=list[WorkspaceModel])
async def get_workspaces(
    db: AsyncSession = Depends(get_db),
):
    result = await db.execute(
        select(Workspace).order_by(Workspace.updated_at.desc()).limit(20).offset(0)
    )
    workspaces = result.scalars().all()
    return [WorkspaceModel.model_validate(workspace) for workspace in workspaces]


@router.post("", response_model=WorkspaceModel, status_code=status.HTTP_201_CREATED)
async def create_workspace(
    workspace: WorkspaceUpdateModel,
    db: AsyncSession = Depends(get_db),
    user: Auth = Depends(get_user),
):
    try:
        config = WorkspaceDefaultConfigModel.model_validate(workspace.config).model_dump()
    except ValidationError as e:
        raise HTTPException(status_code=status.HTTP_400_BAD_REQUEST, detail=str(e.errors()))

    new_workspace = Workspace(title=workspace.title, user_id=user.user_id, config=config)
    db.add(new_workspace)

    await db.commit()

    return WorkspaceModel.model_validate(new_workspace)


@router.get("/{workspace_id}", response_model=WorkspaceModel)
async def get_workspace(
    workspace_id: str,
    db: AsyncSession = Depends(get_db),
):
    result = await db.execute(select(Workspace).where(Workspace.workspace_id == workspace_id))
    workspace = result.scalar_one_or_none()
    if workspace is None:
        raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail="Workspace not found")
    return WorkspaceModel.model_validate(workspace)


@router.put("/{workspace_id}", response_model=WorkspaceModel)
async def update_workspace(
    workspace_id: str,
    workspace: WorkspaceUpdateModel,
    db: AsyncSession = Depends(get_db),
):
    result = await db.execute(
        select(Workspace).where(Workspace.workspace_id == workspace_id).options(selectinload("*"))
    )
    workspace_to_update = result.scalar_one_or_none()

    if workspace_to_update is None:
        raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail="Workspace not found")

    update_data = workspace.model_dump(exclude_unset=True)

    if "config" in update_data:
        new_config = update_data.pop("config")

        try:
            existing_config = WorkspaceDefaultConfigModel.model_validate(workspace_to_update.config)
            merged_config = existing_config.model_copy(update=new_config)
        except ValidationError as e:
            raise HTTPException(status_code=status.HTTP_422_UNPROCESSABLE_ENTITY, detail=str(e))

        setattr(workspace_to_update, "config", merged_config.model_dump())

    for key, value in update_data.items():
        setattr(workspace_to_update, key, value)

    await db.commit()
    return WorkspaceModel.model_validate(workspace_to_update)


@router.delete("/{workspace_id}")
async def delete_workspace(
    workspace_id: str,
    db: AsyncSession = Depends(get_db),
):
    result = await db.execute(select(Workspace).where(Workspace.workspace_id == workspace_id))
    workspace = result.scalar_one_or_none()
    if workspace is None:
        raise HTTPException(status_code=404, detail="Workspace not found")

    await db.execute(delete(Workspace).where(Workspace.workspace_id == workspace_id))
    await db.commit()
    return {"detail": "Workspace deleted successfully"}
