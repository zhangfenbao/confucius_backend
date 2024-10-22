import { revalidateAll } from "@/app/actions";
import { getApiClient } from "@/lib/sesameApiClient";
import { NextRequest, NextResponse } from "next/server";

export async function PUT(request: NextRequest) {
  try {
    const { conversation_id, title } = await request.json();

    const apiClient = await getApiClient();
    const response =
      await apiClient.api.updateConversationTitleApiConversationsConversationIdPut(
        conversation_id,
        {
          title,
        }
      );

    const json = await response.json();

    await revalidateAll();

    return NextResponse.json(json);
  } catch (error) {
    console.error("Failed to update conversation:", error);
    return NextResponse.json(
      {
        error:
          error instanceof Error
            ? error.message
            : "Failed to update conversation",
      },
      { status: 500 }
    );
  }
}
