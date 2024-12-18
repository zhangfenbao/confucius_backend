from typing import Tuple, Optional

from bots.tasks.summarize import generate_conversation_summary
from common.auth import Auth, get_db_with_token
from common.models import (
    Conversation,
    ConversationCreateModel,
    ConversationModel,
    ConversationUpdateModel,
    Message,
    MessageCreateModel,
    MessageModel,
    MessageWithConversationModel,
    Workspace,
    WorkspaceWithConversations,
    Attachment,
    AttachmentModel,
    FileParseResponse,
)
from common.utils.parser import parse_pdf_to_markdown
from fastapi import APIRouter, Depends, HTTPException, Query, status, File, UploadFile
from pydantic import ValidationError
from sqlalchemy import delete, func, select
from sqlalchemy.ext.asyncio import AsyncSession
from sqlalchemy.orm import joinedload
from webapp import get_db

router = APIRouter(prefix="/conversations")


@router.get(
    "/{workspace_id}", response_model=list[ConversationModel], name="Get Conversations by Workspace"
)
async def get_conversations_by_workspace(
    workspace_id: str,
    limit: int = Query(20, ge=1),
    offset: int = Query(0, ge=0),
    db: AsyncSession = Depends(get_db),
):
    result = await db.execute(
        select(Conversation)
        .where(Conversation.workspace_id == workspace_id)
        .where(~Conversation.archived)
        .order_by(Conversation.updated_at.desc())
        .limit(limit)
        .offset(offset)
    )

    conversations = result.scalars().all()

    return [ConversationModel.model_validate(convo) for convo in conversations]


@router.get(
    "/",
    response_model=list[WorkspaceWithConversations],
    name="Get Recent Conversations (with workspace)",
)
async def get_recent_conversations(
    limit: int = Query(50, ge=1),
    db: AsyncSession = Depends(get_db),
):
    workspace_query = select(Workspace).order_by(Workspace.updated_at.desc())

    workspace_result = await db.execute(workspace_query)
    workspaces = workspace_result.scalars().all()
    workspace_models = []

    for workspace in workspaces:
        conversation_query = (
            select(Conversation)
            .where(Conversation.workspace_id == workspace.workspace_id)
            .where(~Conversation.archived)
            .order_by(Conversation.updated_at.desc())
            .limit(limit)
        )

        conversation_result = await db.execute(conversation_query)
        conversations = conversation_result.scalars().all()

        conversation_models = [
            ConversationModel.model_validate(conversation) for conversation in conversations
        ]

        workspace_data = workspace.__dict__.copy()

        workspace_data["conversations"] = conversation_models

        workspace_model = WorkspaceWithConversations.model_validate(workspace_data)
        workspace_models.append(workspace_model)

    return workspace_models


@router.post("", response_model=ConversationModel, status_code=status.HTTP_201_CREATED)
async def create_conversation(
    conversation: ConversationCreateModel,
    db: AsyncSession = Depends(get_db),
):
    new_convo = Conversation(
        workspace_id=conversation.workspace_id,
        title=conversation.title or None,
    )
    db.add(new_convo)

    await db.flush()

    result = await db.execute(
        select(Conversation)
        .where(Conversation.conversation_id == new_convo.conversation_id)
        .options(joinedload(Conversation.workspace))
    )
    new_convo = result.scalar_one_or_none()

    if new_convo is None:
        raise HTTPException(
            status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
            detail="Failed to create conversation",
        )

    if new_convo.workspace.config.get("default_llm_context"):
        for message_data in new_convo.workspace.config["default_llm_context"]:
            try:
                valid_message = MessageCreateModel.model_validate(message_data)
                initial_message = Message(
                    conversation_id=new_convo.conversation_id, **valid_message.model_dump()
                )
                db.add(initial_message)
            except ValidationError:
                continue
        await db.commit()

    return ConversationModel.model_validate(new_convo)


@router.delete("/{conversation_id}")
async def delete_conversation(
    conversation_id: str,
    db: AsyncSession = Depends(get_db),
):
    result = await db.execute(
        select(Conversation).where(Conversation.conversation_id == conversation_id)
    )
    convo = result.scalar_one_or_none()
    if convo is None:
        raise HTTPException(status_code=404, detail="Conversation not found")

    await db.execute(delete(Conversation).where(Conversation.conversation_id == conversation_id))
    await db.commit()
    return {"detail": "Conversation deleted successfully"}


@router.put(
    "/{conversation_id}", response_model=ConversationModel, name="Update Conversation Title"
)
async def update_conversation(
    conversation_id: str,
    conversation_update: ConversationUpdateModel,
    db: AsyncSession = Depends(get_db),
):
    result = await db.execute(
        select(Conversation).where(Conversation.conversation_id == conversation_id)
    )
    conversation = result.scalar_one_or_none()

    if not conversation:
        raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail="Conversation not found")

    update_data = conversation_update.model_dump(exclude_unset=True)

    for field, value in update_data.items():
        setattr(conversation, field, value)

    await db.commit()
    await db.refresh(conversation)

    return ConversationModel.model_validate(conversation)


