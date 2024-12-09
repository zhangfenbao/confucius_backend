import { authHeaders } from "@/lib/auth";
import { Api, RequestParams } from "./sesameApi";

export const getApiClient = async (
  baseApiParams: Omit<RequestParams, "baseUrl" | "signal" | "cancelToken"> = {}
) => {
  const headers = await authHeaders();
  console.log(process.env.SESAME_BASE_URL);
  console.log(headers);
  console.log(baseApiParams);
  return new Api({
    baseUrl: process.env.SESAME_BASE_URL,
    baseApiParams: {
      headers,
      ...baseApiParams,
    },
  });
};
