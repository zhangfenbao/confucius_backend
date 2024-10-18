"use server";

import { revalidateTag } from "next/cache";

export async function revalidateConversations() {
  revalidateTag("conversations");
}

export async function revalidateConversation(conversationId: string) {
  revalidateTag(conversationId);
  revalidateConversations();
}

export async function revalidateWorkspaces() {
  revalidateTag("workspaces");
}

export async function revalidateWorkspace(workspaceId: string) {
  revalidateTag(workspaceId);
  revalidateWorkspaces();
}
