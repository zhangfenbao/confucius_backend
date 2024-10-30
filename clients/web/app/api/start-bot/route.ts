import { getApiClient } from "@/lib/sesameApiClient";
import { NextRequest, NextResponse } from "next/server";

export async function POST(request: NextRequest) {
  try {
    const json = await request.json();

    const { conversation_id } = json;

    const apiClient = await getApiClient();
    const response = await apiClient.api.connectApiRtviConnectPost({
      actions: [],
      conversation_id,
    });

    const res = await response.json();
    if (response.status !== 200) {
      return NextResponse.json(res, { status: response.status });
    }
    return NextResponse.json(res);
  } catch (error) {
    console.error("Error starting bot:", error);
    return NextResponse.json(
      { error: error instanceof Error ? error.message : "Failed to start bot" },
      { status: 500 }
    );
  }
}
