import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { WorkspaceFormConfig } from "./ConfigurationForm";
import ConfigurationGroup from "./ConfigurationGroup";
import ConfigurationItem from "./ConfigurationItem";

interface Props {
  formState: WorkspaceFormConfig;
  setFormState: React.Dispatch<React.SetStateAction<WorkspaceFormConfig>>;
}

export default function WorkspaceOptionsSection({
  formState,
  setFormState,
}: Props) {
  const { name } = formState.workspaceOptions;

  return (
    <ConfigurationGroup label="Workspace Options">
      {/* Workspace Name */}
      <ConfigurationItem>
        <Label className="text-base font-semibold" htmlFor="workspace-title">
          Name
        </Label>
        <div>
          <Input
            required
            autoComplete="off"
            id="workspace-title"
            name="workspace-title"
            type="text"
            value={name}
            onChange={(e) =>
              setFormState((state) => ({
                ...state,
                workspaceOptions: {
                  ...state.workspaceOptions,
                  name: e.target.value,
                },
              }))
            }
          />
        </div>
      </ConfigurationItem>
    </ConfigurationGroup>
  );
}
