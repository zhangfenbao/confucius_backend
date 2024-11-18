import { redirect } from "next/navigation";

interface WorkspaceConversationPageProps {
  params: Promise<{ workspaceId: string }>;
}

export default async function WorkspaceConversationPage({
  params,
}: WorkspaceConversationPageProps) {
  const { workspaceId } = await params;

  // Redirect to the main workspace page, which will in turn redirect to the most recent conversation
  redirect(`/${workspaceId}`);
}
