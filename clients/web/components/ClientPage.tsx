"use client";

import AutoScrollToBottom from "@/components/AutoScrollToBottom";
import ChatControls from "@/components/ChatControls";
import PageRefresher from "@/components/PageRefresher";
import QueryClientProvider from "@/components/QueryClientProvider";
import Logo from "@/components/svg/Logo";
import { Button } from "@/components/ui/button";
import {
  Tooltip,
  TooltipContent,
  TooltipProvider,
  TooltipTrigger,
} from "@/components/ui/tooltip";
import emitter from "@/lib/eventEmitter";
import type { Message } from "@/lib/messages";
import { cn } from "@/lib/utils";
import { WorkspaceStructuredData } from "@/lib/workspaces";
import { ArrowDownIcon } from "lucide-react";
import { useSearchParams } from "next/navigation";
import { useEffect, useLayoutEffect, useState } from "react";
import { DailyTransport, DailyVoiceClient } from "realtime-ai-daily";
import { RTVIClientAudio, RTVIClientProvider } from "realtime-ai-react";
import ChatMessages from "./ChatMessages";
import Settings from "./Settings";

interface Props {
  conversationId: string;
  messages: Message[];
  structuredWorkspace: WorkspaceStructuredData;
  workspaceId: string;
}

export default function ClientPage({
  conversationId,
  messages,
  structuredWorkspace,
  workspaceId,
}: Props) {
  const searchParams = useSearchParams();

  const [client, setClient] = useState<DailyVoiceClient>();

  useEffect(() => {
    setClient(
      new DailyVoiceClient({
        enableCam: false,
        enableMic: false,
        transport: new DailyTransport(),
        params: {
          baseUrl: "/api",
          endpoints: {
            connect: "/start-bot",
            action: "/bot-action",
          },
          requestData: {
            botProfile: "voice_2024_08",
            maxDuration: 600,
            conversation_id: conversationId,
            workspace_id: workspaceId,
          },
        },
      })
    );
  }, [conversationId, workspaceId]);

  const [showScrollToBottom, setShowScrollToBottom] = useState(false);
  const [showMessages, setShowMessages] = useState(
    messages.length > 0 || Boolean(searchParams.get("v"))
  );

  useLayoutEffect(() => {
    const handleScroll = () => {
      const scroller = document.scrollingElement;
      if (!scroller) return;
      const scrollBottom =
        scroller.scrollHeight - scroller.clientHeight - scroller.scrollTop;
      setShowScrollToBottom(
        scroller.scrollHeight > scroller.clientHeight && scrollBottom > 150
      );
    };
    window.addEventListener("scroll", handleScroll);
    return () => {
      window.removeEventListener("scroll", handleScroll);
    };
  }, []);

  useEffect(() => {
    const handleShowMessages = () => {
      setShowMessages(true);
    };
    emitter.on("showChatMessages", handleShowMessages);
    return () => {
      emitter.off("showChatMessages", handleShowMessages);
    };
  }, []);

  const animate = !searchParams.has("a") || searchParams.get("a") === "1";

  return (
    <RTVIClientProvider client={client!}>
      <QueryClientProvider>
        <PageRefresher />
        <div
          className={cn("flex flex-col justify-between flex-grow", {
            "animate-appear": animate,
          })}
        >
          <AutoScrollToBottom />

          {/* Messages */}
          <div className="relative flex-grow p-4 flex flex-col">
            {messages.length > 0 || showMessages ? (
              <ChatMessages
                autoscroll={!showScrollToBottom}
                conversationId={conversationId}
                messages={messages}
                structuredWorkspace={structuredWorkspace}
                workspaceId={workspaceId}
              />
            ) : (
              <div className="flex flex-col gap-4 items-center justify-center h-full my-auto">
                <div className="relative z-10 flex items-center justify-center size-16 bg-primary rounded-full">
                  <Logo className="text-white size-8 rotate-12" />
                </div>
                <h2 className="font-semibold text-xl text-center">
                  Start chatting
                </h2>
              </div>
            )}
            {showScrollToBottom && (
              <TooltipProvider>
                <Tooltip>
                  <TooltipTrigger asChild>
                    <Button
                      className="rounded-full fixed right-4 bottom-20 z-20"
                      onClick={() =>
                        document.scrollingElement?.scrollTo({
                          behavior: "smooth",
                          top: document.scrollingElement?.scrollHeight,
                        })
                      }
                      size="icon"
                      variant="outline"
                    >
                      <ArrowDownIcon size={16} />
                    </Button>
                  </TooltipTrigger>
                  <TooltipContent
                    align="center"
                    className="bg-popover text-popover-foreground"
                    side="left"
                  >
                    Scroll to bottom
                  </TooltipContent>
                </Tooltip>
              </TooltipProvider>
            )}
          </div>

          {/* Chat controls */}
          <div className="bg-background sticky bottom-0 z-10">
            <ChatControls
              conversationId={conversationId}
              workspaceId={workspaceId}
            />
          </div>
        </div>

        <RTVIClientAudio />
        <Settings conversationId={conversationId} workspaceId={workspaceId} />
      </QueryClientProvider>
    </RTVIClientProvider>
  );
}
