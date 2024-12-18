import base64
import os
import uuid
from datetime import datetime, timedelta, timezone
from typing import Any, List, Optional

from common.encryption import decrypt_with_secret
from common.errors import ServiceConfigurationError
from common.service_factory import ServiceType
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
    UniqueConstraint,
    func,
    select,
    text,
)
from sqlalchemy.dialects.postgresql import JSONB, TSVECTOR, UUID
from sqlalchemy.exc import IntegrityError
from sqlalchemy.ext.asyncio import AsyncSession
from sqlalchemy.orm import (
    Mapped,
    declarative_base,
    joinedload,
    mapped_column,
    relationship,
)
from sqlalchemy.sql import expression

from pipecat.processors.frameworks.rtvi import RTVIServiceConfig

Base = declarative_base()

# ==========================
# SQLAlchemy Models
# ==========================


class User(Base):
    __tablename__ = "users"

    user_id = Column(String(64), primary_key=True)
    email = Column(String(255), unique=True, nullable=False)
    password_hash = Column(String, nullable=False)
    created_at = Column(TIMESTAMP(timezone=True), server_default=func.now())
    updated_at = Column(TIMESTAMP(timezone=True), server_default=func.now(), onupdate=func.now())

    tokens: Mapped[List["Token"]] = relationship("Token", back_populates="user")

    __table_args__ = (Index("idx_users_email", "email"),)

    @classmethod
    async def get_user_by_email(cls, email: str, db: AsyncSession):
        result = await db.execute(select(User).where(User.email == email))
        return result.scalars().first()

    @classmethod
    async def authenticate_by_email(cls, email: str, db: AsyncSession):
        try:
            result = await db.execute(
                text("SELECT * FROM get_user_for_login(:email)"), {"email": email}
            )
            user = result.fetchone()
            if not user:
                raise
        except Exception:
            raise Exception(f"Unable to authenticate user with Email {email}")

        try:
            await db.execute(
                text("SELECT set_current_user_id(:user_id)"), {"user_id": user.user_id}
            )
        except Exception:
            raise Exception(f"Unable to set current user with id {user.user_id}")

        return user


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
        cls,
        user_id: str,
        db: AsyncSession,
        title: Optional[str] = None,
        expiration_minutes: int = 30,
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
    async def get_or_create_token_for_user(
        cls,
        user_id: str,
        db: AsyncSession,
        title: Optional[str] = None,
        expiration_minutes: Optional[int] = None,
    ):
        try:
            query = select(Token).where(Token.user_id == user_id, Token.revoked.is_(False)).limit(1)
            token = (await db.execute(query)).scalar_one_or_none()
            if not token:
                expiry = expiration_minutes or int(os.getenv("SESAME_TOKEN_EXPIRY", 525600))
                token = await Token.create_token_for_user(
                    user_id=user_id,
                    db=db,
                    title=title or "Generated token",
                    expiration_minutes=expiry,
                )
                await db.commit()
            return token
        except Exception as e:
            await db.rollback()
            raise Exception(f"Error obtaining token for user: {str(e)}")

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

    conversations: Mapped[List["Conversation"]] = relationship(
        "Conversation", back_populates="workspace"
    )

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
    attachments: Mapped[List["Attachment"]] = relationship(
        "Attachment", 
        back_populates="conversation"
    )

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
        Index(
            "idx_messages_conversation_number",
            "conversation_id",
            "message_number",
            unique=True,
        ),
    )
    """
    # Enable this to normalize content shape on save
    @validates("content")
    def validate_content(self, key, content):
        from common.utils.llm import llm_message_normalize
        normalized = llm_message_normalize(content)
        return normalized
    """

    @classmethod
    async def get_messages_by_conversation_id(cls, conversation_id: str, db: AsyncSession):
        result = await db.execute(
            select(Message)
            .where(Message.conversation_id == conversation_id)
            .order_by(Message.message_number)
        )
        return result.scalars().all()

    @classmethod
    async def save_messages(
        cls, conversation_id: str, language_code: str, messages: List[Any], db: AsyncSession
    ):
        ms = []
        for i, message_data in enumerate(messages, start=1):
            m = Message(
                conversation_id=conversation_id,
                content=message_data,
                language_code=language_code or "english",
            )
            ms.append(m)
        try:
            db.add_all(ms)
            await db.flush()
        except IntegrityError as e:
            await db.rollback()
            raise e
        except Exception as e:
            await db.rollback()
            raise e


