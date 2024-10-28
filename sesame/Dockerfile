# Base image
FROM python:3.12.7-slim-bookworm

# Set up environment variables
ARG DEBIAN_FRONTEND=noninteractive
ENV PYTHONUNBUFFERED=1 \
    PYTHONPATH=/app \
    PIP_NO_CACHE_DIR=1

# Install system dependencies
RUN apt-get update && \
    apt-get install --no-install-recommends -y \
    build-essential \
    libpq-dev \
    && pip3 install --no-cache-dir wheel && \
    useradd -m -u 1000 user && \
    apt-get clean && \
    rm -rf /var/lib/apt/lists/* /var/cache/apt/* && \
    rm -rf ~/.cache/pip/*

# Set working directory
WORKDIR /app

# Copy requirements files
COPY webapp/requirements.txt webapp/
COPY bots/requirements.txt bots/
RUN pip3 install --no-cache-dir -r webapp/requirements.txt -r bots/requirements.txt && \
    rm -rf webapp bots ~/.cache/pip/*

# Switch to non-root user
USER user

# Copy the entire project
COPY --chown=user . .

# Expose FastAPI port
EXPOSE 7860

# Run FastAPI
CMD ["uvicorn", "webapp.main:app", "--host", "0.0.0.0", "--port", "7860"]