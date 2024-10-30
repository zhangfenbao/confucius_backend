import { getApiClient } from "@/lib/sesameApiClient";
import { NextRequest, NextResponse } from "next/server";

export async function DELETE(request: NextRequest) {
  try {
    const { workspace_id } = await request.json();

    const apiClient = await getApiClient();
    const response =
      await apiClient.api.deleteWorkspaceApiWorkspacesWorkspaceIdDelete(
        workspace_id
      );

    const json = await response.json();

    return NextResponse.json(json);
  } catch (error) {
    console.error("Failed to delete workspace:", error);
    return NextResponse.json(
      {
        error:
          error instanceof Error ? error.message : "Failed to delete workspace",
      },
      { status: 500 }
    );
  }
}
