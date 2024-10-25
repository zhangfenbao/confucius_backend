package co.daily.opensesame.ui

import ai.rtvi.client.result.Result
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import co.daily.opensesame.LoadingOverlay
import co.daily.opensesame.Workspace
import co.daily.opensesame.WorkspaceConfig
import co.daily.opensesame.WorkspaceId
import co.daily.opensesame.api.RepeatedFetchResultRenderer
import co.daily.opensesame.api.RepeatingFetch
import co.daily.opensesame.api.rememberRepeatingFetchState
import co.daily.opensesame.ui.theme.LocalAppTheme
import co.daily.opensesame.ui.theme.StyledText
import co.daily.opensesame.utils.MutableJsonElement
import kotlinx.serialization.json.JsonObject

@Composable
fun EditWorkspaceScreen(
    onBack: () -> Unit,
    workspaceId: WorkspaceId
) {
    var backConfirmationDialog by remember { mutableStateOf(false) }

    BackHandler {
        backConfirmationDialog = true
    }

    if (backConfirmationDialog) {
        ConfirmationDialog(
            onYes = {
                backConfirmationDialog = false
                onBack()
            },
            onNo = {
                backConfirmationDialog = false
            },
            title = "Abandon changes",
            message = "Discard the changes made to this workspace?",
            yesText = "Discard",
            noText = "Don't discard"
        )
    }

    val theme = LocalAppTheme.current
    val api = LocalDataApi.current

    var loading by remember { mutableStateOf(false) }
    val errors = remember { mutableStateListOf<String>() }

    val scrollState = rememberScrollState()

    val fetchState = rememberRepeatingFetchState<Workspace?>(
        key = workspaceId,
        default = null,
        cacheLookupResult = null
    )

    RepeatingFetch(state = fetchState) {
        api.getWorkspace(workspaceId).map { it }
    }

    RepeatedFetchResultRenderer(
        modifier = Modifier.fillMaxSize(),
        state = fetchState,
        loadingSpinnerColor = theme.menuDarker.headerForeground
    ) { workspace ->

        if (workspace != null) {

            val originalConfig = workspace.config ?: JsonObject(emptyMap())

            var newConfig by remember {
                mutableStateOf(
                    WorkspaceConfig.fromJsonConfig(
                        title = workspace.title ?: "",
                        value = originalConfig
                    )
                )
            }

            Column(Modifier.fillMaxSize()) {
                Column(
                    Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .verticalScroll(scrollState)
                ) {
                    ActionBar(title = "Edit Workspace")

                    theme.menuDarker.StyledMenu(Modifier.padding(20.dp)) {

                        WorkspaceConfigEditor(
                            onConfigChanged = { newConfig = it },
                            config = newConfig
                        )
                    }
                }

                Column(Modifier.fillMaxWidth()) {

                    HorizontalDivider(
                        thickness = 1.dp,
                        color = theme.appConfig.textFieldBorder
                    )

                    Box(Modifier.padding(20.dp)) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .clickable(onClick = {
                                    loading = true


                                    val newRawConfig =
                                        (MutableJsonElement.fromImmutable(originalConfig) as? MutableJsonElement.Object)
                                            ?: MutableJsonElement.Object()

                                    newConfig.updateJsonConfig(newRawConfig)

                                    api
                                        .updateWorkspace(
                                            id = workspace.id,
                                            newTitle = newConfig.title,
                                            newConfig = newRawConfig.toImmutable()
                                        )
                                        .withCallback { result ->
                                            loading = false

                                            when (result) {
                                                is Result.Err -> errors.add(result.error.description)
                                                is Result.Ok -> onBack()
                                            }
                                        }
                                })
                                .background(theme.appConfig.buttonBackground)
                                .padding(horizontal = 20.dp, vertical = 15.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            theme.appConfig.buttonText.StyledText("Save")
                        }
                    }
                }
            }
        }

        if (loading || workspace == null) {
            LoadingOverlay()
        }
    }

    errors.firstOrNull()?.let { error ->
        ErrorDialog(onDismissRequest = { errors.removeFirstOrNull() }, text = error)
    }
}