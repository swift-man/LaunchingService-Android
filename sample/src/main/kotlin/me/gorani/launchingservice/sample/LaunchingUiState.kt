package me.gorani.launchingservice.sample

import me.gorani.launchingservice.AppUpdateStatus

sealed interface LaunchingUiState {
  data object Loading : LaunchingUiState
  data class Ready(val status: AppUpdateStatus) : LaunchingUiState
  data class Error(val message: String) : LaunchingUiState
}
