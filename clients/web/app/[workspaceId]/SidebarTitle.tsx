"use client";

import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuGroup,
  DropdownMenuItem,
  DropdownMenuLabel,
  DropdownMenuSeparator,
  DropdownMenuTrigger,
} from "@/components/ui/dropdown-menu";
import emitter from "@/lib/eventEmitter";
import { WorkspaceModel } from "@/lib/sesameApi";
import { cn } from "@/lib/utils";
import { CheckIcon, ChevronsUpDownIcon, LayoutGridIcon } from "lucide-react";
import Link from "next/link";
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
        <DropdownMenuContent align="end" className="max-w-64">
          <DropdownMenuGroup>
            <DropdownMenuLabel>Switch to</DropdownMenuLabel>
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
                  <div className="text-nowrap text-ellipsis overflow-hidden">{ws.title}</div>
                  {ws.workspace_id === workspace?.workspace_id ? (
                    <CheckIcon className="flex-none" size={16} />
                  ) : (
                    <span className="flex-none w-4 h-4" />
                  )}
                </div>
              </DropdownMenuItem>
            ))}
          </DropdownMenuGroup>
          <DropdownMenuSeparator />
          <DropdownMenuItem asChild>
            <Link
              href="/workspaces"
              className="flex items-center justify-between gap-4 w-full"
            >
              Manage workspaces…
              <LayoutGridIcon size={16} />
            </Link>
          </DropdownMenuItem>
        </DropdownMenuContent>
      </DropdownMenu>
    </div>
  );
};

export default SidebarTitle;
