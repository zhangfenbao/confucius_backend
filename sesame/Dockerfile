# Build stage
FROM python:3.12.7-slim-bookworm AS builder

# Set up environment variables
ARG DEBIAN_FRONTEND=noninteractive
ENV PYTHONUNBUFFERED=1 \
    PYTHONPATH=/app \
    PIP_NO_CACHE_DIR=1

WORKDIR /app

# Install build dependencies
RUN apt-get update && \
    apt-get install --no-install-recommends -y \
    build-essential \
    libpq-dev \
    && rm -rf /var/lib/apt/lists/* /var/cache/apt/*

# Install Python dependencies (removing additions to reduce layer size)
COPY webapp/requirements.txt webapp/
COPY bots/requirements.txt bots/
RUN pip3 install --no-cache-dir --prefer-binary -r webapp/requirements.txt -r bots/requirements.txt \
    && rm -rf /usr/local/lib/python3.12/site-packages/*.dist-info \
    && rm -rf /usr/local/lib/python3.12/site-packages/*.egg-inf

# Final stage
FROM python:3.12.7-slim-bookworm

ENV PYTHONUNBUFFERED=1 \
    PYTHONPATH=/app

WORKDIR /app

# Install only runtime dependencies
RUN apt-get update && \
    apt-get install --no-install-recommends -y \
    libpq5 \
    && rm -rf /var/lib/apt/lists/* /var/cache/apt/*

# Create user
RUN useradd -m -u 1000 user
USER user

# Copy builder and application files
COPY --from=builder /usr/local/lib/python3.12/site-packages/ /usr/local/lib/python3.12/site-packages/
COPY --chown=user . .

EXPOSE 7860

CMD ["python", "-m", "uvicorn", "webapp.main:app", "--host", "0.0.0.0", "--port", "7860"]