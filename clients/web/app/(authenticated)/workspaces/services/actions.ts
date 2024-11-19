"use server";

import { ServiceCreateModel, ServiceUpdateModel } from "@/lib/sesameApi";
import { getApiClient } from "@/lib/sesameApiClient";

export async function deleteService(serviceId: string) {
  const apiClient = await getApiClient();

  try {
    await apiClient.api.deleteServiceApiServicesServiceIdDelete(serviceId);
    return true;
  } catch {
    return false;
  }
}

export async function updateService(
  serviceId: string,
  data: ServiceUpdateModel
) {
  const apiClient = await getApiClient();

  try {
    await apiClient.api.updateServiceApiServicesServiceIdPut(serviceId, data);
    return true;
  } catch (e) {
    console.error(e);
    return false;
  }
}

export async function addService(
  data: ServiceCreateModel
) {
  const apiClient = await getApiClient();

  try {
    await apiClient.api.createServiceApiServicesPost(data);
    return true;
  } catch (e) {
    console.error(e);
    return false;
  }
}
