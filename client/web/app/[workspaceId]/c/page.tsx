import { redirect } from "next/navigation";

interface WorkspaceConversationPageProps {
  params: { workspaceId: string };
}

export default async function WorkspaceConversationPage({
  params,
}: WorkspaceConversationPageProps) {
  const { workspaceId } = params;

  // Redirect to the main workspace page, which will in turn redirect to the most recent conversation
  redirect(`/${workspaceId}`);
}
