"use client";

import {
  useCallback,
  useEffect,
  useLayoutEffect,
  useRef,
  useState,
} from "react";

import { revalidateAll } from "@/app/actions";
import ChatMessage from "@/components/ChatMessage";
import { queryClient } from "@/components/QueryClientProvider";
import emitter from "@/lib/eventEmitter";
import { LLMMessageRole } from "@/lib/llm";
import type { Message } from "@/lib/messages";
import { WorkspaceStructuredData } from "@/lib/workspaces";
import { useRouter } from "next/navigation";
import {
  BotLLMTextData,
  RTVIEvent,
  TTSTextData,
  TranscriptData,
} from "realtime-ai";
import { useRTVIClient, useRTVIClientEvent } from "realtime-ai-react";
import { v4 as uuidv4 } from "uuid";

interface LiveMessage extends Message {
  final?: boolean;
}

interface Props {
  autoscroll: boolean;
  conversationId: string;
  isBotSpeaking?: boolean;
  messages: Message[];
  structuredWorkspace: WorkspaceStructuredData;
  workspaceId: string;
}

const addNewLinesBeforeCodeblocks = (markdown: string) =>
  markdown.match(/([^\n])(\n```)/)
    ? markdown.replace(/([^\n])(\n```)/g, "$1\n$2")
    : markdown;

interface MessageChunk {
  createdAt?: Date;
  final: boolean;
  replace?: boolean;
  role: LLMMessageRole;
  text: string;
  updatedAt?: Date;
}

const revalidateConversation = async (workspaceId: string) => {
  await revalidateAll();
  queryClient.invalidateQueries({
    queryKey: ["conversations", workspaceId],
    type: "all",
  });
};

