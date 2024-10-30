package co.daily.opensesame.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyItemScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import co.daily.opensesame.ConfigConstants
import co.daily.opensesame.R
import co.daily.opensesame.WorkspaceId
import co.daily.opensesame.api.RepeatedFetchResultRenderer
import co.daily.opensesame.api.RepeatingFetch
import co.daily.opensesame.api.rememberRepeatingFetchState
import co.daily.opensesame.ui.theme.LocalAppTheme
import co.daily.opensesame.ui.theme.StyledText

enum class WorkspacesSort {
    Recent,
    Alphabetical
}

@Composable
fun WorkspacesScreen(
    onWorkspaceClicked: (WorkspaceId) -> Unit,
    onEditWorkspaceClicked: (WorkspaceId) -> Unit,
    onCreateWorkspaceClicked: () -> Unit,
    onAppSettingsClicked: () -> Unit,
) {
    val theme = LocalAppTheme.current.workspaces

    Column(Modifier.fillMaxSize()) {

        val api = LocalDataApi.current
        val fetchState =
            rememberRepeatingFetchState(
                key = Unit,
                default = emptyList(),
                cacheLookupResult = api.cacheLookupAllWorkspaces()
            )

        RepeatingFetch(fetchState) { api.getWorkspaces() }

        ActionBar(
            onNavButtonClicked = null,
            onSettingsButtonClicked = onAppSettingsClicked,
            settingsButtonIcon = R.drawable.cog,
            title = "Workspaces"
        )

        RepeatedFetchResultRenderer(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            state = fetchState,
            loadingSpinnerColor = theme.foreground
        ) { workspaces ->

            var sort by remember { mutableStateOf(WorkspacesSort.Recent) }

            val workspacesSorted = when (sort) {
                WorkspacesSort.Recent -> workspaces.sortedByDescending { it.updatedAt }
                WorkspacesSort.Alphabetical -> workspaces.sortedBy {
                    it.title?.lowercase() ?: "Unnamed workspace"
                }
            }

            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentPadding = PaddingValues(15.dp)
            ) {
                item {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        SortButton(
                            onClick = { sort = WorkspacesSort.Recent },
                            selected = sort == WorkspacesSort.Recent,
                            name = "Recent"
                        )

                        SortButton(
                            onClick = { sort = WorkspacesSort.Alphabetical },
                            selected = sort == WorkspacesSort.Alphabetical,
                            name = "Alphabetical"
                        )
                    }
                }

                item {
                    Spacer(Modifier.height(32.dp))
                    theme.header.StyledText("Your Workspaces")
                    Spacer(Modifier.height(32.dp))
                }

                items(items = workspacesSorted, key = { it.id.id }) { workspace ->

                    Row(
                        Modifier
                            .animateItem()
                            .fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(12.dp))
                                .background(theme.itemBackground)
                                .clickable { onWorkspaceClicked(workspace.id) }
                                .padding(15.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                Modifier
                                    .clip(CircleShape)
                                    .background(theme.defaultIconBackground)
                                    .size(48.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    modifier = Modifier.size(24.dp),
                                    painter = painterResource(R.drawable.frame),
                                    tint = theme.foreground,
                                    contentDescription = null,
                                )
                            }

                            Spacer(Modifier.width(15.dp))

                            Column {
                                theme.itemTitle.StyledText(workspace.title?.takeUnless { it.isBlank() }
                                    ?: "Unnamed")

                                Spacer(Modifier.height(3.dp))

                                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {

                                    val llmModelId = workspace.getConfigOptionString("llm", "model")

                                    val llmModelName =
                                        ConfigConstants.llmModelNamesById[llmModelId] ?: llmModelId

                                    theme.itemKey.StyledText("LLM")
                                    theme.itemValue.StyledText(llmModelName ?: "Unknown model")
                                }
                            }
                        }

                        Spacer(Modifier.width(10.dp))

                        Box(
                            Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .clickable { onEditWorkspaceClicked(workspace.id) }
                                .padding(6.dp)
                        ) {
                            Icon(
                                modifier = Modifier.size(24.dp),
                                painter = painterResource(R.drawable.edit),
                                contentDescription = "Edit workspace",
                                tint = theme.editButton
                            )
                        }
                    }

                    Spacer(Modifier.height(9.dp))
                }

                item {
                    CreateNewWorkspaceButton(onClick = onCreateWorkspaceClicked)
                }
            }

        }
    }
}

@Composable
private fun SortButton(onClick: () -> Unit, selected: Boolean, name: String) {
    val theme = LocalAppTheme.current.workspaces

    val background by animateColorAsState(if (selected) theme.foreground else theme.itemBackground)
    val foreground by animateColorAsState(if (selected) theme.background else theme.foreground)

    Box(
        Modifier
            .clip(RoundedCornerShape(100))
            .background(background)
            .clickable(onClick = onClick)
            .padding(horizontal = 15.dp, vertical = 9.dp)
    ) {
        theme.sortButtonText.StyledText(text = name, color = foreground)
    }
}

@Composable
private fun LazyItemScope.CreateNewWorkspaceButton(onClick: () -> Unit) {

    val theme = LocalAppTheme.current.workspaces

    Row(
        modifier = Modifier
            .animateItem()
            .clickable(onClick = onClick)
            .padding(horizontal = 24.dp, vertical = 20.dp)
            .fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterHorizontally)
    ) {
        Icon(
            modifier = Modifier.size(20.dp),
            painter = painterResource(R.drawable.add_box),
            tint = theme.createWorkspaceIcon,
            contentDescription = null,
        )

        theme.createWorkspaceText.StyledText("Create new workspace")
    }
}

@Preview
@Composable
private fun PreviewWorkspacesScreen() {
    AppContextPreview {
        WorkspacesScreen({}, {}, {}, {})
    }
}