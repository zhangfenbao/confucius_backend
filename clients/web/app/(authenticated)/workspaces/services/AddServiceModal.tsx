"use client";

import { Button } from "@/components/ui/button";
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from "@/components/ui/dialog";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
} from "@/components/ui/select";
import { ServiceInfo, WorkspaceModel } from "@/lib/sesameApi";
import { LoaderCircleIcon } from "lucide-react";
import { useId, useState } from "react";
import { addService } from "./actions";

type ServiceType = "llm" | "stt" | "tts" | "transport;";

interface Props {
  initialType?: ServiceType;
  initialWorkspaceId?: string;
  onClose: () => void;
  onSaved: () => void;
  services: ServiceInfo[];
  workspaces: WorkspaceModel[];
}

export default function AddServiceModal({
  initialType = "llm",
  initialWorkspaceId = "all",
  onClose,
  onSaved,
  services,
  workspaces,
}: Props) {
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [error, setError] = useState("");

  const [serviceType, setServiceType] = useState<ServiceType>(initialType);
  const [serviceProvider, setServiceProvider] = useState("");
  const [workspaceId, setWorkspaceId] = useState(initialWorkspaceId);

  const workspace = workspaces.find((ws) => ws.workspace_id === workspaceId);
  const service = services.find((s) => s.service_name === serviceProvider);

  const handleSubmit = async (ev: React.FormEvent<HTMLFormElement>) => {
    ev.preventDefault();

    const formData = new FormData(ev.currentTarget);
    const title = formData.get("title");
    const apiKey = formData.get("apiKey");

    if (!title || !apiKey) return;

    setError("");
    setIsSubmitting(true);

    const result = await addService({
      api_key: apiKey.toString(),
      service_provider: serviceProvider,
      service_type: serviceType,
      title: title.toString(),
      options: {},
    });

    if (result) {
      onSaved();
      return;
    }

    setError("Failed to add service.");
    setIsSubmitting(false);
  };

  const typeId = useId();
  const providerId = useId();
  const titleId = useId();
  const apiKeyId = useId();
  const workspaceFormId = useId();

  return (
    <Dialog open onOpenChange={onClose}>
      <DialogContent>
        <form onSubmit={handleSubmit}>
          <DialogHeader>
            <DialogTitle>Add service</DialogTitle>
            <DialogDescription>
              Add a new service configuration. Click save when you&apos;re done.
            </DialogDescription>
          </DialogHeader>

          <div className="flex flex-col gap-2">
            <fieldset>
              <Label htmlFor={typeId}>Service Type</Label>
              <Select
                onValueChange={(v: ServiceType) => setServiceType(v)}
                value={serviceType}
              >
                <SelectTrigger id={typeId}>{serviceType}</SelectTrigger>
                <SelectContent>
                  <SelectItem value="llm">LLM</SelectItem>
                  <SelectItem value="stt">STT</SelectItem>
                  <SelectItem value="tts">TTS</SelectItem>
                  <SelectItem value="transport">Transport</SelectItem>
                </SelectContent>
              </Select>
            </fieldset>

            <fieldset>
              <Label htmlFor={providerId}>Provider</Label>
              <Select
                disabled={!serviceType}
                onValueChange={setServiceProvider}
                value={serviceProvider}
              >
                <SelectTrigger id={providerId}>
                  <span className="capitalize">{serviceProvider}</span>
                </SelectTrigger>
                <SelectContent>
                  {services
                    .filter((s) => s.service_type === serviceType)
                    .map((s) => (
                      <SelectItem key={s.service_name} value={s.service_name}>
                        <span className="capitalize">{s.service_name}</span>
                      </SelectItem>
                    ))}
                </SelectContent>
              </Select>
            </fieldset>

            <fieldset>
              <Label htmlFor={titleId}>Title</Label>
              <Input id={titleId} name="title" required />
            </fieldset>

            <fieldset>
              <Label htmlFor={apiKeyId}>
                API Key {service?.requires_api_key && "(required)"}
              </Label>
              <Input
                id={apiKeyId}
                name="apiKey"
                required={service?.requires_api_key}
              />
            </fieldset>

            <fieldset>
              <Label htmlFor={workspaceFormId}>Workspace</Label>
              <Select onValueChange={setWorkspaceId} value={workspaceId}>
                <SelectTrigger>{workspace?.title ?? "All"}</SelectTrigger>
                <SelectContent>
                  <SelectItem value="all">All</SelectItem>
                  {workspaces.map((ws) => (
                    <SelectItem key={ws.workspace_id} value={ws.workspace_id}>
                      {ws.title}
                    </SelectItem>
                  ))}
                </SelectContent>
              </Select>
            </fieldset>

            {error && <p className="text-destructive">{error}</p>}
          </div>

          <DialogFooter className="mt-4">
            <Button className="gap-1" disabled={isSubmitting} type="submit">
              {isSubmitting && (
                <LoaderCircleIcon className="animate-spin" size={16} />
              )}
              Save
            </Button>
          </DialogFooter>
        </form>
      </DialogContent>
    </Dialog>
  );
}
