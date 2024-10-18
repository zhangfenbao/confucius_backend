import authHeaders from "@/lib/authHeaders";
import { Api, RequestParams } from "./sesameApi";

export const getApiClient = async (
  baseApiParams: Omit<RequestParams, "baseUrl" | "signal" | "cancelToken"> = {}
) => {
  const headers = authHeaders();
  return new Api({
    baseUrl: process.env.SESAME_BASE_URL,
    baseApiParams: {
      headers,
      ...baseApiParams,
    },
  });
};
