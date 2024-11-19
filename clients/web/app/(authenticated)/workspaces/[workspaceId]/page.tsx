import PageRefresher from "@/components/PageRefresher";
import { getAvailableServices, getServices } from "@/lib/services";
import { defaultWorkspace, getWorkspace } from "@/lib/workspaces";
import ConfigurationForm from "./ConfigurationForm";

interface WorkspacePageProps {
  params: Promise<{ workspaceId: string }>;
}

export default async function EditWorkspacePage({
  params,
}: WorkspacePageProps) {
  const { workspaceId } = await params;

  const services = await getServices();
  const availableServices = await getAvailableServices();

  if (workspaceId === "new") {
    return (
      <ConfigurationForm
        availableServices={availableServices}
        services={services}
        workspace={defaultWorkspace}
      />
    );
  }

  // Fetch workspace config
  const workspace = await getWorkspace(workspaceId);

  if (!workspace) {
    return <p>404: Workspace not found</p>;
  }

  return (
    <>
      <ConfigurationForm
        availableServices={availableServices}
        services={services}
        workspace={workspace}
      />
      <PageRefresher />
    </>
  );
}
