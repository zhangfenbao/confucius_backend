import base64
import os
import uuid
from datetime import datetime, timedelta, timezone
from typing import List, Optional

from pipecat.processors.frameworks.rtvi import RTVIServiceConfig
from pydantic import BaseModel, Field
from sqlalchemy import (
    TIMESTAMP,
    Boolean,
    CheckConstraint,
    Column,
    ForeignKey,
    Index,
    Integer,
    String,
    func,
    select,
)
from sqlalchemy.dialects.postgresql import JSONB, TSVECTOR, UUID
from sqlalchemy.ext.asyncio import AsyncSession
from sqlalchemy.orm import (
    Mapped,
    declarative_base,
    joinedload,
    mapped_column,
    relationship,
)
from sqlalchemy.sql import expression

Base = declarative_base()

# ==========================
# SQLAlchemy Models
# ==========================


class User(Base):
    __tablename__ = "users"

    user_id = Column(String(64), primary_key=True)
    username = Column(String(64), unique=True, nullable=False)
    password_hash = Column(String, nullable=False)
    created_at = Column(TIMESTAMP(timezone=True), server_default=func.now())
    updated_at = Column(TIMESTAMP(timezone=True), server_default=func.now(), onupdate=func.now())

    tokens: Mapped[List["Token"]] = relationship("Token", back_populates="user")

    __table_args__ = (Index("idx_users_username", "username"),)

    @classmethod
    async def get_user_by_username(cls, username: str, db: AsyncSession):
        result = await db.execute(select(User).where(User.username == username))
        return result.scalars().first()


class Token(Base):
    __tablename__ = "tokens"

    token_id = Column(UUID(as_uuid=True), primary_key=True, default=uuid.uuid4)
    user_id = Column(String(64), ForeignKey("users.user_id", ondelete="CASCADE"), nullable=False)
    token = Column(String(64), nullable=False, unique=True)
    title = Column(String(255))
    created_at = Column(TIMESTAMP(timezone=True), server_default=func.now())
    expires_at = Column(TIMESTAMP(timezone=True), nullable=False)
    revoked = Column(Boolean, default=False)

    user: Mapped["User"] = relationship("User", back_populates="tokens")

    __table_args__ = (
        Index("idx_tokens_user_id", "user_id"),
        Index("idx_tokens_user_token", "token", "user_id"),
        Index("idx_tokens_token_revoked_expires_at", "token", "revoked", "expires_at"),
    )

    @classmethod
    async def create_token_for_user(
        cls, user_id: str, db: AsyncSession, title: str = None, expiration_minutes: int = 30
    ):
        expires_at = datetime.now(timezone.utc) + timedelta(minutes=expiration_minutes)
        random_bytes = os.urandom(32)
        token = base64.urlsafe_b64encode(random_bytes).decode("utf-8")

        new_token = cls(
            user_id=user_id,
            token=token,
            title=title,
            expires_at=expires_at,
        )

        db.add(new_token)

        return new_token

    @classmethod
    async def get_token(cls, token: str, db: AsyncSession):
        result = await db.execute(
            select(Token).where(Token.token == token, Token.revoked.is_(False))
        )
        return result.scalar_one_or_none()

    @classmethod
    async def revoke_token(cls, token_id: UUID, db: AsyncSession):
        result = await db.execute(select(Token).where(Token.token_id == token_id))
        token = result.scalar_one_or_none()
        if token:
            token.revoked = True
            await db.commit()
            return True
        return False


class Workspace(Base):
    __tablename__ = "workspaces"

    workspace_id = Column(UUID(as_uuid=True), primary_key=True, default=uuid.uuid4)
    user_id = Column(String(64), nullable=False)
    title = Column(String(255), nullable=False)
    config = Column(JSONB, nullable=False, default=dict)
    created_at = Column(TIMESTAMP(timezone=True), server_default=func.now())
    updated_at = Column(TIMESTAMP(timezone=True), server_default=func.now(), onupdate=func.now())

    conversations: Mapped["Conversation"] = relationship("Conversation", back_populates="workspace")

    __mapper_args__ = {"eager_defaults": True}

    __table_args__ = (
        Index("idx_workspaces_title", "title"),
        Index("idx_workspaces_user_id", "user_id"),
    )

    @classmethod
    async def get_workspaces(cls, db: AsyncSession):
        result = await db.execute(
            select(Workspace).order_by(Workspace.updated_at.desc()).limit(20).offset(0)
        )
        return result.scalars().all()


