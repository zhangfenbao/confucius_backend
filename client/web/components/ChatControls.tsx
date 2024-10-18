"use client";

import ExpiryCountdown from "@/components/ExpiryCountdown";
import emitter from "@/lib/eventEmitter";
import { cn } from "@/lib/utils";
import {
  ArrowUpIcon,
  Keyboard,
  LoaderCircle,
  Mic,
  PaperclipIcon,
  Speech,
  TriangleAlertIcon,
  X,
} from "lucide-react";
import Image from "next/image";
import { usePathname, useRouter, useSearchParams } from "next/navigation";
import {
  FormEvent,
  KeyboardEvent,
  useCallback,
  useEffect,
  useRef,
  useState,
} from "react";
import { RTVIEvent, RTVIMessage } from "realtime-ai";
import { DailyVoiceClient } from "realtime-ai-daily";
import {
  useRTVIClient,
  useRTVIClientEvent,
  useRTVIClientTransportState,
  VoiceVisualizer,
} from "realtime-ai-react";
import BotReadyAudio from "./BotReadyAudio";
import { Button } from "./ui/button";
import {
  Carousel,
  CarouselContent,
  CarouselItem,
  CarouselNext,
  CarouselPrevious,
} from "./ui/carousel";
import { Dialog, DialogClose, DialogContent } from "./ui/dialog";
import { Textarea } from "./ui/textarea";
import {
  Tooltip,
  TooltipContent,
  TooltipProvider,
  TooltipTrigger,
} from "./ui/tooltip";

interface Props {
  conversationId: string;
  onChangeMode?: (isVoiceMode: boolean) => void;
  workspaceId: string;
}

