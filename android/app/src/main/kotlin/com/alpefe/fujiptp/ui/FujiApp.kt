package com.alpefe.fujiptp.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CollectionsBookmark
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Surface
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.alpefe.fujiptp.ui.editor.EditorScreen
import com.alpefe.fujiptp.ui.home.HomeScreen
import com.alpefe.fujiptp.ui.home.backlog.BacklogScreen
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.receiveAsFlow

@Composable
fun FujiApp(viewModel: FujiViewModel = viewModel()) {
    val screen by viewModel.screen.collectAsStateWithLifecycle()
    val busy by viewModel.busy.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        viewModel.messages.receiveAsFlow().collectLatest { snackbar.showSnackbar(it) }
    }

    // Editor is a full-screen destination; the two main tabs live in a scaffold.
    when (val s = screen) {
        is Screen.Editor -> {
            BackHandler { viewModel.pop() }
            EditorScreen(
                viewModel = viewModel,
                recipeId = s.recipeId,
                fromSlot = s.fromSlot,
                assignOnSave = s.assignOnSave,
                onBack = { viewModel.pop() },
            )
        }
        else -> {
            BackHandler { /* root: nothing to pop */ }
            MainScaffold(
                viewModel = viewModel,
                current = screen,
                busy = busy,
                snackbar = snackbar,
                onNavigate = { viewModel.push(it) },
            )
        }
    }
}

@Composable
private fun MainScaffold(
    viewModel: FujiViewModel,
    current: Screen,
    busy: Boolean,
    snackbar: SnackbarHostState,
    onNavigate: (Screen) -> Unit,
) {
    val isBacklog = current is Screen.Backlog
    Scaffold(
        snackbarHost = { SnackbarHost(snackbar) },
        bottomBar = {
            NavigationBar(containerColor = MaterialTheme.colorScheme.surface) {
                NavigationBarItem(
                    selected = !isBacklog,
                    onClick = { if (isBacklog) onNavigate(Screen.Active) },
                    icon = { Icon(Icons.Filled.GridView, contentDescription = "Activas") },
                    label = { Text("Activas") },
                )
                NavigationBarItem(
                    selected = isBacklog,
                    onClick = { if (!isBacklog) onNavigate(Screen.Backlog) },
                    icon = { Icon(Icons.Filled.CollectionsBookmark, contentDescription = "Biblioteca") },
                    label = { Text("Biblioteca") },
                )
            }
        },
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            if (isBacklog) {
                BacklogScreen(viewModel)
            } else {
                HomeScreen(viewModel)
            }
            if (busy) {
                LinearProgressIndicator(
                    Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = Color.Transparent,
                )
            }
        }
    }
}
