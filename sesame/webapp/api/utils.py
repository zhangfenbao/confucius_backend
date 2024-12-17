from fastapi import APIRouter, UploadFile, File, HTTPException, status
from pydantic import BaseModel
from common.utils.parser import parse_pdf_to_markdown

router = APIRouter(prefix="/utils")

class PDFParseResponse(BaseModel):
    content: str

@router.post(
    "/parse-file", 
    response_model=PDFParseResponse,
    status_code=status.HTTP_200_OK
)
async def parse_pdf(
    file: UploadFile = File(...),
):
    """
    解析上传的PDF文件并返回Markdown格式的内容
    """
    if not file.filename.lower().endswith('.pdf'):
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST, 
            detail="只支持PDF文件格式"
        )
        
    try:
        # 直接读取上传文件的内容到内存
        content = await file.read()
        
        # 解析PDF
        markdown_content = await parse_pdf_to_markdown(content)
        
        return PDFParseResponse(content=markdown_content)
        
    except Exception as e:
        raise HTTPException(
            status_code=status.HTTP_500_INTERNAL_SERVER_ERROR, 
            detail=f"PDF解析失败: {str(e)}"
        )