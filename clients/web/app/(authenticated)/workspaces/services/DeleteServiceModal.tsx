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
import { ServiceModel } from "@/lib/sesameApi";
import { useState } from "react";
import { deleteService } from "./actions";

interface Props {
  onClose: () => void;
  onDeleted: () => void;
  service: ServiceModel;
}

export default function DeleteServiceModal({
  onClose,
  onDeleted,
  service,
}: Props) {
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [error, setError] = useState("");

  const handleSubmit = async (ev: React.FormEvent<HTMLFormElement>) => {
    ev.preventDefault();
    setError("");
    setIsSubmitting(true);

    if (await deleteService(service.service_id)) {
      onDeleted();
      return;
    }
    setError("Failed to delete service.");
    setIsSubmitting(false);
  };

  return (
    <Dialog open onOpenChange={onClose}>
      <DialogContent>
        <form onSubmit={handleSubmit}>
          <DialogHeader>
            <DialogTitle>Delete service</DialogTitle>
            <DialogDescription>
              Do you really want to delete the service{" "}
              <code className="capitalize text-foreground">
                {service.title}
              </code>
              ?
            </DialogDescription>
          </DialogHeader>

          {error && <p className="text-destructive-foreground my-4">{error}</p>}

          <DialogFooter className="mt-4">
            <Button variant="secondary" onClick={onClose} type="button">
              Cancel
            </Button>
            <Button disabled={isSubmitting} variant="destructive" type="submit">
              Yes, delete
            </Button>
          </DialogFooter>
        </form>
      </DialogContent>
    </Dialog>
  );
}
