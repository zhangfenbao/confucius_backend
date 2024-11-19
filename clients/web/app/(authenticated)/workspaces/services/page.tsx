"use server";

import { getAvailableServices, getServices } from "@/lib/services";
import {
  ServiceInfo,
  ServiceModel,
  WorkspaceWithConversations,
} from "@/lib/sesameApi";
import { getWorkspaces } from "@/lib/workspaces";
import ServiceConfig from "./ServiceConfig";

export default async function ServicesPage() {
  let workspaces: WorkspaceWithConversations[];
  let services: ServiceModel[];
  let availableServices: ServiceInfo[];

  try {
    workspaces = await getWorkspaces();
    services = await getServices();
    availableServices = await getAvailableServices();
  } catch {
    workspaces = [];
    services = [];
    availableServices = [];
  }

  return (
    <div className="animate-appear flex flex-col gap-4">
      <h1 className="font-semibold text-xl">Services</h1>
      <ServiceConfig
        availableServices={availableServices}
        services={services}
        workspaces={workspaces}
      />
    </div>
  );
}
