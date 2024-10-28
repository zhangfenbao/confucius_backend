FROM python:3.12.7-slim-bookworm

ARG DEBIAN_FRONTEND=noninteractive
ENV PYTHONUNBUFFERED=1 \
    HOME=/home/user \
    PATH=/home/user/.local/bin:$PATH \
    PYTHONPATH=$HOME/app

# Expose FastAPI port
EXPOSE 7860

# Install system dependencies
RUN apt-get update && apt-get install --no-install-recommends -y \
    build-essential \
    ffmpeg \
    google-perftools \
    ca-certificates curl gnupg \
    libpq-dev \
    && apt-get clean && rm -rf /var/lib/apt/lists/* && rm -rf /var/cache/apt/*

# Set up a new user named "user" with user ID 1000
RUN useradd -m -u 1000 user

# Install Python dependencies before copying the whole project to leverage layer caching
WORKDIR /tmp

# Install dependencies
COPY webapp/requirements.txt \
    bots/requirements.txt \
    ./
RUN cat *requirements.txt > combined-requirements.txt \
    && pip3 install --no-cache-dir -r combined-requirements.txt \
    && rm *requirements.txt \
    && rm -rf ~/.cache/pip/*

# Now switch to non-root user
USER user
WORKDIR $HOME/app

# Copy the entire project as the last step
COPY --chown=user . .

# Start the FastAPI server
CMD ["uvicorn", "webapp.main:app", "--host", "0.0.0.0", "--port", "7860"]