package com.nextgoal.app.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nextgoal.app.data.model.MatchUi
import com.nextgoal.app.data.repository.ApiResult
import com.nextgoal.app.data.repository.FootballRepository
import com.nextgoal.app.ui.common.UiState
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class HomeViewModel(private val repository: FootballRepository) : ViewModel() {

    private val _state = MutableStateFlow<UiState<List<MatchUi>>>(UiState.Loading)
    val state: StateFlow<UiState<List<MatchUi>>> = _state

    init {
        startPolling()
    }

    fun refresh() {
        viewModelScope.launch { fetchOnce() }
    }

    /** Live matches change minute by minute, so we re-fetch every 45s while this screen is alive. */
    private fun startPolling() {
        viewModelScope.launch {
            while (true) {
                fetchOnce()
                delay(45_000)
            }
        }
    }

    private suspend fun fetchOnce() {
        when (val result = repository.getHomeFeed()) {
            is ApiResult.Success -> _state.value = UiState.Success(result.data)
            is ApiResult.Error -> _state.value = UiState.Error(result.message)
        }
    }
}
