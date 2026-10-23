package me.gorani.launchingservice.sample

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import me.gorani.launchingservice.LaunchingServiceClient

class LaunchingViewModel(
  private val launchingService: LaunchingServiceClient,
) : ViewModel() {
  private val mutableUiState = MutableStateFlow<LaunchingUiState>(LaunchingUiState.Loading)
  private var refreshJob: Job? = null
  val uiState: StateFlow<LaunchingUiState> = mutableUiState.asStateFlow()

  init {
    refresh()
  }

  fun refresh() {
    refreshJob?.cancel()
    mutableUiState.value = LaunchingUiState.Loading
    refreshJob = viewModelScope.launch {
      mutableUiState.value = try {
        LaunchingUiState.Ready(launchingService.fetchAppUpdateStatus())
      } catch (error: CancellationException) {
        throw error
      } catch (error: Exception) {
        LaunchingUiState.Error(error.message ?: "Launching status could not be loaded.")
      }
    }
  }

  companion object {
    fun factory(launchingService: LaunchingServiceClient): ViewModelProvider.Factory =
      object : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
          require(modelClass.isAssignableFrom(LaunchingViewModel::class.java))
          return LaunchingViewModel(launchingService) as T
        }
      }
  }
}
