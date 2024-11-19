"use client";

import PageTransitionLink from "@/components/PageTransitionLink";
import QueryClientProvider, {
  queryClient,
} from "@/components/QueryClientProvider";
import SignOutButton from "@/components/SignOutButton";
import { Input } from "@/components/ui/input";
import { Separator } from "@/components/ui/separator";
import {
  Sheet,
  SheetContent,
  SheetDescription,
  SheetTitle,
} from "@/components/ui/sheet";
import emitter from "@/lib/eventEmitter";
import { ConversationModel, WorkspaceModel } from "@/lib/sesameApi";
import { cn } from "@/lib/utils";
import { Edit, LoaderCircleIcon } from "lucide-react";
import { usePathname, useRouter } from "next/navigation";
import {
  Suspense,
  useCallback,
  useDeferredValue,
  useEffect,
  useState,
} from "react";
import ConversationList from "./ConversationList";
import SearchResults from "./SearchResults";
import SidebarTitle from "./SidebarTitle";

interface SidebarProps {
  conversations: ConversationModel[];
  signOut?: boolean;
  workspace?: WorkspaceModel | null;
  workspaces?: WorkspaceModel[];
}

export default function Sidebar({
  conversations,
  signOut = false,
  workspace,
  workspaces = [],
}: SidebarProps) {
  const { refresh } = useRouter();
  const [isOpen, setIsOpen] = useState(false);
  const [search, setSearch] = useState("");
  const deferredSearch = useDeferredValue(search.trim());

  const pathname = usePathname();

  useEffect(() => {
    const toggleSidebar = () => setIsOpen((prev) => !prev);
    const updateSidebar = () =>
      queryClient.invalidateQueries({
        queryKey: ["conversations", workspace?.workspace_id],
      });
    const handleResize = () => {
      if (window.innerWidth >= 1024) setIsOpen(false);
    };
    emitter.on("toggleSidebar", toggleSidebar);
    emitter.on("updateSidebar", updateSidebar);
    window.addEventListener("resize", handleResize);
    return () => {
      emitter.off("toggleSidebar", toggleSidebar);
      emitter.off("updateSidebar", updateSidebar);
      window.removeEventListener("resize", handleResize);
    };
  }, [workspace?.workspace_id]);

  useEffect(() => {
    if (!workspace?.workspace_id) return;
    if (
      !pathname.match(new RegExp(`\/${workspace.workspace_id}\/c\/[a-f0-9-]+`))
    )
      return;
    // Attempt to refresh conversations, if the current conversation isn't included in the list
    if (conversations.every((c) => !pathname.includes(c.conversation_id))) {
      refresh();
    }
  }, [conversations, pathname, refresh, workspace?.workspace_id]);

  const handleClick = useCallback(() => setIsOpen(false), []);

  const content = (
    <>
      <div className="flex flex-col gap-1">
        {workspace && (
          <PageTransitionLink
            href={`/${workspace.workspace_id}`}
            className={cn(
              "flex items-center gap-3 px-3 py-2 rounded-lg transition-colors hover:bg-secondary-foreground/[.05]",
              {
                "bg-input": pathname === `/${workspace.workspace_id}`,
              }
            )}
            onClick={() => setIsOpen(false)}
          >
            <Edit size={21} />
            New chat
          </PageTransitionLink>
        )}
      </div>

      <Separator />

      <Input
        type="search"
        placeholder="Search conversations…"
        value={search}
        onChange={(ev) => setSearch(ev.target.value)}
      />

      <QueryClientProvider>
        {workspace ? (
          deferredSearch ? (
            <Suspense
              fallback={<LoaderCircleIcon className="animate-spin" size={16} />}
            >
              <SearchResults
                onClick={handleClick}
                onReset={() => setSearch("")}
                query={deferredSearch}
                workspaceId={workspace.workspace_id}
              />
            </Suspense>
          ) : (
            <ConversationList
              conversations={conversations}
              onClick={handleClick}
              workspaceId={workspace.workspace_id}
            />
          )
        ) : null}
      </QueryClientProvider>

      {signOut && (
        <SignOutButton className="sticky bottom-0 z-10 mt-auto">Sign out</SignOutButton>
      )}
    </>
  );

  return (
    <>
      {/* Mobile Sidebar using Sheet component */}
      <div className="lg:hidden">
        <Sheet open={isOpen} onOpenChange={setIsOpen}>
          <SheetContent side="left" className="overflow-y-auto">
            <SheetTitle asChild>
              <SidebarTitle
                className="bg-background -top-4"
                onSwitchWorkspace={() => {
                  setIsOpen(false);
                }}
                workspace={workspace}
                workspaces={workspaces}
              />
            </SheetTitle>
            <SheetDescription></SheetDescription>
            <div className="flex flex-col gap-6 p-4 min-h-full">{content}</div>
          </SheetContent>
        </Sheet>
      </div>

      {/* Desktop Sidebar */}
      <div className="hidden lg:block lg:w-[var(--sidebar-width)] bg-secondary sticky top-0 overflow-y-auto h-dvh">
        <div className="flex flex-col gap-6 p-4 min-h-full">
          <SidebarTitle
            className="bg-secondary"
            workspace={workspace}
            workspaces={workspaces}
          />
          {content}
        </div>
      </div>
    </>
  );
}
