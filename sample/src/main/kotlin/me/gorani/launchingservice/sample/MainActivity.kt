package me.gorani.launchingservice.sample

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.material3.MaterialTheme
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.getValue
import me.gorani.launchingservice.LaunchingService

class MainActivity : ComponentActivity() {
  private val viewModel: LaunchingViewModel by viewModels {
    LaunchingViewModel.factory(LaunchingService(applicationContext))
  }

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContent {
      MaterialTheme {
        val uiState by viewModel.uiState.collectAsStateWithLifecycle()
        LaunchingScreen(
          uiState = uiState,
          onRefresh = viewModel::refresh,
        )
      }
    }
  }
}
