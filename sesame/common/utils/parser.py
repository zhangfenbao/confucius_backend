import pymupdf
import pymupdf4llm
import io
import re
import json
from typing import List, Dict, Any
from fastapi import HTTPException, status

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

async def merge_messages_with_attachment(messages: list[Any], attachment: Any) -> List[Dict]:
    """
    将消息与附件内容合并
    
    Args:
        messages: 输入的消息列表
        attachment: 附件对象
        
    Returns:
        List[Dict]: 合并后的消息列表
    """
    # 验证输入messages格式
    input_texts = []
    for msg in messages:
        if not isinstance(msg, dict) or 'content' not in msg:
            raise HTTPException(
                status_code=status.HTTP_400_BAD_REQUEST,
                detail=f"Invalid message format: {msg}"
            )
        if 'type' not in msg['content'] or msg['content']['type'] != 'text':
            continue
        input_texts.append(msg['content']['text'])
    
    # 验证attachment
    if not attachment or not attachment.content:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail="Attachment not found or empty"
        )
    
    result_messages = []
    image_counter = 1
    processed_pages = []
    
    try:
        # 处理content中的每一页
        for page_content in attachment.content:
            # 使用正则表达式找出所有base64图片
            base64_pattern = r'!\[.*?\]\(data:image\/[^;]+;base64,[^)]+\)'
            image_matches = re.finditer(base64_pattern, page_content)
            
            # 替换base64为图片标记并收集base64数据
            processed_content = page_content
            for match in image_matches:
                # 提取base64数据
                base64_data = re.search(r'data:image\/[^;]+;base64,([^)]+)', match.group(0)).group(0)
                
                # 添加图片消息
                result_messages.append({
                    'type': 'image_url',
                    'image_url': {'url': base64_data}
                })
                
                # 在文本中替换base64为图片标记
                processed_content = processed_content.replace(
                    match.group(0), 
                    f'[图片{image_counter}]'
                )
                image_counter += 1
            
            # 将处理后的文本添加到页面列表中
            if processed_content.strip():
                processed_pages.append(processed_content.strip())
        
        # 将所有页面内容转换为JSON字符串
        content_json = json.dumps(processed_pages, ensure_ascii=False)
        
        # 组合输入文本和内容JSON
        combined_text = f"{' '.join(input_texts)}\n\n{content_json}"
        
        # 添加组合后的文本消息
        result_messages.append({
            'type': 'text',
            'text': combined_text
        })
    
    except Exception as e:
        raise HTTPException(
            status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
            detail=f"Error processing attachment content: {str(e)}"
        )
    
    return [{"role": "user", "content": result_messages}]


