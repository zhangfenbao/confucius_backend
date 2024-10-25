import { revalidateAll } from "@/app/actions";
import { getApiClient } from "@/lib/sesameApiClient";
import { NextRequest, NextResponse } from "next/server";

export async function POST(request: NextRequest) {
  try {
    const { title, workspace_id } = await request.json();

    const apiClient = await getApiClient();
    const response =
      await apiClient.api.createConversationApiConversationsPost(
        {
          title,
          workspace_id: workspace_id
        }
      );

    const json = await response.json();

    await revalidateAll();

    return NextResponse.json(json);
  } catch (error) {
    console.error("Failed to create conversation:", error);
    return NextResponse.json(
      {
        error:
          error instanceof Error
            ? error.message
            : "Failed to create conversation",
      },
      { status: 500 }
    );
  }
}
