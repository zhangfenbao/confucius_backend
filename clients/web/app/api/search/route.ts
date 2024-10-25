import { getApiClient } from "@/lib/sesameApiClient";
import { NextRequest, NextResponse } from "next/server";

export async function GET(request: NextRequest) {
  try {
    const search = (request.nextUrl.searchParams.get("q") ?? "").trim();
    const workspaceId = (request.nextUrl.searchParams.get("wid") ?? "").trim();

    if (!workspaceId) {
      return NextResponse.json(
        {
          detail: "Missing or empty param: wid",
        },
        {
          status: 400,
        }
      );
    }

    if (!search) {
      return NextResponse.json(
        {
          detail: "Missing or empty param: q",
        },
        {
          status: 400,
        }
      );
    }

    const apiClient = await getApiClient();
    const response =
      await apiClient.api.searchMessagesApiConversationsWorkspaceIdSearchGet(
        workspaceId,
        {
          search_term: search,
        }
      );

    if (response.ok) {
      const json = await response.json();
      return NextResponse.json(json);
    } else {
      const text = await response.text();
      console.error(text);
      throw new Error(await response.text());
    }
  } catch (error) {
    if (error instanceof Response) {
      switch (error.status) {
        case 404:
          return NextResponse.json([], { status: 404 });
      }
    }
    console.error("Failed to search conversations:", error);
    return NextResponse.json(
      {
        error:
          error instanceof Error
            ? error.message
            : "Failed to search conversations",
      },
      { status: 500 }
    );
  }
}
