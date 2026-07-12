package me.gorani.launchingservice.sample

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import me.gorani.launchingservice.AppUpdateStatus

@Composable
fun LaunchingScreen(
  uiState: LaunchingUiState,
  onRefresh: () -> Unit,
) {
  Surface(modifier = Modifier.fillMaxSize()) {
    Column(
      modifier = Modifier
        .fillMaxSize()
        .padding(24.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
    ) {
      when (uiState) {
        LaunchingUiState.Loading -> CircularProgressIndicator()
        is LaunchingUiState.Error -> {
          Text(text = uiState.message, style = MaterialTheme.typography.bodyLarge)
          Button(onClick = onRefresh) { Text("Retry") }
        }
        is LaunchingUiState.Ready -> {
          Text(
            text = uiState.status.displayText(),
            style = MaterialTheme.typography.headlineSmall,
          )
          Button(onClick = onRefresh) { Text("Refresh") }
        }
      }
    }
  }
}

private fun AppUpdateStatus.displayText(): String = when (this) {
  AppUpdateStatus.Valid -> "Valid"
  is AppUpdateStatus.ForcedUpdateRequired -> "Forced update: ${alert.title}"
  is AppUpdateStatus.OptionalUpdateRequired -> "Optional update: ${alert.title}"
  is AppUpdateStatus.Notice -> "Notice: ${alert.title}"
}
