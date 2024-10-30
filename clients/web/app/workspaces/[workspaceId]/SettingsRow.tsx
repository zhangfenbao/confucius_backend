import { Label } from "@/components/ui/label";

interface Props {
  children: React.ReactNode;
  id: string;
  label: string;
}

export default function SettingsRow({ children, id, label }: Props) {
  return (
    <div className="flex gap-4 items-center justify-between">
      <Label className="font-semibold" htmlFor={id}>
        {label}
      </Label>
      <div>{children}</div>
    </div>
  );
}