@router.get(
    "/{conversation_id}/messages", response_model=dict, name="Get Conversation and Messages"
)
async def get_conversation_messages(
    conversation_id: str,
    db_and_auth: Tuple[AsyncSession, Auth] = Depends(get_db_with_token),
):
    db, auth = db_and_auth

    result = await db.execute(
        select(Conversation)
        .options(joinedload(Conversation.messages))
        .where(Conversation.conversation_id == conversation_id)
    )

    conversation = result.scalars().first()

    if not conversation:
        raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail="Conversation not found")

    result = await db.execute(
        select(Message)
        .where(Message.conversation_id == conversation_id)
        .order_by(Message.message_number)
    )

    messages = result.scalars().all()

    return {
        "conversation": ConversationModel.model_validate(conversation),
        "messages": [MessageModel.model_validate(msg) for msg in messages],
    }


@router.post(
    "/{conversation_id}/messages", response_model=MessageModel, status_code=status.HTTP_201_CREATED
)
async def create_message(
    conversation_id: str,
    message: MessageCreateModel,
    db: AsyncSession = Depends(get_db),
):
    conversation_result = await db.execute(
        select(Conversation).where(Conversation.conversation_id == conversation_id)
    )
    conversation = conversation_result.scalars().first()

    if not conversation:
        raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail="Conversation not found")

    new_message = Message(
        conversation_id=conversation_id,
        content=message.content,
        extra_metadata=message.extra_metadata,
    )

    db.add(new_message)
    await db.commit()
    await db.refresh(new_message)

    return MessageModel.model_validate(new_message)


@router.get("/{workspace_id}/search", response_model=list[MessageWithConversationModel])
async def search_messages(
    workspace_id: str,
    search_term: str = Query(..., min_length=1),
    limit: int = Query(20, ge=1),
    offset: int = Query(0, ge=0),
    db: AsyncSession = Depends(get_db),
):
    result = await db.execute(
        select(Message, Conversation)
        .join(Conversation, Message.conversation_id == Conversation.conversation_id)
        .where(Conversation.workspace_id == workspace_id)
        .where(~Conversation.archived)
        .where(
            func.to_tsvector(
                "english", func.concat_ws(" ", Conversation.title, Message.content_tsv)
            ).op("@@")(func.to_tsquery("english", search_term))
        )
        .order_by(Message.created_at.desc())
        .limit(limit)
        .offset(offset)
    )

    rows = result.all()

    if not rows:
        raise HTTPException(status_code=404, detail="No matching messages found")

    response = []
    for message, conversation in rows:
        response.append(
            MessageWithConversationModel(
                message=MessageModel.model_validate(message),
                conversation=ConversationModel.model_validate(conversation),
            )
        )

    return response


@router.post("/summarize", response_model=ConversationModel, name="Summarize a conversation")
async def summarize_conversation(
    conversation_id: str,
    db_and_auth: Tuple[AsyncSession, Auth] = Depends(get_db_with_token),
):
    db, auth = db_and_auth

    try:
        conversation = await generate_conversation_summary(conversation_id, db)
        return ConversationModel.model_validate(conversation)
    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))


@router.post(
    "/{conversation_id}/attachments",
    response_model=FileParseResponse,
    status_code=status.HTTP_201_CREATED,
)
async def create_attachment(
    conversation_id: str,
    message_id: Optional[str] = None,
    file: UploadFile = File(...),
    db: AsyncSession = Depends(get_db),
):
    """
    为会话创建附件,解析上传的PDF文件并返回Markdown格式的内容
    """
    if not file.filename.lower().endswith('.pdf'):
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail="目前只支持PDF文件格式"
        )

    try:
        # 读取文件内容
        content = await file.read()
        
        # 解析PDF
        markdown_content = await parse_pdf_to_markdown(content)
        
        # 创建附件记录
        attachment = await Attachment.create_attachment(
            conversation_id=conversation_id,
            message_id=message_id,
            file_url=None,  # 暂时为None
            file_name=file.filename.split('.')[0],
            file_type=file.filename.split('.')[-1].lower(),
            content=markdown_content,
            db=db
        )
        
        return FileParseResponse(
            attachment_id=attachment.attachment_id,
            content=markdown_content
        )
        
    except Exception as e:
        raise HTTPException(
            status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
            detail=f"PDF解析失败: {str(e)}"
        )


@router.get(
    "/{conversation_id}/attachments",
    response_model=list[AttachmentModel],
    name="Get Conversation Attachments"
)
async def get_conversation_attachments(
    conversation_id: str,
    db: AsyncSession = Depends(get_db),
):
    """获取会话的所有附件"""
    result = await db.execute(
        select(Attachment)
        .where(Attachment.conversation_id == conversation_id)
        .order_by(Attachment.created_at.desc())
    )
    
    attachments = result.scalars().all()
    return [AttachmentModel.model_validate(attachment) for attachment in attachments]
