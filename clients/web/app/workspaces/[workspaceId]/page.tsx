import PageRefresher from "@/components/PageRefresher";
import { defaultWorkspace, getWorkspace } from "@/lib/workspaces";
import ConfigurationForm from "./ConfigurationForm";

interface WorkspacePageProps {
  params: { workspaceId: string };
}

export default async function EditWorkspacePage({
  params,
}: WorkspacePageProps) {
  const { workspaceId } = params;

  if (workspaceId === "new") {
    return <ConfigurationForm workspace={defaultWorkspace} />;
  }

  // Fetch workspace config
  const workspace = await getWorkspace(workspaceId);

  if (!workspace) {
    return <p>404: Workspace not found</p>;
  }

  return (
    <>
      <ConfigurationForm workspace={workspace} />
      <PageRefresher />
    </>
  );
}
