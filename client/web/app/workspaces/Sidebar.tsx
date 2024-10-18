"use client";

import PageTransitionLink from "@/components/PageTransitionLink";
import { Separator } from "@/components/ui/separator";
import {
  Sheet,
  SheetContent,
  SheetDescription,
  SheetTitle,
} from "@/components/ui/sheet";
import emitter from "@/lib/eventEmitter";
import { WorkspaceModel } from "@/lib/sesameApi";
import { cn } from "@/lib/utils";
import { SquarePenIcon, SquarePlusIcon } from "lucide-react";
import { usePathname } from "next/navigation";
import { useEffect, useState } from "react";
import WorkspaceSidebarCard from "./WorkspaceSidebarCard";

interface SidebarProps {
  workspaces: WorkspaceModel[];
}

export default function Sidebar({ workspaces }: SidebarProps) {
  const [isOpen, setIsOpen] = useState(false);

  const pathname = usePathname();

  useEffect(() => {
    const toggleSidebar = () => setIsOpen((prev) => !prev);
    const handleResize = () => {
      if (window.innerWidth >= 1024) setIsOpen(false);
    };
    emitter.on("toggleSidebar", toggleSidebar);
    window.addEventListener("resize", handleResize);
    return () => {
      emitter.off("toggleSidebar", toggleSidebar);
      window.removeEventListener("resize", handleResize);
    };
  }, []);

  const hasWorkspaces = workspaces.length > 0;

  const getContent = (hasTitle: boolean) => (
    <div className="flex flex-col gap-6 p-4 sticky top-0 overflow-y-auto h-dvh">
      {hasTitle && (
        <h2 className="text-xl font-semibold -m-4 mb-0 p-4 sticky -top-4 bg-secondary shadow-md z-10">
          Workspaces
        </h2>
      )}

      <div>
        <PageTransitionLink
          href="/workspaces/new"
          className={cn(
            "flex gap-2 items-center px-3 py-2 rounded-lg transition-colors hover:bg-input",
            {
              "bg-input": pathname === `/workspaces/new`,
            }
          )}
          onClick={() => setIsOpen(false)}
        >
          <SquarePlusIcon size={16} />
          Create new workspaces
        </PageTransitionLink>
      </div>

      <Separator />

      <span className="text-xl font-semibold">Your Workspaces</span>

      {hasWorkspaces ? (
        workspaces.map((workspace) => (
          <div
            key={workspace.workspace_id}
            className={cn(
              "flex gap-0 items-center group max-w-full overflow-hidden",
              {
                "bg-input rounded-lg outline outline-4 outline-input":
                  pathname.includes(workspace.workspace_id),
              }
            )}
          >
            <PageTransitionLink
              href={`/${workspace.workspace_id}`}
              className={cn(
                "transition-all w-full group-focus-within:w-[calc(100%-40px)] group-hover:w-[calc(100%-40px)]",
                {
                  "w-[calc(100%-40px)]": pathname.includes(
                    workspace.workspace_id
                  ),
                }
              )}
            >
              <WorkspaceSidebarCard workspace={workspace} />
            </PageTransitionLink>
            <PageTransitionLink
              href={`/workspaces/${workspace.workspace_id}`}
              className={cn(
                "overflow-hidden transition-all w-0 group-focus-within:w-10 group-hover:w-10 group-focus-within:px-2 group-hover:px-2 focus-visible:opacity-50 hover:opacity-50",
                {
                  "w-10 px-2": pathname.includes(workspace.workspace_id),
                }
              )}
              onClick={() => setIsOpen(false)}
            >
              <SquarePenIcon size={24} />
            </PageTransitionLink>
          </div>
        ))
      ) : (
        <h3 className="text-md font-bold mb-2 text-secondary-foreground">
          No workspaces
        </h3>
      )}
    </div>
  );

  return (
    <>
      {/* Mobile Sidebar using Sheet component */}
      <div className="lg:hidden">
        <Sheet open={isOpen} onOpenChange={setIsOpen}>
          <SheetContent className="bg-secondary" side="left">
            <SheetTitle className="-m-4 mb-0 p-4 sticky top-0 bg-background z-10 shadow-md">
              Workspaces
            </SheetTitle>
            <SheetDescription></SheetDescription>
            {getContent(false)}
          </SheetContent>
        </Sheet>
      </div>

      {/* Desktop Sidebar */}
      <div className="hidden lg:block lg:w-[var(--sidebar-width)] bg-secondary">
        {getContent(true)}
      </div>
    </>
  );
}
