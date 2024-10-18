#!/bin/bash

echo "creating a workspace and conversation"

# get ENV VARS
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
source ${SCRIPT_DIR}/../server/.env

if [ -z ${DAILY_API_KEY} ];then
  echo "no DAILY_API_KEY found. please check the script at ${SCRIPT_DIR} and address."
  exit 1
fi

# todo unique-ify the names with POSIX friendly `date +%s`
WORKSPACE=$(curl -s --request POST http://localhost:8000/api/workspaces \
-H "Authorization: Bearer $SESAME_APP_SECRET" \
-H "Content-Type: application/json" \
-d '{
  "title": "test-workspace-01",
  "config": {
    "api_keys": {
      "cartesia": "'"${CARTESIA_API_KEY}"'",
      "daily": "'"${DAILY_API_KEY}"'",
      "deepgram": "'"${DEEPGRAM_API_KEY}"'",
      "together": "'"${TOGETHER_API_KEY}"'"
    },
    "config" : [
      {
         "options" : [
            {
               "name" : "params",
               "value" : {
                  "stop_secs" : 0.3
               }
            }
         ],
         "service" : "vad"
      },
      {
         "options" : [
            {
               "name" : "voice",
               "value" : "79a125e8-cd45-4c13-8a67-188112f4dd22"
            },
            {
               "name" : "model",
               "value" : "sonic-english"
            },
            {
               "name" : "language",
               "value" : "en"
            }
         ],
         "service" : "tts"
      },
      {
         "options" : [
            {
               "name" : "model",
               "value" : "meta-llama/Meta-Llama-3.1-8B-Instruct-Turbo"
            },
            {
               "name" : "run_on_config",
               "value" : false
            }
         ],
         "service" : "llm"
      },
      {
         "options" : [
            {
               "name" : "model",
               "value" : "nova-2-conversationalai"
            },
            {
               "name" : "language",
               "value" : "en"
            }
         ],
         "service" : "stt"
      }
    ],
    "default_llm_context": [
      {
      "content": {
          "role": "system",
          "content": "You are ExampleBot, a helpful assistant."
      },
      "extra_metadata": null
      }
    ],
    "services": {
      "tts": "cartesia",
      "llm": "together",
      "stt": "deepgram"
    }
  }
}' |jq -r .workspace_id)
# =>
# {"workspace_id":"b54e7073-2575-4e51-a6e6-7c511478b58a","title":"test-workspace-01","config":{"config":[{"options":[{"name":"model","value":"meta-llama/Meta-Llama-3.1-8B-Instruct-Turbo"},{"name":"messages","value":[{"role":"system","content":"You are Frances, a helpful assistant."}]},{"name":"run_on_config","value":false}],"service":"llm"}],"api_keys":{"cartesia":"XXX","deepgram":"XXX","together":"XXX"},"services":{"llm":"together","stt":"deepgram","tts":"cartesia"},"daily_api_key":"pc-XXXXX"},"created_at":"2024-09-26T21:55:34.279358Z","updated_at":"2024-09-26T21:55:34.279358Z"}
echo "workspace_id: ${WORKSPACE}"

CONVERSATION=$(curl -s --request POST http://localhost:8000/api/conversations \
-H "Authorization: Bearer $SESAME_APP_SECRET" \
-H "Content-Type: application/json" \
-d '{
  "workspace_id": "'"${WORKSPACE}"'",
  "title": "test-conversation-'"${WORKSPACE}"'"
}' | jq -r .conversation_id)
# =>
# {"conversation_id":"011447dc-c12b-409b-bd96-71970fdaae75","workspace_id":"b54e7073-2575-4e51-a6e6-7c511478b58a","title":"test-conversation-01","archived":false,"language_code":"english","created_at":"2024-09-26T21:56:10.159786Z","updated_at":"2024-09-26T21:56:10.159786Z"}
echo "conversation_id: ${CONVERSATION}"

curl --request POST --url http://localhost:8000/api/rtvi/connect \
-H "Authorization: Bearer $SESAME_APP_SECRET" \
-H "Content-Type: application/json" -d '{
  "conversation_id": "'"${CONVERSATION}"'",
  "workspace_id": "'"${WORKSPACE}"'",
  "actions": [
    {
      "label": "rtvi-ai",
      "type": "action",
      "id": "1234",
      "data": {
        "service": "llm",
        "action": "append_to_messages",
        "arguments": [
          {
            "name": "messages",
            "value": [
              {
                "role": "user",
                "content": "What is a solar eclipse?"
              }
            ]
          }
        ]
      }
    }
  ]
}' | jq