class Attachment(Base):
    __tablename__ = "attachments"

    attachment_id = Column(UUID(as_uuid=True), primary_key=True, default=uuid.uuid4)
    conversation_id = Column(
        UUID(as_uuid=True),
        ForeignKey("conversations.conversation_id", ondelete="CASCADE"),
        nullable=False,
    )
    message_id = Column(
        UUID(as_uuid=True),
        ForeignKey("messages.message_id", ondelete="SET NULL"),
        nullable=True,
    )
    file_url = Column(String, nullable=True)
    file_name = Column(String(255), nullable=False)
    file_type = Column(String(50), nullable=False)
    created_at = Column(TIMESTAMP(timezone=True), server_default=func.now())
    content = Column(JSONB, nullable=False)
    
    conversation: Mapped["Conversation"] = relationship(
        "Conversation", 
        back_populates="attachments"
    )
    message: Mapped[Optional["Message"]] = relationship(
        "Message", 
        back_populates="attachments"
    )

    __table_args__ = (
        Index("idx_attachments_conversation_id", "conversation_id"),
        Index("idx_attachments_message_id", "message_id"),
    )

    @classmethod
    async def create_attachment(
        cls,
        conversation_id: uuid.UUID,
        file_url: str,
        file_name: str,
        file_type: str,
        content: dict,
        db: AsyncSession,
    ):
        new_attachment = cls(
            conversation_id=conversation_id,
            file_url=file_url,
            file_name=file_name,
            file_type=file_type,
            content=content,
        )
        db.add(new_attachment)
        await db.commit()
        return new_attachment

    @classmethod
    async def link_to_message(
        cls,
        attachment_id: uuid.UUID,
        message_id: uuid.UUID,
        db: AsyncSession,
    ):
        result = await db.execute(
            select(cls).where(cls.attachment_id == attachment_id)
        )
        attachment = result.scalar_one_or_none()
        if attachment:
            attachment.message_id = message_id
            attachment.status = "linked"
            await db.commit()
            return attachment
        return None


