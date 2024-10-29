import { syncWorkspaceServices } from "@/lib/services";
import { WorkspaceModel } from "@/lib/sesameApi";
import { getApiClient } from "@/lib/sesameApiClient";
import { NextRequest, NextResponse } from "next/server";

export async function POST(request: NextRequest) {
  try {
    const workspace: WorkspaceModel = await request.json();

    const apiClient = await getApiClient();
    const response = await apiClient.api.createWorkspaceApiWorkspacesPost({
      config: workspace.config,
      title: workspace.title,
    });

    if (response.ok) {
      const json = await response.json();

      await syncWorkspaceServices(json as WorkspaceModel);

      return NextResponse.json(json);
    } else {
      const text = await response.text();
      console.error(text);
      throw new Error(await response.text());
    }
  } catch (error) {
    console.error("Failed to create workspace:", error);
    return NextResponse.json(
      {
        error:
          error instanceof Error ? error.message : "Failed to create workspace",
      },
      { status: 500 }
    );
  }
}
