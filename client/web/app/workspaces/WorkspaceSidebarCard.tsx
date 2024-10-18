import { Card, CardContent } from "@/components/ui/card";
import { WorkspaceModel } from "@/lib/sesameApi";
import { getWorkspaceStructuredData } from "@/lib/workspaces";
import { ArrowRightIcon } from "lucide-react";

interface Props {
  workspace: WorkspaceModel;
}

export default function WorkspaceSidebarCard({ workspace }: Props) {
  const structuredData = getWorkspaceStructuredData(workspace.config);

  return (
    <Card className="rounded-lg w-full">
      <CardContent className="p-2 hover:bg-secondary transition-colors overflow-hidden rounded-lg">
        <div className="flex gap-2 items-center justify-between">
          <div className="flex-shrink overflow-hidden">
            <span className="block text-nowrap text-ellipsis overflow-hidden underline">{workspace.title}</span>
            <div className="text-xs font-mono">
              <div className="flex gap-4">
                <span className="capitalize">
                  {structuredData.llm.service} ({structuredData.llm.model.label})
                </span>
              </div>
            </div>
          </div>
          <ArrowRightIcon className="flex-none" />
        </div>
      </CardContent>
    </Card>
  );
}
