"use client";

import { useRouter } from "next/navigation";
import { useEffect } from "react";
import { useRTVIClient } from "realtime-ai-react";

interface Props {
  onRefresh?: () => void;
}

/**
 * Refreshes page data, when tab is enabled again.
 */
export default function PageRefresher({ onRefresh }: Props) {
  const { refresh } = useRouter();

  const rtviClient = useRTVIClient();

  useEffect(() => {
    const handleVisibilityChange = () => {
      if (document.visibilityState === "visible") {
        const shouldRefresh = !rtviClient || !rtviClient.connected;
        if (shouldRefresh) {
          refresh();
          onRefresh?.();
        }
      }
    };
    document.addEventListener("visibilitychange", handleVisibilityChange);
    return () => {
      document.removeEventListener("visibilitychange", handleVisibilityChange);
    };
  }, [onRefresh, refresh, rtviClient]);

  return null;
}
