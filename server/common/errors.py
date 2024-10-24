from typing import Optional


class ServiceConfigurationError(Exception):
    """Exception raised for service configuration and retrieval errors"""

    def __init__(
        self,
        message: str,
        missing_services: Optional[list[str]] = None,
        service_type: Optional[str] = None,
    ):
        self.message = message
        self.missing_services = missing_services
        self.service_type = service_type
        super().__init__(self.message)