class Conversation(Base):
    __tablename__ = "conversations"

    conversation_id = Column(UUID(as_uuid=True), primary_key=True, default=uuid.uuid4)
    workspace_id = Column(
        UUID(as_uuid=True),
        ForeignKey("workspaces.workspace_id", ondelete="CASCADE"),
        nullable=False,
    )
    title: Mapped[Optional[str]] = mapped_column(String(255), nullable=True)
    archived = Column(Boolean, default=False)
    language_code: Mapped[str] = mapped_column(String(20), default="english")
    created_at = Column(TIMESTAMP(timezone=True), server_default=func.now())
    updated_at = Column(TIMESTAMP(timezone=True), server_default=func.now(), onupdate=func.now())

    workspace: Mapped["Workspace"] = relationship("Workspace", back_populates="conversations")
    messages: Mapped[List["Message"]] = relationship("Message", back_populates="conversation")

    __table_args__ = (
        Index("idx_conversations_language_code", "language_code"),
        Index("idx_conversations_workspace_id", "workspace_id"),
    )

    @classmethod
    async def get_conversation_by_id(cls, conversation_id: str, db: AsyncSession):
        result = await db.execute(
            select(Conversation)
            .options(joinedload(Conversation.workspace))
            .options(joinedload(Conversation.messages))
            .where(Conversation.conversation_id == conversation_id)
        )
        return result.scalars().first()


class Message(Base):
    __tablename__ = "messages"

    message_id = Column(UUID(as_uuid=True), primary_key=True, default=uuid.uuid4)
    conversation_id = Column(
        UUID(as_uuid=True),
        ForeignKey("conversations.conversation_id", ondelete="CASCADE"),
        nullable=False,
    )
    message_number = Column(
        Integer,
        nullable=False,
        server_default=expression.text("next_message_number(conversation_id::uuid)"),
    )
    content = Column(JSONB, nullable=False)
    content_tsv = Column(TSVECTOR, nullable=True)
    language_code = Column(String(20), default="english")
    created_at = Column(TIMESTAMP(timezone=True), server_default=func.now())
    updated_at = Column(TIMESTAMP(timezone=True), server_default=func.now(), onupdate=func.now())
    token_count = Column(Integer, default=0)
    extra_metadata = Column(JSONB, nullable=True)

    conversation: Mapped[Conversation] = relationship("Conversation", back_populates="messages")
    attachments: Mapped[List["Attachment"]] = relationship("Attachment", back_populates="message")

    __table_args__ = (
        CheckConstraint("token_count >= 0", name="non_negative_token_count"),
        Index("idx_messages_conversation_id", "conversation_id"),
        Index("idx_messages_language_code", "language_code"),
        Index("idx_messages_conversation_number", "conversation_id", "message_number", unique=True),
    )

    @classmethod
    async def get_messages_by_conversation_id(cls, conversation_id: str, db: AsyncSession):
        result = await db.execute(
            select(Message)
            .where(Message.conversation_id == conversation_id)
            .order_by(Message.message_number)
        )
        return result.scalars().all()


