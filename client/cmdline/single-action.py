import base64
import json

import requests


def send_request_and_process_events(url, headers, data):
    try:
        with requests.post(url, headers=headers, json=data, stream=True) as response:
            response.raise_for_status()
            for line in response.iter_lines():
                if line:
                    decoded_line = line.decode("utf-8")
                    if decoded_line.startswith("data: "):
                        event_data = decoded_line[6:]  # Remove 'data: ' prefix
                        try:
                            decoded_event = base64.b64decode(event_data).decode("utf-8")
                            data = json.loads(decoded_event)
                            if data.get("type") == "bot-llm-text":
                                text_content = data.get("data", {}).get("text", "")
                                print(text_content, end="", flush=True)
                        except Exception as e:
                            print(f"Error decoding event: {e}")
    except requests.exceptions.RequestException as e:
        print(f"HTTP Request failed: {e}")
    except Exception as e:
        print(f"An error occurred: {e}")


if __name__ == "__main__":
    url = "http://localhost:8000/api/rtvi/action"
    headers = {"Content-Type": "application/json", "Authorization": "Bearer hello"}
    data = {
        "conversation_id": "c6c8046e-e4ba-4c9d-bf22-6d36323d089d",
        "workspace_id": "21f39445-5135-45cd-be7a-794b2ea0235b",
        "actions": [
            {
                "label": "rtvi-ai",
                "type": "action",
                "id": "23",
                "data": {
                    "service": "llm",
                    "action": "set_context",
                    "arguments": [
                        {
                            "name": "messages",
                            "value": [
                                {"role": "user", "content": "Tell me a story about a unicorn!"},
                            ],
                        },
                        # {"name": "run_immediately", "value": False},
                    ],
                },
            },
            # {
            #     "label": "rtvi-ai",
            #     "type": "action",
            #     "id": "23",
            #     "data": {
            #         "service": "llm",
            #         "action": "append_to_messages",
            #         "arguments": [
            #             {
            #                 "name": "messages",
            #                 "value": [
            #                     {
            #                         "role": "user",
            #                         "content": "Tell me another one, this time about a penguin!",
            #                     }
            #                 ],
            #             }
            #         ],
            #     },
            # },
            # {
            #     "label": "rtvi-ai",
            #     "type": "action",
            #     "id": "23",
            #     "data": {"service": "llm", "action": "run", "arguments": []},
            # },
        ],
    }

    send_request_and_process_events(url, headers, data)
