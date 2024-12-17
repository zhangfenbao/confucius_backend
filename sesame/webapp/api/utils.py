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

# 定义默认的 UUID
DEFAULT_MESSAGE_ID = uuid.UUID('00000000-0000-0000-0000-000000000000')

@router.post(
    "/parse-file", 
    response_model=FileParseResponse,
    status_code=status.HTTP_200_OK
)
async def parse_file(
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
        
        # 如果 message_id 为 None，使用默认值
        actual_message_id = message_id if message_id is not None else DEFAULT_MESSAGE_ID
        
        # 创建附件记录
        attachment = await Attachment.create_attachment(
            message_id=actual_message_id,
            file_url=None,  # 暂时为None
            file_name=file.filename.split('.')[0],
            file_type=file.filename.split('.')[-1].lower(),
            content={'content': markdown_content},
            db=db
        )
        
        return FileParseResponse(
            content=markdown_content,
            attachment_id=attachment.attachment_id
        )
        
    except Exception as e:
        # 获取数据库用户和权限信息
        debug_info = {}
        try:
            async with db.begin():
                result = await db.execute(text("SELECT current_user"))
                debug_info['current_user'] = result.scalar()
                
                result = await db.execute(text("""
                    SELECT has_table_privilege(current_user, 'attachments', 'INSERT') as has_insert,
                           has_table_privilege(current_user, 'attachments', 'SELECT') as has_select
                """))
                debug_info['privileges'] = dict(result.mappings().first())
        except Exception as debug_e:
            debug_info['error'] = str(debug_e)
            
        raise HTTPException(
            status_code=status.HTTP_500_INTERNAL_SERVER_ERROR, 
            detail=f"PDF解析失败: {str(e)}\n调试信息: {debug_info}"
        )