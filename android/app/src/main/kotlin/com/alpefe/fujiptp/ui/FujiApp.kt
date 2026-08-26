package com.alpefe.fujiptp.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CollectionsBookmark
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.alpefe.fujiptp.ui.editor.EditorScreen
import com.alpefe.fujiptp.ui.home.HomeScreen
import com.alpefe.fujiptp.ui.DiagnosticsScreen
import com.alpefe.fujiptp.ui.home.backlog.BacklogScreen
import com.alpefe.fujiptp.ui.home.backlog.CollectionScreen
import com.alpefe.fujiptp.ui.home.discover.DiscoverCollectionScreen
import com.alpefe.fujiptp.ui.home.discover.DiscoverRecipeDetailScreen
import com.alpefe.fujiptp.ui.home.discover.DiscoverScreen
import com.alpefe.fujiptp.ui.theme.Canvas
import com.alpefe.fujiptp.ui.theme.Ink
import com.alpefe.fujiptp.ui.theme.InkSoft
import com.alpefe.fujiptp.ui.theme.Lavender
import com.alpefe.fujiptp.ui.theme.LavenderDeep
import com.alpefe.fujiptp.ui.theme.Peach
import com.alpefe.fujiptp.ui.theme.PeachDeep
import com.alpefe.fujiptp.ui.theme.Surface
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.receiveAsFlow

@Composable
fun FujiApp(viewModel: FujiViewModel = viewModel()) {
    val screen by viewModel.screen.collectAsStateWithLifecycle()
    val busy by viewModel.busy.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    var showSplash by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        viewModel.messages.receiveAsFlow().collectLatest { snackbar.showSnackbar(it) }
    }

    if (showSplash) {
        SplashScreen(onFinished = { showSplash = false })
        return
    }

    // Global screen transition: gentle slide + fade. The MainScaffold (tabs)
    // stays mounted; only the inner destination animates.
    AnimatedContent(
        targetState = screen,
        transitionSpec = {
            (slideInHorizontally(tween(220)) { it / 10 } + fadeIn(tween(180)))
                .togetherWith(slideOutHorizontally(tween(180)) { -it / 12 } + fadeOut(tween(140)))
        },
        label = "screenTransition",
    ) { target ->
        val isRoot = target is Screen.Active || target is Screen.Backlog || target is Screen.Discover
        if (isRoot) {
            BackHandler { /* root */ }
            MainScaffold(
                viewModel = viewModel,
                current = target,
                busy = busy,
                snackbar = snackbar,
                onNavigate = { viewModel.push(it) },
            )
        } else {
            when (val s = target) {
                is Screen.Editor -> {
                    BackHandler { viewModel.pop() }
                    EditorScreen(
                        viewModel = viewModel,
                        recipeId = s.recipeId,
                        fromSlot = s.fromSlot,
                        assignOnSave = s.assignOnSave,
                        collectionId = s.collectionId,
                        onBack = { viewModel.pop() },
                    )
                }
                is Screen.Collection -> {
                    BackHandler { viewModel.pop() }
                    CollectionScreen(
                        viewModel = viewModel,
                        collectionId = s.collectionId,
                        collectionName = s.name,
                        onBack = { viewModel.pop() },
                    )
                }
                is Screen.Diagnostics -> {
                    BackHandler { viewModel.pop() }
                    DiagnosticsScreen(onBack = { viewModel.pop() })
                }
                is Screen.DiscoverCollection -> {
                    BackHandler { viewModel.pop() }
                    DiscoverCollectionScreen(
                        viewModel = viewModel,
                        collectionId = s.id,
                        collectionName = s.name,
                        onBack = { viewModel.pop() },
                    )
                }
                is Screen.DiscoverRecipeDetail -> {
                    BackHandler { viewModel.pop() }
                    DiscoverRecipeDetailScreen(
                        viewModel = viewModel,
                        collectionId = s.collectionId,
                        recipeId = s.recipeId,
                        onBack = { viewModel.pop() },
                    )
                }
                else -> {}
            }
        }
    }
}

/** The three root tabs. */
private val tabs = listOf(
    TabSpec(Screen.Active, Icons.Filled.GridView, "Activas", Peach, PeachDeep),
    TabSpec(Screen.Backlog, Icons.Filled.CollectionsBookmark, "Biblioteca", Lavender, LavenderDeep),
    TabSpec(Screen.Discover, Icons.Filled.Explore, "Discover", com.alpefe.fujiptp.ui.theme.SoftBlue, com.alpefe.fujiptp.ui.theme.SoftBlueDeep),
)

private data class TabSpec(
    val screen: Screen,
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val label: String,
    val tint: Color,
    val deep: Color,
)

@Composable
private fun MainScaffold(
    viewModel: FujiViewModel,
    current: Screen,
    busy: Boolean,
    snackbar: SnackbarHostState,
    onNavigate: (Screen) -> Unit,
) {
    val currentTab = current as? Screen.Active ?: current as? Screen.Backlog ?: Screen.Discover

    Scaffold(
        containerColor = Canvas,
        snackbarHost = { SnackbarHost(snackbar) },
        bottomBar = {
            // Compact floating nav bar.
            NavigationBar(
                containerColor = Surface,
                tonalElevation = 0.dp,
                modifier = Modifier
                    .padding(horizontal = 20.dp, vertical = 10.dp)
                    .height(62.dp)
                    .clip(RoundedCornerShape(22.dp)),
            ) {
                tabs.forEach { tab ->
                    val selected = currentTab == tab.screen
                    NavigationBarItem(
                        selected = selected,
                        onClick = { if (!selected) onNavigate(tab.screen) },
                        icon = {
                            Icon(
                                tab.icon,
                                contentDescription = tab.label,
                                modifier = Modifier.size(20.dp),
                            )
                        },
                        label = {
                            Text(
                                tab.label,
                                style = MaterialTheme.typography.labelSmall,
                            )
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = tab.deep,
                            selectedTextColor = Ink,
                            indicatorColor = tab.tint,
                            unselectedIconColor = InkSoft,
                            unselectedTextColor = InkSoft,
                        ),
                    )
                }
            }
        },
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            AnimatedContent(
                targetState = currentTab,
                transitionSpec = {
                    (fadeIn(tween(220)) togetherWith fadeOut(tween(160)))
                },
                label = "tabSwitch",
            ) { tab ->
                when (tab) {
                    is Screen.Active -> HomeScreen(viewModel)
                    is Screen.Backlog -> BacklogScreen(viewModel)
                    else -> DiscoverScreen(viewModel)
                }
            }
            if (busy) {
                LinearProgressIndicator(
                    Modifier.fillMaxWidth(),
                    color = PeachDeep,
                    trackColor = Color.Transparent,
                )
            }
        }
    }
}
