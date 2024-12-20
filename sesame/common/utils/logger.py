import sys
import os
from loguru import logger

_logger_initialized = False

def get_logger():
    global _logger_initialized
    if not _logger_initialized:
        try:
            logger.remove()
        except ValueError:
            pass
        logger.add(sys.stderr, level=os.getenv("SESAME_BOT_LOG_LEVEL", "DEBUG"))
        _logger_initialized = True
    return logger
