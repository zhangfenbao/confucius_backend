"use server";

import ClientPage from "@/components/ClientPage";
import { getConversation } from "@/lib/conversations";
import { getWorkspace, getWorkspaceStructuredData } from "@/lib/workspaces";
import { redirect } from "next/navigation";

interface ConversationPageProps {
  params: Promise<{
    conversationId: string;
    workspaceId: string;
  }>;
}

export default async function ConversationPage({
  params,
}: ConversationPageProps) {
  const { conversationId, workspaceId } = await params;

  const workspace = await getWorkspace(workspaceId);

  if (!workspace) redirect("/");

  const conversation = await getConversation(conversationId);

  if (!conversation) redirect(`/${workspaceId}`);

  return (
    <ClientPage
      conversationId={conversationId}
      messages={conversation.messages}
      structuredWorkspace={getWorkspaceStructuredData(workspace.config)}
      workspaceId={workspaceId}
    />
  );
}
