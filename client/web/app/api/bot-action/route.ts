import { getApiClient } from "@/lib/sesameApiClient";
import { NextRequest, NextResponse } from "next/server";

export async function POST(request: NextRequest) {
  try {
    const json = await request.json();

    const { actions, conversation_id, workspace_id } = json;

    const apiClient = await getApiClient();
    const res = await apiClient.api.streamActionApiRtviActionPost(
      {
        conversation_id,
        workspace_id,
        actions,
      }
    );

    if (!res.ok) {
      return NextResponse.json(
        { error: "Response not okay from service" },
        { status: 500 }
      );
    }

    if (!res.body) {
      return NextResponse.json(
        { error: "No stream from completions endpoint" },
        { status: 500 }
      );
    }

    return new Response(res.body, {
      status: res.status,
      headers: {
        "Content-Type": res.headers.get("Content-Type") ?? "text/plain",
        "Transfer-Encoding": "chunked", // Ensure chunked encoding for streaming
      },
    });
  } catch (error) {
    console.error("Failed to send text message:", error);
    if (error instanceof Response) {
      console.error(error.status, error.statusText, await error.text());
    }
    return NextResponse.json(
      {
        error:
          error instanceof Error
            ? error.message
            : "Failed to send text message",
      },
      { status: 500 }
    );
  }
}
