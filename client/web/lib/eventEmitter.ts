"use client";

import { EventEmitter } from "events";

interface EventMap {
  deleteConversation: [conversationId: string];
  disabledInSandboxMode: [];
  showChatMessages: [];
  showPageTransitionLoader: [];
  toggleSettings: [];
  toggleSidebar: [];
  updateSidebar: [];
  userTextMessage: [text: string];
}

const emitter = new EventEmitter<EventMap>();

// Optional: Set the maximum number of listeners to avoid potential memory leaks in large apps.
emitter.setMaxListeners(10);

export default emitter;