class Attachment(Base):
    __tablename__ = "attachments"

    attachment_id = Column(UUID(as_uuid=True), primary_key=True, default=uuid.uuid4)
    message_id = Column(
        UUID(as_uuid=True), ForeignKey("messages.message_id", ondelete="CASCADE"), nullable=False
    )
    file_url = Column(String, nullable=False)
    file_name = Column(String(255), nullable=False)
    file_type = Column(String(50), nullable=False)
    created_at = Column(TIMESTAMP(timezone=True), server_default=func.now())

    message: Mapped["Message"] = relationship("Message", back_populates="attachments")

    __table_args__ = (Index("idx_attachments_message_id", "message_id"),)

    @classmethod
    async def create_attachment(
        cls,
        message_id: uuid.UUID,
        file_url: str,
        file_name: str,
        file_type: str,
        db: AsyncSession,
    ):
        new_attachment = cls(
            message_id=message_id,
            file_url=file_url,
            file_name=file_name,
            file_type=file_type,
        )
        db.add(new_attachment)
        await db.commit()
        return new_attachment

    @classmethod
    async def get_attachments_by_message_id(cls, message_id: uuid.UUID, db: AsyncSession):
        result = await db.execute(select(cls).where(cls.message_id == message_id))
        return result.scalars().all()

    @classmethod
    async def delete_attachment(cls, attachment_id: uuid.UUID, db: AsyncSession):
        result = await db.execute(select(cls).where(cls.attachment_id == attachment_id))
        attachment = result.scalar_one_or_none()
        if attachment:
            await db.delete(attachment)
            await db.commit()
            return True
        return False


# ==========================
# Pydantic Models
# ==========================


class UserLoginModel(BaseModel):
    username: str
    password: str

    model_config = {"from_attributes": True}


class UserModel(BaseModel):
    user_id: str
    username: str
    created_at: datetime
    updated_at: datetime

    model_config = {
        "from_attributes": True,
    }


class CreateTokenRequest(BaseModel):
    title: Optional[str] = None


class RevokeTokenRequest(BaseModel):
    token: Optional[str] = None


class MessageCreateModel(BaseModel):
    content: dict
    extra_metadata: Optional[dict] = None

    model_config = {
        "from_attributes": True,
        "extra": "allow",
    }


class WorkspaceDefaultConfigModel(BaseModel):
    config: Optional[List[RTVIServiceConfig]] = None
    api_keys: Optional[dict] = None
    services: Optional[dict] = None
    default_llm_context: Optional[list[MessageCreateModel]] = Field(default_factory=list)

    model_config = {"extra": "allow", "from_attributes": True, "arbitrary_types_allowed": True}


class WorkspaceModel(BaseModel):
    workspace_id: uuid.UUID
    title: str
    config: WorkspaceDefaultConfigModel
    created_at: datetime
    updated_at: datetime

    model_config = {"from_attributes": True, "arbitrary_types_allowed": True}


class WorkspaceUpdateModel(BaseModel):
    title: Optional[str] = None
    config: Optional[WorkspaceDefaultConfigModel] = Field(default_factory=dict)

    model_config = {
        "from_attributes": True,
    }


class ConversationModel(BaseModel):
    conversation_id: uuid.UUID
    workspace_id: uuid.UUID
    title: Optional[str] = None
    archived: Optional[bool] = False
    language_code: Optional[str] = "english"
    created_at: datetime
    updated_at: datetime

    model_config = {"from_attributes": True, "arbitrary_types_allowed": True}


class ConversationCreateModel(BaseModel):
    workspace_id: uuid.UUID
    title: Optional[str]
    language_code: Optional[str] = "english"

    model_config = {
        "from_attributes": True,
    }


class ConversationUpdateModel(BaseModel):
    title: Optional[str]
    language_code: Optional[str] = "english"

    model_config = {
        "from_attributes": True,
    }


class MessageModel(BaseModel):
    message_id: uuid.UUID
    conversation_id: uuid.UUID
    message_number: int
    content: dict
    language_code: Optional[str] = "english"
    created_at: datetime
    updated_at: datetime
    extra_metadata: Optional[dict] = None

    model_config = {"from_attributes": True, "arbitrary_types_allowed": True}


class MessageWithConversationModel(BaseModel):
    message: MessageModel
    conversation: ConversationModel

    model_config = {
        "from_attributes": True,
    }


class WorkspaceWithConversations(WorkspaceModel):
    conversations: List[ConversationModel]

    model_config = {
        "from_attributes": True,
    }
