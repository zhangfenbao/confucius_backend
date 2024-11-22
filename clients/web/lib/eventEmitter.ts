"use client";

import { ImageContent, TextContent } from "@/lib/messages";
import { WorkspaceModel } from "@/lib/sesameApi";
import { EventEmitter } from "events";

interface EventMap {
  changeLlmModel: [llmModel: string];
  deleteConversation: [conversationId: string];
  deleteWorkspace: [workspace: WorkspaceModel];
  disabledInSandboxMode: [];
  showChatMessages: [];
  showPageTransitionLoader: [];
  toggleSettings: [];
  toggleSidebar: [];
  updateSidebar: [];
  userTextMessage: [text: Array<TextContent | ImageContent>];
}

const emitter = new EventEmitter<EventMap>();

// Optional: Set the maximum number of listeners to avoid potential memory leaks in large apps.
emitter.setMaxListeners(10);

export default emitter;
