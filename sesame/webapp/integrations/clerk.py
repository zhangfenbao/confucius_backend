import os
from typing import Optional

import httpx
from loguru import logger


class ClerkAuth:
    def __init__(self):
        self.api_base = "https://api.clerk.dev/v1"
        self.api_secret = os.getenv("SESAME_CLERK_SECRET_KEY")
        if not self.api_secret:
            raise ValueError("SESAME_CLERK_SECRET_KEY not set")

    async def verify_session(self, session_token: str) -> Optional[str]:
        """
        Verify a session token with Clerk and return the session data if valid.
        """
        async with httpx.AsyncClient() as client:
            headers = {
                "Authorization": f"Bearer {self.api_secret}",
                "Content-Type": "application/json",
            }

            try:
                response = await client.get(
                    f"{self.api_base}/sessions/{session_token}",
                    headers=headers,
                )

                if response.status_code == 200:
                    session_data = response.json()
                    if session_data.get("status") == "active":
                        return await self.get_user_details(session_data.get("user_id"))
                    return None
                return None
            except Exception as e:
                logger.debug(f"Error verifying session: {str(e)}")
                return None

    async def get_user_details(self, user_id: str) -> Optional[str]:
        """
        Fetch user details from Clerk using the user ID
        """
        async with httpx.AsyncClient() as client:
            headers = {
                "Authorization": f"Bearer {self.api_secret}",
                "Content-Type": "application/json",
            }

            try:
                response = await client.get(
                    f"{self.api_base}/users/{user_id}",
                    headers=headers,
                )

                if response.status_code == 200:
                    user_data = response.json()
                    primary_email_id = user_data.get("primary_email_address_id")
                    if not primary_email_id:
                        return None
                    email_addresses = user_data.get("email_addresses", [])
                    primary_email = next(
                        (
                            email["email_address"]
                            for email in email_addresses
                            if email["id"] == primary_email_id
                        ),
                        None,
                    )
                    return primary_email

                return None
            except Exception as e:
                print(f"Error fetching user details: {str(e)}")
                return None