class Service(Base):
    __tablename__ = "services"

    service_id = Column(UUID(as_uuid=True), primary_key=True, default=uuid.uuid4)
    user_id = Column(String(64), ForeignKey("users.user_id", ondelete="CASCADE"), nullable=False)
    workspace_id = Column(
        UUID(as_uuid=True),
        ForeignKey("workspaces.workspace_id", ondelete="CASCADE"),
        nullable=True,
    )
    title = Column(String(255), nullable=False)
    service_type = Column(String(255), nullable=False)
    service_provider = Column(String(255), nullable=False)
    api_key = Column(String, nullable=False)
    options = Column(JSONB, nullable=True, default=dict)
    created_at = Column(TIMESTAMP(timezone=True), server_default=func.now())
    updated_at = Column(TIMESTAMP(timezone=True), server_default=func.now(), onupdate=func.now())

    __table_args__ = (
        UniqueConstraint(
            "workspace_id",
            "service_provider",
            "service_type",
            name="unique_provider_per_workspace",
        ),
        UniqueConstraint(
            "user_id",
            "service_provider",
            "service_type",
            name="unique_provider_per_user",
            info=dict(where="workspace_id IS NULL"),
        ),
        # Indexes
        Index("idx_services_user_id", "user_id"),
        Index("idx_services_workspace_id", "workspace_id"),
        Index("idx_services_service_type", "service_type"),
    )

    @classmethod
    async def get_services_by_user(cls, db: AsyncSession):
        result = await db.execute(select(Service).order_by(Service.created_at.desc()))
        services = result.scalars().all()
        for service in services:
            service.api_key = "hidden"

        return services

    @classmethod
    async def get_services_by_provider(
        cls, provider: str, db: AsyncSession, workspace_id: Optional[uuid.UUID] = None
    ) -> Optional["Service"]:
        result = await db.execute(
            select(Service)
            .where(Service.service_provider == provider)
            .order_by(Service.updated_at.desc())
        )
        services = result.scalars().all()

        if not services:
            return None

        if workspace_id:
            workspace_service = next(
                (service for service in services if str(service.workspace_id) == str(workspace_id)),
                None,
            )
            service = workspace_service or services[0]
        else:
            service = services[0]

        decrypted_key = decrypt_with_secret(str(service.api_key))
        db.expunge(service)
        service.api_key = decrypted_key
        return service

    @classmethod
    async def get_services_by_type_map(
        cls,
        workspace_services: dict[str, str],
        db: AsyncSession,
        workspace_id: Optional[uuid.UUID] = None,
        service_type_filter: Optional[ServiceType] = None,
    ) -> dict[str, "Service"]:
        if service_type_filter:
            service_type_str = service_type_filter.value
            provider = workspace_services.get(service_type_str)
            conditions = [
                Service.service_type == service_type_str,
                Service.service_provider == provider if provider else True,
            ]
            services_to_check = {service_type_str: workspace_services.get(service_type_str)}
        else:
            conditions = [
                Service.service_type.in_(workspace_services.keys()),
                Service.service_provider.in_(workspace_services.values()),
            ]
            services_to_check = workspace_services

        query = select(Service).where(*conditions).order_by(Service.updated_at.desc())

        result = await db.execute(query)
        services = result.scalars().all()

        # Group services by type
        services_by_type: dict[str, list[Service]] = {}
        for service in services:
            # Convert Column to string for comparison
            if str(service.service_provider) == str(workspace_services[str(service.service_type)]):
                service_type = str(service.service_type)
                if service_type not in services_by_type:
                    services_by_type[service_type] = []
                services_by_type[service_type].append(service)

        # Build final result prioritizing workspace services
        final_services: dict[str, Service] = {}
        missing_types = []

        for service_type, provider in services_to_check.items():
            services_of_type = services_by_type.get(service_type, [])

            if not services_of_type:
                missing_types.append(f"{service_type} ({provider})")
                continue

            # Try to find workspace-specific service first
            workspace_service = None
            user_service = None

            for service in services_of_type:
                if workspace_id and str(service.workspace_id) == str(workspace_id):
                    workspace_service = service
                    break
                elif service.workspace_id is None:
                    user_service = service

            # Use workspace service if found, otherwise fall back to user service
            if workspace_service:
                service = workspace_service
            elif user_service:
                service = user_service
            else:
                missing_types.append(f"{service_type} ({provider})")
                continue

            # Decrypt API key before returning
            decrypted_key = decrypt_with_secret(str(service.api_key))
            db.expunge(service)
            service.api_key = decrypted_key
            final_services[service_type] = service

        if missing_types:
            raise ServiceConfigurationError(
                "Required services not found", missing_services=missing_types
            )

        return final_services


# ==========================
# Pydantic Models
# ==========================


class UserLoginModel(BaseModel):
    email: str
    password: str

    model_config = {"from_attributes": True}


class UserModel(BaseModel):
    user_id: str
    email: str
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

    model_config = {
        "extra": "allow",
        "from_attributes": True,
        "arbitrary_types_allowed": True,
    }


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
    title: Optional[str] = None
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


class ServiceCreateModel(BaseModel):
    title: str
    service_type: str
    service_provider: Optional[str] = None
    api_key: str
    workspace_id: Optional[uuid.UUID] = None
    options: Optional[dict] = Field(default_factory=dict)

    model_config = {
        "from_attributes": True,
    }


class ServiceUpdateModel(BaseModel):
    title: Optional[str] = None
    api_key: Optional[str] = None
    options: Optional[dict] = None

    model_config = {
        "from_attributes": True,
    }


class ServiceModel(BaseModel):
    service_id: uuid.UUID
    user_id: str
    workspace_id: Optional[uuid.UUID]
    title: str
    service_type: str
    service_provider: Optional[str]
    api_key: str
    options: Optional[dict]
    created_at: datetime
    updated_at: datetime

    model_config = {
        "from_attributes": True,
    }

class FileParseResponse(BaseModel):
    attachment_id: uuid.UUID
    content: dict

    model_config = {
        "from_attributes": True,
    }