const ChatControls: React.FC<Props> = ({
  conversationId,
  onChangeMode,
  workspaceId,
}) => {
  const { push, replace } = useRouter();
  const pathname = usePathname();
  const searchParams = useSearchParams();

  const [isVoiceMode, setIsVoiceMode] = useState(
    Boolean(searchParams.get("v"))
  ); // Track whether we're in voice mode
  const [isMuted, setIsMuted] = useState(false); // Track mute/unmute state
  const [, setSelectedImages] = useState<File[]>([]); // Track selected image files
  const [previewUrls, setPreviewUrls] = useState<string[]>([]); // Track preview URLs
  const [imageZoom, setImageZoom] = useState(false);
  const [startIndex, setStartIndex] = useState(0);

  const [text, setText] = useState("");

  const rtviClient = useRTVIClient();
  const transportState = useRTVIClientTransportState();

  const [endDate, setEndDate] = useState<Date | null>(null);
  const [error, setError] = useState("");
  const [processingAction, setProcessingAction] = useState(false);

  const formRef = useRef<HTMLFormElement>(null);

  const handleTextKeyDown = (ev: KeyboardEvent<HTMLTextAreaElement>) => {
    if (!formRef.current) return;
    if (ev.key === "Enter" && !ev.shiftKey) {
      ev.preventDefault();
      formRef.current.requestSubmit();
    }
  };

  const newConversationIdRef = useRef<string>("");

  const sendTextMessage = async (client: DailyVoiceClient, message: string) => {
    emitter.emit("userTextMessage", message);
    setText("");

    try {
      await client?.action({
        service: "llm",
        action: "append_to_messages",
        arguments: [
          {
            name: "messages",
            value: [
              {
                role: "user",
                content: message,
              },
            ],
          },
        ],
      });
    } catch (e) {
      if (e instanceof RTVIMessage) {
        console.error(e.data);
      }
    } finally {
      setProcessingAction(false);
    }
  };

  const createConversation = useCallback(
    async (voice: boolean) => {
      if (!rtviClient) return;

      emitter.emit("showChatMessages");

      const response = await fetch(`/api/create-conversation`, {
        method: "POST",
        body: JSON.stringify({
          title: "New conversation",
          workspace_id: workspaceId,
        }),
      });
      if (response.ok) {
        const json = await response.json();
        const newConversationId = json.conversation_id;

        newConversationIdRef.current = newConversationId;

        rtviClient.params.requestData = {
          ...(rtviClient.params.requestData ?? {}),
          conversation_id: newConversationId,
        };

        emitter.emit("updateSidebar");
        if (voice) push(`/${workspaceId}/c/${newConversationId}?v=1`);

        return newConversationId;
      }
      return null;
    },
    [push, rtviClient, workspaceId]
  );

  const handleTextSubmit = async (ev: FormEvent<HTMLFormElement>) => {
    ev.preventDefault();

    if (processingAction || !rtviClient) return;

    setProcessingAction(true);

    const message = text.trim();

    if (conversationId === "new") {
      const newConversationId = await createConversation(false);
      let botStarted = false;
      rtviClient.addListener("botLlmStarted", () => {
        botStarted = true;
      });
      rtviClient.addListener("botLlmStopped", () => {
        // Wait at most 3 seconds before redirect
        setTimeout(() => push(`/${workspaceId}/c/${newConversationId}`), 3000);
      });
      rtviClient.addListener("storageItemStored", () => {
        if (botStarted) push(`/${workspaceId}/c/${newConversationId}`);
      });
    }
    sendTextMessage(rtviClient, message);
  };

  const handleConnect = useCallback(async () => {
    setIsVoiceMode(true);
    setIsMuted(false);
    rtviClient?.enableMic(true);
    onChangeMode?.(true);
    setEndDate(new Date(Number(rtviClient?.transportExpiry) * 1000));
  }, [onChangeMode, rtviClient]);

  const handleDisconnect = useCallback(() => {
    setIsVoiceMode(false);
    setIsMuted(false);
    rtviClient?.enableMic(false);
    onChangeMode?.(false);
    setEndDate(null);
  }, [onChangeMode, rtviClient]);

  useRTVIClientEvent(RTVIEvent.Connected, handleConnect);
  useRTVIClientEvent(RTVIEvent.Disconnected, handleDisconnect);

  const handleSwitchToTextMode = useCallback(() => {
    setIsVoiceMode(false);
    rtviClient?.disconnect();
  }, [rtviClient]);
  const handleSwitchToVoiceMode = useCallback(
    async (createIfNew = true) => {
      setIsVoiceMode(true);
      if (conversationId === "new" && createIfNew) {
        await createConversation(true);
        return;
      }
      try {
        await rtviClient?.connect();
      } catch (e) {
        console.error(e);
        handleSwitchToTextMode();
      }
    },
    [conversationId, createConversation, handleSwitchToTextMode, rtviClient]
  );

  useEffect(() => {
    if (searchParams.get("v")) {
      handleSwitchToVoiceMode(false);
      replace(pathname);
    }
  }, [handleSwitchToVoiceMode, pathname, replace, searchParams]);

  useRTVIClientEvent(
    RTVIEvent.Error,
    useCallback(
      (message: RTVIMessage) => {
        console.error(message);
        setError("An error occurred during the voice session.");
        handleSwitchToTextMode();
      },
      [handleSwitchToTextMode]
    )
  );

  // Toggle between mute and unmute in voice mode
  const handleMuteToggle = useCallback(() => {
    setIsMuted((muted) => {
      rtviClient?.enableMic(muted);
      return !muted;
    });
  }, [rtviClient]);

  // Handle image selection
  const handleImageChange = (event: React.ChangeEvent<HTMLInputElement>) => {
    const files = Array.from(event.target.files ?? []);
    if (files.length) {
      setSelectedImages((images) => [...images, ...files]);
      setPreviewUrls((urls) => [
        ...urls,
        ...files.map((f) => URL.createObjectURL(f)),
      ]);
    }
    event.target.value = "";
  };

  // Remove the selected image
  const handleRemoveImage = (idx: number) => {
    setSelectedImages((images) => {
      const newImages = [...images];
      newImages.splice(idx, 1);
      return newImages;
    });
    setPreviewUrls((urls) => {
      const newUrls = [...urls];
      newUrls.splice(idx, 1);
      return newUrls;
    });
  };

  useEffect(() => {
    if (previewUrls.length) return;
    setImageZoom(false);
    setStartIndex(0);
  }, [previewUrls.length]);

  const isReadyToSpeak = transportState === "ready";

  const feedbackClassName =
    "bg-gradient-to-t from-background absolute w-full bottom-full translate-y-2 pt-4 flex gap-2 items-center justify-center z-10";

  return (
    <div className="relative w-full">
      <BotReadyAudio active={isVoiceMode} />
      <Dialog open={imageZoom} onOpenChange={setImageZoom}>
        <DialogContent
          noCloseButton
          className="border-none bg-transparent shadow-none p-12 max-w-none w-[100dvw] max-h-[100dvh]"
        >
          <DialogClose className="top-4 right-2 absolute">
            <X className="text-white" />
          </DialogClose>
          <Carousel
            className={cn("relative h-[calc(100dvh-6rem)] w-100", {
              "mx-8": previewUrls.length > 1,
            })}
            opts={{
              loop: true,
              startIndex,
            }}
          >
            <CarouselContent className="items-center">
              {previewUrls.map((url, idx) => (
                <CarouselItem
                  key={idx}
                  className="relative h-[calc(100dvh-6rem)] overflow-hidden"
                >
                  <img
                    src={url}
                    alt="Selected Preview"
                    className="object-contain h-full w-full"
                  />
                </CarouselItem>
              ))}
            </CarouselContent>
            {previewUrls.length > 1 && (
              <>
                <CarouselPrevious />
                <CarouselNext />
              </>
            )}
          </Carousel>
        </DialogContent>
      </Dialog>

      {error ? (
        <div className={feedbackClassName}>
          <TriangleAlertIcon />
          <span>{error}</span>
        </div>
      ) : transportState === "authenticating" ||
        transportState === "connecting" ||
        transportState === "connected" ? (
        <div className={feedbackClassName}>
          <LoaderCircle className="animate-spin" />
          <span>Connecting…</span>
        </div>
      ) : transportState === "ready" ? (
        <div className={feedbackClassName}>
          <span>
            {isMuted
              ? "Tap to unmute"
              : processingAction
              ? "Thinking…"
              : "Listening"}
          </span>
        </div>
      ) : processingAction ? (
        <div className={feedbackClassName}>
          <LoaderCircle className="animate-spin" />
        </div>
      ) : null}

      {/* Image Preview (if an image is selected) */}
      {previewUrls.length > 0 && (
        <div className="relative mb-2 w-full flex justify-start gap-2 px-2">
          {previewUrls.map((url, idx) => (
            <div key={idx + url} className="relative inline-block">
              <Image
                src={url}
                alt="Selected Preview"
                className="cursor-zoom-in h-20 w-20 object-cover rounded-lg"
                onClick={() => {
                  setStartIndex(idx);
                  setImageZoom(true);
                }}
                height={80}
                width={80}
              />
              {/* Remove button */}
              <TooltipProvider>
                <Tooltip>
                  <TooltipTrigger asChild>
                    <button
                      onClick={() => handleRemoveImage(idx)}
                      className="absolute top-[-4px] right-[-4px] bg-destructive text-destructive-foreground p-1 rounded-full focus:outline-none"
                    >
                      <X className="w-4 h-4" />
                    </button>
                  </TooltipTrigger>
                  <TooltipContent className="bg-popover text-popover-foreground">
                    Remove image
                  </TooltipContent>
                </Tooltip>
              </TooltipProvider>
            </div>
          ))}
        </div>
      )}

      {/* Chat Controls */}
      <div className="flex items-end gap-3 p-2 w-full">
        {/* Image Button (File picker with camera support on mobile) */}
        <TooltipProvider>
          <Tooltip>
            <TooltipTrigger asChild>
              <Button
                className="rounded-full relative mb-2 p-2 h-10 w-10 border border-secondary"
                size="icon"
                variant="secondary"
                onClick={() => {
                  emitter.emit("disabledInSandboxMode");
                }}
              >
                <PaperclipIcon />
                {/* File input (visually hidden) */}
                <input
                  type="file"
                  accept="image/*"
                  multiple
                  className="absolute hidden inset-0 opacity-0 file:cursor-pointer file:inset-0 file:absolute"
                  onChange={handleImageChange}
                />
              </Button>
            </TooltipTrigger>
            <TooltipContent className="bg-secondary text-secondary-foreground">
              Attach images
            </TooltipContent>
          </Tooltip>
        </TooltipProvider>

        {/* Chat input area */}
        <div className="flex-grow flex items-end gap-3 p-2 relative">
          {/* Text input or Voice indicator based on mode */}
          {isVoiceMode ? (
            // Voice mode with mute/unmute toggle and voice indicator
            <div className="border border-border rounded-full w-full flex items-center gap-3 p-1 pe-4">
              {/* Mic button for mute/unmute */}
              <TooltipProvider>
                <Tooltip>
                  <TooltipTrigger asChild>
                    <button
                      type="button"
                      onClick={handleMuteToggle}
                      className={cn(
                        "p-1 rounded-full focus:outline-none hover:bg-secondary",
                        {
                          "bg-destructive hover:bg-destructive text-destructive-foreground w-28":
                            isMuted,
                        }
                      )}
                    >
                      <Mic className="w-6 h-6 m-auto" />
                    </button>
                  </TooltipTrigger>
                  <TooltipContent className="bg-secondary text-secondary-foreground">
                    {isMuted ? "Unmute microphone" : "Mute microphone"}
                  </TooltipContent>
                </Tooltip>
              </TooltipProvider>

              {/* Voice Indicator when microphone is active and not muted */}
              {isReadyToSpeak && !isMuted ? (
                <VoiceVisualizer
                  backgroundColor="transparent"
                  barColor={isMuted ? "gray" : "black"}
                  barGap={4}
                  barWidth={8}
                  barMaxHeight={20}
                  participantType="local"
                />
              ) : isMuted ? null : (
                <div className="w-5 h-5 animate-spin">
                  <LoaderCircle size={20} />
                </div>
              )}

              {endDate && (
                <div className="ml-auto justify-self-end">
                  <span className="select-none tabular-nums font-mono">
                    <ExpiryCountdown endDate={endDate} />
                  </span>
                </div>
              )}
            </div>
          ) : (
            // Text input mode
            <form
              ref={formRef}
              className="relative w-full border border-input rounded-3xl ps-4 pe-10 focus-within:ring-1 focus-within:ring-accent"
              id="text-chat-form"
              onSubmit={handleTextSubmit}
            >
              <Textarea
                autoFocus
                className="!border-0 !border-none !shadow-none !outline-none focus-visible:ring-0 text-base min-h-0 h-auto max-h-32 p-0 py-2 resize-none"
                onChange={(ev) => setText(ev.currentTarget.value)}
                onKeyDown={handleTextKeyDown}
                required
                placeholder="Type message here"
                value={text}
                rows={text.split("\n").length}
              />
              <Button
                className="absolute h-8 w-8 p-0 right-1 top-1/2 -translate-y-1/2 rounded-full"
                size="icon"
                variant="secondary"
                type="submit"
              >
                <ArrowUpIcon size={16} />
              </Button>
            </form>
          )}
          <TooltipProvider>
            <Tooltip>
              <TooltipTrigger asChild>
                <button
                  onClick={
                    isVoiceMode
                      ? handleSwitchToTextMode
                      : () => handleSwitchToVoiceMode()
                  }
                  type="button"
                  className="bg-secondary relative flex gap-4 p-2 rounded-full border border-secondary focus:outline-none"
                >
                  <span
                    className={cn(
                      "rounded-full bg-background w-12 h-8 absolute left-1 top-1 transition-transform",
                      {
                        "translate-x-14": isVoiceMode,
                      }
                    )}
                  />
                  <Keyboard
                    className={cn(
                      "w-10 h-6 z-10 text-foreground transition-colors",
                      {
                        "text-muted-foreground": isVoiceMode,
                      }
                    )}
                  />
                  <Speech
                    className={cn(
                      "w-10 h-6 z-10 text-foreground transition-colors",
                      {
                        "text-muted-foreground": !isVoiceMode,
                      }
                    )}
                  />
                </button>
              </TooltipTrigger>
              <TooltipContent
                align="center"
                className="bg-secondary text-secondary-foreground"
              >
                Toggle conversation mode
              </TooltipContent>
            </Tooltip>
          </TooltipProvider>
        </div>
      </div>
    </div>
  );
};

export default ChatControls;
