import pymupdf4llm
import io

async def parse_pdf_to_markdown(pdf_bytes: bytes) -> str:
    """
    将PDF字节数据转换为Markdown格式
    
    Args:
        pdf_bytes: PDF文件的字节数据
        
    Returns:
        str: Markdown格式的文本
    """
    try:
        # 使用BytesIO创建内存中的文件对象
        pdf_stream = io.BytesIO(pdf_bytes)
        md_text = pymupdf4llm.to_markdown(pdf_stream)
        return md_text
    except Exception as e:
        raise Exception(f"PDF解析失败: {str(e)}")
