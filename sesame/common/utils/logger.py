import sys
import os
from loguru import logger

_bot_logger_initialized = False
_webapp_logger_initialized = False

def get_bot_logger():
    bot_logger = logger.bind(context="bot")
    global _bot_logger_initialized
    if not _bot_logger_initialized:
        try:
            bot_logger.remove()
        except ValueError:
            pass
        bot_logger.add(sys.stderr, level=os.getenv("SESAME_BOT_LOG_LEVEL", "DEBUG"))
        _bot_logger_initialized = True
    return bot_logger

def get_webapp_logger():
    # 创建一个新的 logger 实例
    webapp_logger = logger.bind(context="webapp")
    global _webapp_logger_initialized
    if not _webapp_logger_initialized:
        try:
            webapp_logger.remove()
        except ValueError:
            pass
        webapp_logger.add(sys.stderr, level=os.getenv("SESAME_WEBAPP_LOG_LEVEL", "INFO"))
        _webapp_logger_initialized = True
    return webapp_logger
