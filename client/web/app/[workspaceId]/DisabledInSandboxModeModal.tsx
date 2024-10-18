"use client";

import { Button } from "@/components/ui/button";
import {
  Dialog,
  DialogClose,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from "@/components/ui/dialog";
import emitter from "@/lib/eventEmitter";
import { useEffect, useState } from "react";

export default function DisabledInSandboxModeModal() {
  const [dialogOpen, setDialogOpen] = useState(false);

  useEffect(() => {
    const handleMessage = () => setDialogOpen(true);
    emitter.on("disabledInSandboxMode", handleMessage);
    return () => {
      emitter.off("disabledInSandboxMode", handleMessage);
    };
  }, []);

  return (
    <Dialog open={dialogOpen} onOpenChange={setDialogOpen}>
      <DialogContent>
        <DialogHeader>
          <DialogTitle>Disabled in sandbox mode</DialogTitle>
          <DialogDescription>
            This feature is disabled in sandbox mode.
          </DialogDescription>
          <DialogFooter>
            <DialogClose asChild>
              <Button variant="secondary">Close</Button>
            </DialogClose>
          </DialogFooter>
        </DialogHeader>
      </DialogContent>
    </Dialog>
  );
}
