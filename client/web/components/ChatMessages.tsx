"use client";

import ChatMessage from "@/components/ChatMessage";
import { Message } from "@/lib/messages";
import { WorkspaceStructuredData } from "@/lib/workspaces";
import { useCallback, useState } from "react";
import { RTVIEvent } from "realtime-ai";
import { useRTVIClientEvent } from "realtime-ai-react";
import LiveMessages from "./LiveMessages";

interface Props {
  autoscroll?: boolean;
  conversationId: string;
  messages: Message[];
  structuredWorkspace: WorkspaceStructuredData;
  workspaceId: string;
}

export default function ChatMessages({
  autoscroll = true,
  conversationId,
  messages,
  structuredWorkspace,
  workspaceId,
}: Props) {
  const [isBotSpeaking, setIsBotSpeaking] = useState(false);

  useRTVIClientEvent(
    RTVIEvent.BotStartedSpeaking,
    useCallback(() => {
      setIsBotSpeaking(true);
    }, [])
  );
  useRTVIClientEvent(
    RTVIEvent.BotStoppedSpeaking,
    useCallback(() => {
      setIsBotSpeaking(false);
    }, [])
  );
  useRTVIClientEvent(
    RTVIEvent.Disconnected,
    useCallback(() => {
      setIsBotSpeaking(false);
    }, [])
  );

  return (
    <div className="flex flex-col gap-4">
      {messages
        .filter((m) => m.content.role !== "system")
        .map((message, index) => (
          <ChatMessage
            key={index}
            isSpeaking={
              message.content.role === "assistant" &&
              index === messages.length - 1 &&
              isBotSpeaking
            }
            message={message}
          />
        ))}
      <LiveMessages
        autoscroll={autoscroll}
        conversationId={conversationId}
        isBotSpeaking={isBotSpeaking}
        messages={messages}
        structuredWorkspace={structuredWorkspace}
        workspaceId={workspaceId}
      />
    </div>
  );
}