export default function LiveMessages({
  autoscroll,
  conversationId,
  isBotSpeaking,
  messages,
  structuredWorkspace,
  workspaceId,
}: Props) {
  const { refresh } = useRouter();
  const [liveMessages, setLiveMessages] = useState<LiveMessage[]>([]);

  const client = useRTVIClient();

  useEffect(() => {
    if (!client) return;
    client.params = {
      ...client.params,
      requestData: {
        conversation_id: conversationId,
      },
    };
  }, [client, conversationId]);

  const addMessageChunk = useCallback(
    ({
      createdAt = new Date(),
      final,
      replace = false,
      role,
      text,
      updatedAt = createdAt,
    }: MessageChunk) => {
      const createdAtIso = createdAt.toISOString();
      const updatedAtIso = updatedAt.toISOString();

      setLiveMessages((liveMessages) => {
        const matchingMessageIdx = liveMessages.findIndex(
          (m) => m.content.role === role && m.created_at === createdAtIso
        );
        const matchingMessage = liveMessages[matchingMessageIdx];

        const isSameMessage =
          matchingMessage?.final === final &&
          matchingMessage?.content?.content ===
            addNewLinesBeforeCodeblocks(text);

        if (isSameMessage) return liveMessages;

        if (!matchingMessage || matchingMessage?.final) {
          // Append new message
          const message: LiveMessage = {
            content: {
              content: addNewLinesBeforeCodeblocks(text),
              role,
            },
            conversation_id: conversationId,
            created_at: createdAtIso,
            extra_metadata: {},
            final,
            message_id: uuidv4(),
            message_number: messages.length + liveMessages.length + 1,
            updated_at: updatedAtIso,
          };
          return [...liveMessages, message];
        }

        const updatedMessages = [...liveMessages];
        const prevText = updatedMessages[matchingMessageIdx].content.content;
        const updatedMessage: LiveMessage = {
          ...updatedMessages[matchingMessageIdx],
          content: {
            content: addNewLinesBeforeCodeblocks(
              replace ? text : prevText + text
            ),
            role,
          },
          final,
          updated_at: updatedAtIso,
        };

        return liveMessages
          .map((liveMessage, idx) =>
            idx === matchingMessageIdx ? updatedMessage : liveMessage
          )
          .filter((m, idx, arr) => {
            const isEmptyMessage =
              m.content.content.trim() === "" && idx < arr.length - 1;
            return !isEmptyMessage;
          });
      });
    },
    [conversationId, messages.length]
  );

  const firstBotResponseTime = useRef<Date>();
  const userStartedSpeakingTime = useRef<Date>();
  const userStoppedSpeakingTimeout = useRef<ReturnType<typeof setTimeout>>();

  const cleanupUserMessages = useCallback(() => {
    setLiveMessages((messages) => {
      return messages.filter((m) => {
        if (m.content.role !== "user") return true;
        return m.content.content.length > 0;
      });
    });
  }, []);

  const isTextResponse = useRef(false);

  useRTVIClientEvent(
    RTVIEvent.BotLlmStarted,
    useCallback(() => {
      firstBotResponseTime.current = new Date();
      addMessageChunk({
        createdAt: firstBotResponseTime.current,
        final: false,
        role: "assistant",
        text: "",
      });
    }, [addMessageChunk])
  );

  useRTVIClientEvent(
    RTVIEvent.BotText,
    useCallback(
      (text: BotLLMTextData) => {
        if (
          structuredWorkspace.tts.interactionMode !== "informational" &&
          !isTextResponse.current
        )
          return;
        if (firstBotResponseTime.current) {
          addMessageChunk({
            createdAt: firstBotResponseTime.current,
            final: false,
            role: "assistant",
            text: text.text,
            updatedAt: new Date(),
          });
        }
      },
      [addMessageChunk, structuredWorkspace.tts.interactionMode]
    )
  );

  useRTVIClientEvent(
    RTVIEvent.BotLlmStopped,
    useCallback(async () => {
      const textResponse = isTextResponse.current;
      isTextResponse.current = false;
      if (
        structuredWorkspace.tts.interactionMode !== "informational" ||
        !textResponse
      )
        return;
      if (firstBotResponseTime.current) {
        addMessageChunk({
          createdAt: firstBotResponseTime.current,
          final: true,
          role: "assistant",
          text: "",
          updatedAt: new Date(),
        });
        firstBotResponseTime.current = undefined;
        revalidateConversation(workspaceId);
      }
    }, [addMessageChunk, structuredWorkspace.tts.interactionMode, workspaceId])
  );

  useRTVIClientEvent(
    RTVIEvent.BotTtsStarted,
    useCallback(() => {
      if (structuredWorkspace.tts.interactionMode !== "conversational") return;
      if (!firstBotResponseTime.current) {
        firstBotResponseTime.current = new Date();
      }
      addMessageChunk({
        createdAt: firstBotResponseTime.current,
        final: false,
        role: "assistant",
        text: "",
        updatedAt: new Date(),
      });
    }, [addMessageChunk, structuredWorkspace.tts.interactionMode])
  );

  useRTVIClientEvent(
    RTVIEvent.BotTtsText,
    useCallback(
      (text: TTSTextData) => {
        if (structuredWorkspace.tts.interactionMode !== "conversational")
          return;
        if (firstBotResponseTime.current) {
          addMessageChunk({
            createdAt: firstBotResponseTime.current,
            final: false,
            role: "assistant",
            text: " " + text.text,
            updatedAt: new Date(),
          });
        }
      },
      [addMessageChunk, structuredWorkspace.tts.interactionMode]
    )
  );

  useRTVIClientEvent(
    RTVIEvent.BotTtsStopped,
    useCallback(() => {
      if (structuredWorkspace.tts.interactionMode !== "conversational") return;
      const createdAt = firstBotResponseTime.current;
      firstBotResponseTime.current = undefined;
      setTimeout(() => {
        addMessageChunk({
          createdAt,
          final: true,
          role: "assistant",
          text: "",
          updatedAt: new Date(),
        });
      }, 1000);
    }, [addMessageChunk, structuredWorkspace.tts.interactionMode])
  );

  useRTVIClientEvent(
    RTVIEvent.UserStartedSpeaking,
    useCallback(() => {
      clearTimeout(userStoppedSpeakingTimeout.current);
      const now = userStartedSpeakingTime.current ?? new Date();
      userStartedSpeakingTime.current = now;
      addMessageChunk({
        createdAt: now,
        final: false,
        role: "user",
        text: "",
      });
    }, [addMessageChunk])
  );

  useRTVIClientEvent(
    RTVIEvent.UserStoppedSpeaking,
    useCallback(() => {
      userStoppedSpeakingTimeout.current = setTimeout(
        cleanupUserMessages,
        5000
      );
    }, [cleanupUserMessages])
  );

  useRTVIClientEvent(
    RTVIEvent.UserTranscript,
    useCallback(
      (data: TranscriptData) => {
        if (!userStartedSpeakingTime.current) {
          userStartedSpeakingTime.current = new Date();
        }
        addMessageChunk({
          createdAt: userStartedSpeakingTime.current,
          final: data.final,
          replace: true,
          role: "user" as LLMMessageRole,
          text: data.text,
          updatedAt: new Date(),
        });
        if (data.final) {
          userStartedSpeakingTime.current = undefined;
        }
      },
      [addMessageChunk]
    )
  );

  useEffect(() => {
    const handleUserTextMessage = (text: string) => {
      isTextResponse.current = true;
      addMessageChunk({
        createdAt: new Date(),
        final: true,
        role: "user",
        text,
      });
    };
    emitter.on("userTextMessage", handleUserTextMessage);
    return () => {
      emitter.off("userTextMessage", handleUserTextMessage);
    };
  }, [addMessageChunk]);

  useLayoutEffect(() => {
    if (!autoscroll) return;
    const scroller = document.scrollingElement;
    if (!scroller) return;
    scroller.scrollTo({
      behavior: "smooth",
      top: document.documentElement.scrollHeight,
    });
  }, [autoscroll, liveMessages]);

  useEffect(() => {
    // Server-stored messages updated. Empty client state.
    setLiveMessages([]);
  }, [messages.length]);

  useRTVIClientEvent(
    RTVIEvent.Disconnected,
    useCallback(async () => {
      await revalidateConversation(workspaceId);
      refresh();
    }, [refresh, workspaceId])
  );

  return liveMessages.map((m, i) => (
    <ChatMessage
      key={i}
      message={m}
      isSpeaking={
        i === liveMessages.length - 1 &&
        m.content.role === "assistant" &&
        isBotSpeaking &&
        !m.final
      }
    />
  ));
}
