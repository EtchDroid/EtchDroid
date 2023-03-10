package eu.depau.etchdroid.ui.composables

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import eu.depau.etchdroid.ui.ThemeViewModel
import eu.depau.etchdroid.ui.theme.EtchDroidTheme


@Composable
fun MainView(viewModel: ThemeViewModel<*>, content: @Composable () -> Unit) {
    val uiState by viewModel.state.collectAsState()
    val darkMode by viewModel.darkMode

    EtchDroidTheme(
        darkTheme = darkMode, dynamicColor = uiState.dynamicColors
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background,
            content = content
        )
    }
}
