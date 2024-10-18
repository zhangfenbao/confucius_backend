package co.daily.opensesame.ui

import ai.rtvi.client.types.Value
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import co.daily.opensesame.ConfigConstants
import co.daily.opensesame.LLMContextElement
import co.daily.opensesame.NamedOption
import co.daily.opensesame.NamedOptionList
import co.daily.opensesame.R
import co.daily.opensesame.WorkspaceConfig
import co.daily.opensesame.ui.theme.LocalAppTheme
import co.daily.opensesame.ui.theme.StyledText

private sealed interface SettingModalType {
    data object Name : SettingModalType
    data object Language : SettingModalType
    data object LLMProvider : SettingModalType
    data object LLMModel : SettingModalType
    data object LLMPrompt : SettingModalType
    data object TTSProvider : SettingModalType
    data object TTSVoice : SettingModalType
    data object InteractionMode : SettingModalType

    data class ApiKey(
        val id: String,
        val name: String,
    ) : SettingModalType
}

@Composable
fun MenuScope.WorkspaceConfigEditor(
    onConfigChanged: (WorkspaceConfig) -> Unit,
    config: WorkspaceConfig
) {
    var activeModal by remember { mutableStateOf<SettingModalType?>(null) }

    Group("Workspace options") {

        Item(
            onClick = { activeModal = SettingModalType.Name },
            name = "Name",
            firstInList = true,
            secondary = MenuItemSecondary.Chevron(config.title.takeUnless { it.isEmpty() }
                ?: "Unnamed")
        )

        Item(
            onClick = { activeModal = SettingModalType.Language },
            name = "Language",
            secondary = MenuItemSecondary.Chevron(config.language.displayName)
        )

        Item(
            onClick = { activeModal = SettingModalType.InteractionMode },
            name = "Interaction",
            secondary = MenuItemSecondary.Chevron(config.interactionMode.displayName)
        )
    }

    Group("Model") {
        Item(
            onClick = { activeModal = SettingModalType.LLMProvider },
            name = "Provider",
            firstInList = true,
            secondary = MenuItemSecondary.Chevron(config.llmProvider.displayName)
        )

        Item(
            onClick = { activeModal = SettingModalType.LLMModel },
            name = "Model",
            secondary = MenuItemSecondary.Chevron(config.llmModel.displayName)
        )

        Item(
            onClick = { activeModal = SettingModalType.LLMPrompt },
            name = "Prompt",
            secondary = MenuItemSecondary.Chevron()
        )
    }

    Group("Text to speech") {
        Item(
            onClick = { activeModal = SettingModalType.TTSProvider },
            name = "Provider",
            firstInList = true,
            secondary = MenuItemSecondary.Chevron(config.ttsProvider.displayName)
        )

        Item(
            onClick = { activeModal = SettingModalType.TTSVoice },
            name = "Voice",
            secondary = MenuItemSecondary.Chevron(config.ttsVoice.displayName)
        )

        Item(
            onClick = { onConfigChanged(config.copy(readMarkdownBlocks = !config.readMarkdownBlocks)) },
            name = "Read markdown blocks",
            subtitle = "Speak code blocks and tables using TTS",
            secondary = MenuItemSecondary.Checkbox(config.readMarkdownBlocks)
        )
    }

    Group("API Keys") {

        @Composable
        fun ApiKeyItem(id: String, name: String, firstInList: Boolean = false) {
            Item(
                onClick = { activeModal = SettingModalType.ApiKey(id = id, name = name) },
                name = name,
                firstInList = firstInList,
                secondary = MenuItemSecondary.Chevron(config.apiKeys[id]?.takeUnless { it.isEmpty() })
            )
        }

        ApiKeyItem(id = "daily", name = "Daily", firstInList = true)
        ApiKeyItem(id = "anthropic", name = "Anthropic")
        ApiKeyItem(id = "cartesia", name = "Cartesia")
        ApiKeyItem(id = "deepgram", name = "Deepgram")
        ApiKeyItem(id = "elevenlabs", name = "ElevenLabs")
        ApiKeyItem(id = "openai", name = "OpenAI")
        ApiKeyItem(id = "together", name = "Together")
    }

    val dismissModal = { activeModal = null }

    when (val modal = activeModal) {
        SettingModalType.Name -> {
            TextInputDialogLive(
                onUpdated = { onConfigChanged(config.copy(title = it)) },
                onDismissRequest = dismissModal,
                title = "Workspace name",
                value = config.title
            )
        }

        SettingModalType.Language -> {
            OptionsModal(
                onSelected = {
                    onConfigChanged(
                        config.copy(
                            language = it,
                            ttsVoice = config.ttsProvider.voices.get(it)
                                .byIdOrDefault(config.ttsVoice.id),
                        )
                    )
                },
                onDismissed = dismissModal,
                title = "Language",
                options = ConfigConstants.languages,
                selected = config.language
            )
        }

        SettingModalType.InteractionMode -> {
            OptionsModal(
                onSelected = {
                    onConfigChanged(config.copy(interactionMode = it))
                },
                onDismissed = dismissModal,
                title = "Interaction mode",
                options = ConfigConstants.interactionModes,
                selected = config.interactionMode
            )
        }

        is SettingModalType.ApiKey -> {
            TextInputDialogLive(
                onUpdated = { onConfigChanged(config.copy(apiKeys = config.apiKeys + (modal.id to it))) },
                onDismissRequest = dismissModal,
                title = "API key: ${modal.name}",
                value = config.apiKeys[modal.id] ?: ""
            )
        }

        SettingModalType.LLMProvider -> {
            OptionsModal(
                onSelected = {
                    onConfigChanged(
                        config.copy(
                            llmProvider = it,
                            llmModel = it.models.byIdOrDefault(config.llmModel.id)
                        )
                    )
                },
                onDismissed = dismissModal,
                title = "LLM Provider",
                options = ConfigConstants.llmProviders,
                selected = config.llmProvider
            )
        }

        SettingModalType.LLMModel -> {
            OptionsModal(
                onSelected = { onConfigChanged(config.copy(llmModel = it)) },
                onDismissed = dismissModal,
                title = "LLM Model",
                options = config.llmProvider.models,
                selected = config.llmModel
            )
        }

        SettingModalType.LLMPrompt -> {
            PromptEditModal(
                onUpdated = {
                    onConfigChanged(config.copy(prompt = it))
                },
                onDismissed = dismissModal,
                value = ImmutableMarker(config.prompt)
            )
        }

        SettingModalType.TTSProvider -> {
            OptionsModal(
                onSelected = {
                    onConfigChanged(
                        config.copy(
                            ttsProvider = it,
                            ttsVoice = it.voices.get(config.language)
                                .byIdOrDefault(config.ttsVoice.id)
                        )
                    )
                },
                onDismissed = dismissModal,
                title = "TTS Provider",
                options = ConfigConstants.ttsProviders,
                selected = config.ttsProvider
            )
        }

        SettingModalType.TTSVoice -> {
            OptionsModal(
                onSelected = { onConfigChanged(config.copy(ttsVoice = it)) },
                onDismissed = dismissModal,
                title = "TTS Voice",
                options = config.ttsProvider.voices.get(config.language),
                selected = config.ttsVoice
            )
        }

        null -> {}
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun <E : NamedOption> OptionsModal(
    onSelected: (E) -> Unit,
    onDismissed: () -> Unit,
    title: String,
    options: NamedOptionList<E>,
    selected: E
) {
    val theme = LocalAppTheme.current

    val scrollState = rememberScrollState()

    ModalBottomSheet(
        onDismissRequest = onDismissed,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = theme.sheetBackground
    ) {
        Column(Modifier.verticalScroll(scrollState)) {
            theme.menuLighter.StyledMenu(Modifier.padding(20.dp)) {
                Group(title) {

                    options.options.forEachIndexed { i, option ->
                        Item(
                            onClick = {
                                onSelected(option)
                                onDismissed()
                            },
                            name = option.displayName,
                            firstInList = i == 0,
                            secondary = MenuItemSecondary.Radio(selected == option),
                            subtitle = option.displayDescription
                        )
                    }
                }
            }

            Spacer(Modifier.padding(24.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PromptEditModal(
    onUpdated: (List<LLMContextElement>) -> Unit,
    onDismissed: () -> Unit,
    value: ImmutableMarker<List<LLMContextElement>>
) {
    val theme = LocalAppTheme.current

    ModalBottomSheet(
        onDismissRequest = onDismissed,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = theme.sheetBackground
    ) {
        val scrollState = rememberScrollState()

        Column(
            modifier = Modifier
                .verticalScroll(scrollState)
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            value.value.forEachIndexed { i, message ->
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(5.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        theme.promptEditor.role.StyledText(message.content.role.uppercase())

                        Icon(
                            modifier = Modifier.size(20.dp),
                            painter = painterResource(R.drawable.unfold_more),
                            tint = theme.promptEditor.roleSpinnerColor,
                            contentDescription = null
                        )
                    }

                    val inputShape = RoundedCornerShape(12.dp)

                    Box(
                        modifier = Modifier
                            .border(1.dp, theme.promptEditor.textInputBorder, inputShape)
                            .clip(inputShape)
                            .background(theme.promptEditor.textInputBackground)
                            .padding(12.dp)
                    ) {
                        BasicTextField(
                            modifier = Modifier.fillMaxWidth(),
                            textStyle = theme.promptEditor.textInputText,
                            value = (message.content.content as? Value.Str)?.value
                                ?: "<unknown data>",
                            cursorBrush = SolidColor(theme.promptEditor.textInputTextColor),
                            onValueChange = {
                                onUpdated(
                                    value.value.replaceElement(
                                        i,
                                        message.copy(
                                            content = LLMContextElement.Content(
                                                message.content.role,
                                                Value.Str(it)
                                            )
                                        )
                                    )
                                )
                            }
                        )
                    }
                }
            }
        }


        AddContextMessageButton(
            onClick = {
                onUpdated(
                    value.value + LLMContextElement(
                        content = LLMContextElement.Content(
                            role = when (value.value.lastOrNull()?.content?.role) {
                                "user" -> "assistant"
                                else -> "user"
                            },
                            content = Value.Str("")
                        )
                    )
                )
            }
        )
    }
}

private fun <E> List<E>.replaceElement(index: Int, value: E) =
    mapIndexed { i, v ->
        if (index == i) {
            value
        } else {
            v
        }
    }

@Composable
private fun AddContextMessageButton(onClick: () -> Unit) {

    val theme = LocalAppTheme.current.promptEditor

    Row(
        modifier = Modifier
            .clickable(onClick = onClick)
            .padding(horizontal = 24.dp, vertical = 20.dp)
            .fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterHorizontally)
    ) {
        Icon(
            modifier = Modifier.size(20.dp),
            painter = painterResource(R.drawable.add_box),
            tint = theme.addContextIcon,
            contentDescription = null,
        )

        theme.addContextText.StyledText("Add context message")
    }
}

@Preview
@Composable
private fun PreviewWorkspaceConfigEditor() {
    AppContextPreview {
        val theme = LocalAppTheme.current

        theme.menuDarker.StyledMenu {
            WorkspaceConfigEditor(
                onConfigChanged = {}, config = WorkspaceConfig.Default
            )
        }
    }
}

@Preview
@Composable
private fun PreviewWorkspaceConfigEditorModelProvider() {
    AppContextPreview {
        OptionsModal(
            onSelected = {},
            onDismissed = {},
            title = "LLM Provider",
            options = ConfigConstants.llmProviders,
            selected = ConfigConstants.llmProviders.default
        )
    }
}