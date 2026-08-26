package com.alpefe.fujiptp.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Collections
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Home
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
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
import com.alpefe.fujiptp.ui.theme.Danger
import com.alpefe.fujiptp.ui.theme.Ink
import com.alpefe.fujiptp.ui.theme.InkSoft
import com.alpefe.fujiptp.ui.theme.Peach
import com.alpefe.fujiptp.ui.theme.PeachDeep
import com.alpefe.fujiptp.ui.theme.PastelGreen
import com.alpefe.fujiptp.ui.theme.PastelGreenDeep
import com.alpefe.fujiptp.ui.theme.PastelGreen
import com.alpefe.fujiptp.ui.theme.PastelGreenDeep
import com.alpefe.fujiptp.ui.theme.SoftBlue
import com.alpefe.fujiptp.ui.theme.SoftBlueDeep
import com.alpefe.fujiptp.ui.theme.Surface
import kotlinx.coroutines.delay

@Composable
fun FujiApp(viewModel: FujiViewModel = viewModel()) {
    val screen by viewModel.screen.collectAsStateWithLifecycle()
    val busyState by viewModel.busyState.collectAsStateWithLifecycle()
    val feedback by viewModel.feedback.collectAsStateWithLifecycle()
    var showSplash by remember { mutableStateOf(true) }

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

    // Loading overlay: covers the whole app with a spinner + label.
    AnimatedVisibility(
        visible = busyState != null,
        enter = fadeIn(tween(150)) + scaleIn(tween(180), initialScale = 0.95f),
        exit = fadeOut(tween(200)),
    ) {
        Box(
            Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.35f)),
            contentAlignment = Alignment.Center,
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .clip(RoundedCornerShape(28.dp))
                    .background(Surface)
                    .padding(horizontal = 32.dp, vertical = 26.dp),
            ) {
                CircularProgressIndicator(
                    color = PeachDeep,
                    strokeWidth = 4.dp,
                    modifier = Modifier.size(44.dp),
                )
                Spacer(Modifier.height(16.dp))
                Text(
                    busyState ?: "Cargando…",
                    style = MaterialTheme.typography.titleMedium,
                    color = Ink,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }
    }

    // Floating feedback banner (success/error), replaces toasts.
    FeedbackBanner(feedback)
}

@Composable
private fun FeedbackBanner(feedback: FujiViewModel.Feedback?) {
    var visibleFeedback by remember { mutableStateOf(feedback) }

    // Show the new feedback; hide it automatically after a couple of seconds.
    LaunchedEffect(feedback) {
        if (feedback != null) {
            visibleFeedback = feedback
            delay(2600)
            visibleFeedback = null
        }
    }

    // Hide when the source clears too.
    LaunchedEffect(feedback) {
        if (feedback == null) {
            visibleFeedback = null
        }
    }

    val current = visibleFeedback ?: return
    val isError = current.isError
    val accent = if (isError) Danger else PastelGreenDeep
    val icon = if (isError) Icons.Filled.Error else Icons.Filled.CheckCircle

    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.BottomCenter) {
        AnimatedVisibility(
            visible = visibleFeedback != null,
            enter = slideInVertically(tween(280)) { it } + fadeIn(tween(220)),
            exit = slideOutVertically(tween(220)) { it } + fadeOut(tween(180)),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .navigationBarsPadding()
                    .padding(bottom = 16.dp, start = 20.dp, end = 20.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(Surface.copy(alpha = 0.97f))
                    .padding(horizontal = 18.dp, vertical = 12.dp),
            ) {
                Icon(
                    icon,
                    contentDescription = null,
                    tint = accent,
                    modifier = Modifier.size(22.dp),
                )
                Spacer(Modifier.width(10.dp))
                Text(
                    current.message,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Ink,
                )
            }
        }
    }
}

@Composable
private fun MainScaffold(
    viewModel: FujiViewModel,
    current: Screen,
    onNavigate: (Screen) -> Unit,
) {
    val currentTab = current as? Screen.Active ?: current as? Screen.Backlog ?: Screen.Discover

    Scaffold(
        containerColor = Canvas,
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
        }
    }
}

private data class Tab(
    val screen: Screen,
    val label: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val tint: Color,
    val deep: Color,
)

private val tabs = listOf(
    Tab(Screen.Active, "Activas", Icons.Filled.Home, Peach.copy(alpha = 0.25f), PeachDeep),
    Tab(Screen.Backlog, "Biblioteca", Icons.Filled.Collections, SoftBlue.copy(alpha = 0.25f), SoftBlueDeep),
    Tab(Screen.Discover, "Discover", Icons.Filled.Explore, PastelGreen.copy(alpha = 0.25f), PastelGreenDeep),
)
