package com.nextgoal.app.ui.matchdetail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nextgoal.app.data.model.MatchDetailUi
import com.nextgoal.app.data.repository.ApiResult
import com.nextgoal.app.data.repository.FootballRepository
import com.nextgoal.app.ui.common.UiState
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class MatchDetailViewModel(
    private val repository: FootballRepository,
    private val matchId: Int
) : ViewModel() {

    private val _state = MutableStateFlow<UiState<MatchDetailUi>>(UiState.Loading)
    val state: StateFlow<UiState<MatchDetailUi>> = _state

    init {
        viewModelScope.launch {
            while (true) {
                fetchOnce()
                delay(30_000)
            }
        }
    }

    private suspend fun fetchOnce() {
        when (val result = repository.getMatchDetail(matchId)) {
            is ApiResult.Success -> _state.value = UiState.Success(result.data)
            is ApiResult.Error -> if (_state.value !is UiState.Success) _state.value = UiState.Error(result.message)
        }
    }
}
