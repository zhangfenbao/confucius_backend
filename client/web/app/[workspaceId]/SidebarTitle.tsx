"use client";

import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuTrigger,
} from "@/components/ui/dropdown-menu";
import emitter from "@/lib/eventEmitter";
import { WorkspaceModel } from "@/lib/sesameApi";
import { cn } from "@/lib/utils";
import { CheckIcon, ChevronsUpDownIcon } from "lucide-react";
import { useRouter } from "next/navigation";

interface SidebarTitleProps {
  className: string;
  onSwitchWorkspace?: () => void;
  workspace?: WorkspaceModel | null;
  workspaces?: WorkspaceModel[];
}

const SidebarTitle = ({
  className,
  onSwitchWorkspace,
  workspace,
  workspaces = [],
}: SidebarTitleProps) => {
  const { push } = useRouter();
  return (
    <div
      className={cn(
        "flex gap-2 items-center -m-4 mb-0 p-4 sticky top-0 bg-secondary z-10 shadow-short",
        className
      )}
    >
      <h1 className="font-semibold text-nowrap text-ellipsis flex-grow overflow-hidden">
        {workspace?.title ?? "No workspace"}
      </h1>
      <DropdownMenu>
        <DropdownMenuTrigger className="p-2 hover:bg-secondary-foreground/[.07] rounded-md">
          <ChevronsUpDownIcon size={16} className="opacity-60" />
        </DropdownMenuTrigger>
        <DropdownMenuContent align="end">
          {workspaces.map((ws) => (
            <DropdownMenuItem
              key={ws.workspace_id}
              onClick={() => {
                emitter.emit("showPageTransitionLoader");
                onSwitchWorkspace?.();
                // Timeout to close sidebar and reset scroll lock styles
                setTimeout(() => push(`/${ws.workspace_id}`), 200);
              }}
            >
              <div className="flex items-center justify-between gap-4 w-full">
                {ws.title}
                {ws.workspace_id === workspace?.workspace_id ? (
                  <CheckIcon size={16} />
                ) : (
                  <span className="w-4 h-4" />
                )}
              </div>
            </DropdownMenuItem>
          ))}
        </DropdownMenuContent>
      </DropdownMenu>
    </div>
  );
};

export default SidebarTitle;
