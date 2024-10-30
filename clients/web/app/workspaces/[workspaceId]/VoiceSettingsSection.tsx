import { Label } from "@/components/ui/label";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";
import { ToggleGroup, ToggleGroupItem } from "@/components/ui/toggle-group";
import { Tooltip, TooltipContent, TooltipProvider, TooltipTrigger } from "@/components/ui/tooltip";
import { getVoicesByProvider, InteractionMode, languageOptions, TTSService } from "@/lib/voice";
import { HelpCircleIcon } from "lucide-react";
import { WorkspaceFormConfig } from "./ConfigurationForm";
import ConfigurationGroup from "./ConfigurationGroup";
import ConfigurationItem from "./ConfigurationItem";

interface Props {
  formState: WorkspaceFormConfig;
  setFormState: React.Dispatch<React.SetStateAction<WorkspaceFormConfig>>;
}

export default function VoiceSettingsSection({
  formState,
  setFormState,
}: Props) {
  const { mainLanguage, defaultVoice, interactionMode } = formState.voiceSettings;

  const validVoices = getVoicesByProvider(
    defaultVoice.ttsProvider,
    mainLanguage
  );

  const handleProviderChange = (provider: TTSService) => {
    if (!provider) return;
    const firstEnglishVoice = getVoicesByProvider(provider, "en")[0];
    setFormState((state) => ({
      ...state,
      voiceSettings: {
        ...state.voiceSettings,
        mainLanguage: "en", // Reset to English
        defaultVoice: {
          ttsProvider: provider,
          selectedVoice: firstEnglishVoice.voiceId, // Reset voice on provider change
        },
      },
    }));
  };

  const handleInteractionModeChange = (interactionMode: InteractionMode) => {
    if (!interactionMode) return;
    setFormState((state) => ({
      ...state,
      voiceSettings: {
        ...state.voiceSettings,
        interactionMode,
      },
    }));
  }

  return (
    <ConfigurationGroup label="Voice Settings">
      {/* TTS Provider */}
      <fieldset>
        <ConfigurationItem>
          <legend className="text-base font-semibold">TTS Provider</legend>
          <ToggleGroup
            type="single"
            variant="outline"
            className="justify-start"
            value={defaultVoice.ttsProvider}
            onValueChange={handleProviderChange}
          >
            <ToggleGroupItem
              id="cartesia"
              value="cartesia"
              aria-label="Cartesia"
            >
              Cartesia
            </ToggleGroupItem>
            <ToggleGroupItem
              id="elevenlabs"
              value="elevenlabs"
              aria-label="ElevenLabs"
            >
              ElevenLabs
            </ToggleGroupItem>
          </ToggleGroup>
        </ConfigurationItem>
      </fieldset>

      {/* Main Language */}
      <ConfigurationItem>
        <Label className="text-base font-semibold" htmlFor="language">
          Main language
        </Label>
        <div>
          <Select
            value={mainLanguage}
            onValueChange={(lang) => {
              const availableVoices = getVoicesByProvider(
                defaultVoice.ttsProvider,
                lang
              ).map((v) => v.voiceId);

              setFormState((state) => ({
                ...state,
                voiceSettings: {
                  ...state.voiceSettings,
                  mainLanguage: lang,
                  defaultVoice: {
                    ...state.voiceSettings.defaultVoice,
                    selectedVoice: availableVoices.includes(
                      state.voiceSettings.defaultVoice.selectedVoice
                    )
                      ? state.voiceSettings.defaultVoice.selectedVoice
                      : availableVoices[0],
                  },
                },
              }));
            }}
          >
            <SelectTrigger>
              <SelectValue placeholder="Language" />
            </SelectTrigger>
            <SelectContent>
              {languageOptions
                .filter((lang) => Boolean(lang[defaultVoice.ttsProvider]))
                .filter(
                  (lang) =>
                    getVoicesByProvider(
                      defaultVoice.ttsProvider,
                      lang[defaultVoice.ttsProvider]
                    ).length > 0
                )
                .map((lang) => (
                  <SelectItem
                    key={lang.label}
                    value={lang[defaultVoice.ttsProvider]!}
                  >
                    {lang.label}
                  </SelectItem>
                ))}
            </SelectContent>
          </Select>
        </div>
      </ConfigurationItem>

      {/* Voice Dropdown */}
      <ConfigurationItem>
        <Label className="text-base font-semibold" htmlFor="voice">
          Voice
        </Label>
        <div>
          <Select
            onValueChange={(voiceId) => {
              if (validVoices.every((v) => v.voiceId !== voiceId)) return;
              setFormState((state) => ({
                ...state,
                voiceSettings: {
                  ...state.voiceSettings,
                  defaultVoice: {
                    ...state.voiceSettings.defaultVoice,
                    selectedVoice: voiceId,
                  },
                },
              }));
            }}
            value={defaultVoice.selectedVoice}
          >
            <SelectTrigger>
              <SelectValue placeholder="Voice" />
            </SelectTrigger>
            <SelectContent>
              {getVoicesByProvider(defaultVoice.ttsProvider, mainLanguage).map(
                (voice) => (
                  <SelectItem key={voice.voiceId} value={voice.voiceId}>
                    {voice.label}
                  </SelectItem>
                )
              )}
            </SelectContent>
          </Select>
        </div>
      </ConfigurationItem>

      <fieldset>
        <ConfigurationItem>
          <legend className="text-base font-semibold flex gap-2 items-center">
            Interaction mode
            <TooltipProvider>
              <Tooltip>
                <TooltipTrigger>
                  <HelpCircleIcon size={16} />
                </TooltipTrigger>
                <TooltipContent className="bg-secondary max-w-64">
                  A conversational workspace displays words as the bot speaks. Informational displays the full LLM output at once.
                </TooltipContent>
              </Tooltip>
            </TooltipProvider>
          </legend>
          <ToggleGroup
            type="single"
            variant="outline"
            className="justify-start"
            value={interactionMode}
            onValueChange={handleInteractionModeChange}
          >
            <ToggleGroupItem
              id="informational"
              value="informational"
              aria-label="Informational"
            >
              Informational
            </ToggleGroupItem>
            <ToggleGroupItem
              id="conversational"
              value="conversational"
              aria-label="Conversational"
            >
              Conversational
            </ToggleGroupItem>
          </ToggleGroup>
        </ConfigurationItem>
      </fieldset>
    </ConfigurationGroup>
  );
}
