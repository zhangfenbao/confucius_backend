def llm_message_normalize(messages):
    # Track if input was a single message
    is_single = isinstance(messages, dict)

    # Convert single message to list for processing
    if is_single:
        messages = [messages]

    normalized_messages = []
    for message in messages:
        role = message.get("role")
        content = message.get("content")
        if isinstance(content, str):
            content = [{"text": content}]
        normalized_messages.append({"role": role, "content": content})

    # Return single message if input was single message
    return normalized_messages[0] if is_single else normalized_messages
