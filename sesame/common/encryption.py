import os
from base64 import b64encode

from cryptography.fernet import Fernet
from cryptography.hazmat.primitives import hashes
from cryptography.hazmat.primitives.kdf.pbkdf2 import PBKDF2HMAC


def get_encryption_key(salt: bytes) -> bytes:
    """Derive a key from SESAME_APP_SECRET using PBKDF2"""
    kdf = PBKDF2HMAC(
        algorithm=hashes.SHA256(),
        length=32,
        salt=salt,
        iterations=480000,
    )
    key = kdf.derive(os.environ["SESAME_APP_SECRET"].encode())
    return b64encode(key)


def encrypt_with_secret(string: str) -> str:
    """Encrypt a string using Fernet encryption with a derived key"""
    # Use first 16 bytes of app secret as salt
    salt = os.environ["SESAME_APP_SECRET"].encode()[:16]
    key = get_encryption_key(salt)
    f = Fernet(key)
    return f.encrypt(string.encode()).decode()


def decrypt_with_secret(encrypted_string: str) -> str:
    """Decrypt an API key using Fernet encryption with a derived key"""
    salt = os.environ["SESAME_APP_SECRET"].encode()[:16]
    key = get_encryption_key(salt)
    f = Fernet(key)
    return f.decrypt(encrypted_string.encode()).decode()
