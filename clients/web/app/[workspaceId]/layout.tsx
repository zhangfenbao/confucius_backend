"use server";

import ErrorPage from "@/components/ErrorPage";
import QueryClientProvider from "@/components/QueryClientProvider";
import { WorkspaceWithConversations } from "@/lib/sesameApi";
import { getWorkspaces } from "@/lib/workspaces";
import { redirect } from "next/navigation";
import React from "react";
import DeleteConversationModal from "./DeleteConversationModal";
import DisabledInSandboxModeModal from "./DisabledInSandboxModeModal";
import Navbar from "./Navbar";
import Sidebar from "./Sidebar";

// Define the layout for the workspace
interface WorkspaceLayoutProps {
  children: React.ReactNode;
  params: {
    workspaceId: string;
  };
}

export default async function WorkspaceLayout({
  children,
  params,
}: WorkspaceLayoutProps) {
  const { workspaceId } = params;

  let workspaces: WorkspaceWithConversations[];
  try {
    workspaces = await getWorkspaces();
  } catch {
    return (
      <ErrorPage
        title="Unable to fetch from server"
        description={`Unable to retrieve workspaces from ${process.env.SESAME_BASE_URL}. Please check your client .env file.`}
      />
    );
  }
  const workspace = workspaces.find((w) => w.workspace_id === workspaceId) ?? workspaces[0];

  if (!workspace) redirect("/");

  return (
    <div className="lg:grid lg:grid-cols-[var(--sidebar-width)_1fr] min-h-dvh">
      {/* Sidebar */}
      <Sidebar
        conversations={workspace.conversations}
        workspace={workspace}
        workspaces={workspaces}
      />

      {/* Main content area */}
      <div className="flex flex-col min-h-dvh w-full bg-background">
        {/* Navbar */}
        <Navbar />

        {/* Page content */}
        <main className="relative flex-grow mx-auto max-w-3xl w-full flex flex-col">
          {children}
        </main>
      </div>

      <QueryClientProvider>
        <DeleteConversationModal
          conversations={workspace.conversations}
          workspaceId={workspaceId}
        />
      </QueryClientProvider>
      <DisabledInSandboxModeModal />
    </div>
  );
}
