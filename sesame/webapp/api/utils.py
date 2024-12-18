from fastapi import APIRouter, UploadFile, File, HTTPException, status, Depends
from pydantic import BaseModel
from common.utils.parser import parse_pdf_to_markdown
from sqlalchemy.ext.asyncio import AsyncSession
from common.models import Attachment, FileParseResponse
from webapp import get_db
import uuid
from typing import Optional
from sqlalchemy import text

router = APIRouter(prefix="/utils")

@router.post(
    "/parse-file", 
    response_model=FileParseResponse,
    status_code=status.HTTP_200_OK
)
async def parse_file(
    conversation_id: uuid.UUID,
    message_id: Optional[uuid.UUID] = None,
    file: UploadFile = File(...),
    db: AsyncSession = Depends(get_db),
):
    """
    解析上传的PDF文件，保存为附件并返回Markdown格式的内容
    """
    if not file.filename.lower().endswith('.pdf'):
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST, 
            detail="只支持PDF文件格式"
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
            content={'content': markdown_content},
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