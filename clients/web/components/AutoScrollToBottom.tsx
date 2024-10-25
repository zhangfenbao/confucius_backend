"use client";

import { useEffect } from "react";

interface Props {
  behavior?: ScrollBehavior;
}

export default function AutoScrollToBottom({ behavior = "auto" }: Props) {
  useEffect(() => {
    document.scrollingElement?.scrollTo({
      behavior,
      top: document.documentElement.scrollHeight,
    });
  }, [behavior]);

  return null;
}
