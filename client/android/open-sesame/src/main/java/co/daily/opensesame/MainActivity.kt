package co.daily.opensesame

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import co.daily.opensesame.ui.AppConfigScreen
import co.daily.opensesame.ui.AppContext
import co.daily.opensesame.ui.ChatScreen
import co.daily.opensesame.ui.CreateWorkspaceScreen
import co.daily.opensesame.ui.EditWorkspaceScreen
import co.daily.opensesame.ui.MenuItemSecondary
import co.daily.opensesame.ui.StyledMenu
import co.daily.opensesame.ui.WorkspaceDrawer
import co.daily.opensesame.ui.WorkspacesScreen
import co.daily.opensesame.ui.theme.LocalAppTheme
import kotlinx.coroutines.launch


class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            AppContext {
                val theme = LocalAppTheme.current
                val scope = rememberCoroutineScope()

                val apiBaseUrl = Preferences.backendUrl.value
                val apiKey = Preferences.apiKey.value

                val apiDetailsSet: Boolean by remember {
                    derivedStateOf {
                        !apiBaseUrl.isNullOrBlank() && !apiKey.isNullOrBlank()
                    }
                }

                var activeScreen by remember { mutableStateOf(if (apiDetailsSet) Screen.Workspaces else Screen.AppConfig) }

                AnimatedContent(
                    targetState = activeScreen,
                    transitionSpec = {
                        if (targetState.precedence == initialState.precedence) {
                            EnterTransition.None togetherWith ExitTransition.None
                        } else if (targetState.precedence < initialState.precedence) {
                            ContentTransform(
                                EnterTransition.None,
                                slideOutVertically(tween(500)) { it },
                                -1f
                            )
                        } else {
                            ContentTransform(
                                slideInVertically(tween(500)) { it },
                                ExitTransition.KeepUntilTransitionsFinished,
                                1f
                            )
                        }
                    },
                    label = "navigation slide"
                ) { activeScreenVal ->

                    Box(
                        Modifier
                            .fillMaxSize()
                            .background(theme.activityBackground)
                            .systemBarsPadding()
                            .imePadding()
                    ) {
                        when (activeScreenVal) {
                            Screen.CreateWorkspace -> {
                                CreateWorkspaceScreen(onBack = { activeScreen = Screen.Workspaces })
                            }

                            is Screen.EditWorkspace -> {
                                EditWorkspaceScreen(
                                    onBack = { activeScreen = activeScreenVal.cameFrom },
                                    workspaceId = activeScreenVal.workspaceId
                                )
                            }

                            is Screen.AppConfig -> {
                                BackHandler {
                                    activeScreen = Screen.Workspaces
                                }

                                AppConfigScreen(onClose = { activeScreen = Screen.Workspaces })
                            }

                            is Screen.Chat -> {

                                BackHandler {
                                    activeScreen = Screen.Workspaces
                                }

                                var conversationId by remember {
                                    mutableStateOf(activeScreenVal.initialConversationId)
                                }

                                val drawerState =
                                    rememberDrawerState(
                                        initialValue = if (conversationId == null) {
                                            DrawerValue.Open
                                        } else {
                                            DrawerValue.Closed
                                        }
                                    )

                                WorkspaceDrawer(
                                    onConversationSelected = {
                                        scope.launch {
                                            conversationId = it
                                            drawerState.close()
                                        }
                                    },
                                    onWorkspacesSelected = { activeScreen = Screen.Workspaces },
                                    selectedWorkspace = activeScreenVal.workspaceId,
                                    selectedChat = conversationId,
                                    drawerState = drawerState,
                                ) {
                                    ChatScreen(
                                        onNavButtonClicked = { scope.launch { drawerState.open() } },
                                        onEditWorkspaceClicked = {
                                            activeScreen = Screen.EditWorkspace(
                                                workspaceId = activeScreenVal.workspaceId,
                                                cameFrom = Screen.Chat(
                                                    activeScreenVal.workspaceId,
                                                    conversationId
                                                )
                                            )
                                        },
                                        workspaceId = activeScreenVal.workspaceId,
                                        conversationId = conversationId,
                                    )
                                }
                            }

                            Screen.Workspaces -> {
                                WorkspacesScreen(
                                    onWorkspaceClicked = {
                                        activeScreen = Screen.Chat(
                                            workspaceId = it,
                                            initialConversationId = null
                                        )
                                    },
                                    onCreateWorkspaceClicked = {
                                        activeScreen = Screen.CreateWorkspace
                                    },
                                    onEditWorkspaceClicked = {
                                        activeScreen = Screen.EditWorkspace(it, Screen.Workspaces)
                                    },
                                    onAppSettingsClicked = {
                                        activeScreen = Screen.AppConfig
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
@Preview
fun WorkspaceSettingsMenu() {
    LocalAppTheme.current.menuDarker.StyledMenu {

        Group("Configuration") {

            Item(
                name = "Model",
                onClick = { },
                firstInList = true,
                secondary = MenuItemSecondary.Chevron("Llama 3.1 70B"),
            )

            Item(
                name = "Prompt",
                onClick = { },
                secondary = MenuItemSecondary.Chevron()
            )

            Item(
                name = "Data storage",
                onClick = { },
                secondary = MenuItemSecondary.Chevron("Default")
            )
        }

        Group("Voice settings") {

            Item(
                icon = R.drawable.cog,
                name = "Main language",
                onClick = null,
                firstInList = true,
                secondary = MenuItemSecondary.Chevron("English")
            )

            Item(
                name = "Default voice",
                onClick = null,
                secondary = MenuItemSecondary.Chevron("Default")
            )
        }

        Group("Style") {

            Item(
                name = "Color scheme",
                onClick = null,
                firstInList = true,
                secondary = MenuItemSecondary.Chevron("System")
            )
        }

        Group("Workspace options") {

            Item(
                name = "Name",
                onClick = null,
                firstInList = true,
                secondary = MenuItemSecondary.Chevron("Marcus' Workspace")
            )

            Item(
                name = "Icon",
                onClick = null,
                secondary = MenuItemSecondary.Chevron("Default")
            )

            Item(
                name = "Default workspace",
                onClick = null,
                secondary = MenuItemSecondary.Checkbox(true)
            )
        }
    }
}