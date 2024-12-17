import pymupdf
import pymupdf4llm
import io

async def parse_pdf_to_markdown(pdf_bytes: bytes) -> list:
    """
    将PDF字节数据按页转换为Markdown格式
    
    Args:
        pdf_bytes: PDF文件的字节数据
        
    Returns:
        list: 每页对应的Markdown文本列表
    """
    try:
        # 使用BytesIO创建内存中的文件对象
        pdf_stream = io.BytesIO(pdf_bytes)
        doc = pymupdf.open(stream=pdf_stream, filetype="pdf")
        
        # 设置page_chunks=True来获取按页分割的结果
        md_chunks = pymupdf4llm.to_markdown(doc, 
                                          embed_images=True,
                                          write_images=True,
                                          force_text=False,
                                          page_chunks=True)  # 设置为True
        
        # 提取每页的markdown文本
        md_texts = [chunk["text"] for chunk in md_chunks]
        return md_texts
        
    except Exception as e:
        raise Exception(f"PDF解析失败: {str(e)}")


