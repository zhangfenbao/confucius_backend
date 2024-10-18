"use client";

import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { EyeIcon, EyeOffIcon } from "lucide-react";
import { useId, useState } from "react";

const serviceMap: Record<string, string> = {
  daily: "Daily",
  cartesia: "Cartesia",
  together: "Together.ai",
  anthropic: "Anthropic",
  deepgram: "Deepgram",
};

export interface ServiceConfig {
  apiKey: string;
  service: string;
}

interface Props extends ServiceConfig {
  onChange: (config: ServiceConfig) => void;
  required?: boolean;
}

export default function APIKeyRow({ apiKey, onChange, required, service }: Props) {
  const label =
    serviceMap[service] ?? service[0].toUpperCase() + service.slice(1);
  const [showKey, setShowKey] = useState(false);

  const id = useId();

  return (
    <>
      <Label className="text-base font-semibold" htmlFor={id}>
        {label}
      </Label>
      <div className="flex gap-2 items-center">
        <Input
          required={required}
          autoComplete="off"
          id={id}
          name={id}
          type="text"
          value={showKey ? apiKey : apiKey.replace(/./g, '•')}
          onChange={(e) =>
            onChange({
              apiKey: e.currentTarget.value,
              service: service,
            })
          }
        />
        <Button
          size="icon"
          variant="ghost"
          type="button"
          onClick={() => setShowKey((s) => !s)}
        >
          {showKey ? <EyeIcon size={16} /> : <EyeOffIcon size={16} />}
        </Button>
      </div>
    </>
  );
}
