import { revalidateConversations } from "@/app/actions";
import { getApiClient } from "@/lib/sesameApiClient";
import { NextRequest, NextResponse } from "next/server";

export async function DELETE(request: NextRequest) {
  try {
    const { conversation_id } = await request.json();

    const apiClient = await getApiClient();
    const response =
      await apiClient.api.deleteConversationApiConversationsConversationIdDelete(
        conversation_id
      );

    const json = await response.json();

    revalidateConversations();

    return NextResponse.json(json);
  } catch (error) {
    console.error("Failed to delete conversation:", error);
    return NextResponse.json(
      {
        error:
          error instanceof Error
            ? error.message
            : "Failed to delete conversation",
      },
      { status: 500 }
    );
  }
}
