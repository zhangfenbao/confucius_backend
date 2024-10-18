import APIKeyRow, { ServiceConfig } from "./APIKeyRow";
import { WorkspaceFormConfig } from "./ConfigurationForm";
import ConfigurationGroup from "./ConfigurationGroup";
import ConfigurationItem from "./ConfigurationItem";

interface Props {
  formState: WorkspaceFormConfig;
  setFormState: React.Dispatch<React.SetStateAction<WorkspaceFormConfig>>;
}

export default function APIKeysSection({ formState, setFormState }: Props) {
  const services = formState.apiKeys;
  const { llmProvider } = formState.configuration.model;
  const { ttsProvider } = formState.voiceSettings.defaultVoice;

  const dailyKey = services.find((s) => s.service === "daily")?.apiKey ?? "";
  const llmKey = services.find((s) => s.service === llmProvider)?.apiKey ?? "";
  const ttsKey = services.find((s) => s.service === ttsProvider)?.apiKey ?? "";
  const sttKey = services.find((s) => s.service === "deepgram")?.apiKey ?? "";

  const handleChangeServiceConfig = (config: ServiceConfig) => {
    setFormState((state) => {
      const services = [...state.apiKeys];
      const idx = services.findIndex((s) => s.service === config.service);
      if (idx >= 0) {
        services[idx] = config;
      } else {
        services.push(config);
      }
      return {
        ...state,
        apiKeys: services,
      };
    });
  };

  const config = {
    daily: {
      key: dailyKey,
      required: true,
    },
    [llmProvider]: {
      key: llmKey,
      required: false,
    },
    [ttsProvider]: {
      key: ttsKey,
      required: false,
    },
    deepgram: {
      key: sttKey,
      required: false,
    },
  };

  return (
    <ConfigurationGroup label="API Keys">
      {Object.entries(config).map(([service, { key, required }]) => (
        <ConfigurationItem key={service}>
          <APIKeyRow
            service={service}
            apiKey={key}
            onChange={handleChangeServiceConfig}
            required={required}
          />
        </ConfigurationItem>
      ))}
    </ConfigurationGroup>
  );
}
