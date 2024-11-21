import { LLMMessageRole } from "./llm";
import { getApiClient } from "./sesameApiClient";

interface TextContent {
  type: "text";
  text: string;
}

interface ImageContent {
  type: "image_url";
  image_url: {
    url: string;
  };
}

export interface Message {
  created_at: string;
  content: {
    role: LLMMessageRole;
    content: string | Array<TextContent | ImageContent>;
  };
  conversation_id: string;
  extra_metadata: Record<string, unknown> | null;
  message_id: string;
  message_number: number;
  updated_at: string;
}

export async function getMessages(conversationId: string): Promise<Message[]> {
  try {
    const apiClient = await getApiClient();
    const response =
      await apiClient.api.getConversationAndMessagesApiConversationsConversationIdMessagesGet(
        conversationId
      );
    if (response.ok) {
      const json = await response.json();
      if (json.detail === "No messages found for this conversation") {
        return [];
      }
      return json.messages as Message[];
    } else {
      throw new Error(
        `Error fetching messages: ${response.status} ${response.statusText}`
      );
    }
  } catch (e) {
    console.error(e);
    return [];
  }
}
