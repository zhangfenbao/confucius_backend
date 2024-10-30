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


class ServiceFactoryError(Exception):
    """Base exception for service configuration errors"""

    pass


class UnsupportedServiceError(ServiceFactoryError):
    def __init__(self, service_name: str, service_type: str, valid_services: list[str]):
        self.service_name = service_name
        self.service_type = service_type
        self.valid_services = valid_services
        super().__init__(
            f"Service '{service_name}' is not a valid {service_type} service. "
            f"Valid {service_type} services are: {', '.join(valid_services)}"
        )


class InvalidServiceTypeError(ServiceFactoryError):
    def __init__(self, service_type: str, valid_types: list[str]):
        self.service_type = service_type
        self.valid_types = valid_types
        super().__init__(
            f"Invalid service type '{service_type}'. " f"Must be one of: {', '.join(valid_types)}"
        )
