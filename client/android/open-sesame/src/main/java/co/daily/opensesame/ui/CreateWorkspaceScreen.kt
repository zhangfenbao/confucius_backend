package co.daily.opensesame.ui

import ai.rtvi.client.result.Result
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import co.daily.opensesame.LoadingOverlay
import co.daily.opensesame.WorkspaceConfig
import co.daily.opensesame.ui.theme.LocalAppTheme

@Composable
fun CreateWorkspaceScreen(
    onBack: () -> Unit,
) {
    BackHandler(onBack = onBack)

    val theme = LocalAppTheme.current
    val api = LocalDataApi.current

    var loading by remember { mutableStateOf(false) }
    val errors = remember { mutableStateListOf<String>() }

    val scrollState = rememberScrollState()

    Box(Modifier.fillMaxSize()) {
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
        ) {

            ActionBar(title = "Create Workspace")

            var newConfig by remember { mutableStateOf(WorkspaceConfig.Default) }

            theme.menuDarker.StyledMenu(Modifier.padding(20.dp)) {

                WorkspaceConfigEditor(onConfigChanged = { newConfig = it }, config = newConfig)

                Button(
                    onClick = {
                        loading = true
                        api.createWorkspace(
                            title = newConfig.title,
                            config = newConfig.asNewJsonConfig().toImmutable()
                        ).withCallback { result ->
                            loading = false

                            when (result) {
                                is Result.Err -> errors.add(result.error.description)
                                is Result.Ok -> onBack()
                            }
                        }
                    },
                    name = "Save"
                )

            }
        }

        if (loading) {
            LoadingOverlay()
        }
    }

    errors.firstOrNull()?.let { error ->
        ErrorDialog(onDismissRequest = { errors.removeFirstOrNull() }, text = error)
    }
}