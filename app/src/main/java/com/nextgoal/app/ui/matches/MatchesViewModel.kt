package com.nextgoal.app.ui.matches

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nextgoal.app.data.model.MatchUi
import com.nextgoal.app.data.repository.ApiResult
import com.nextgoal.app.data.repository.FootballRepository
import com.nextgoal.app.ui.common.UiState
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.time.LocalDate

data class MatchesUiState(
    val result: UiState<List<MatchUi>> = UiState.Loading,
    val from: LocalDate = LocalDate.now().minusDays(3),
    val to: LocalDate = LocalDate.now().plusDays(6)
)

class MatchesViewModel(private val repository: FootballRepository) : ViewModel() {
    private val _state = MutableStateFlow(MatchesUiState())
    val state: StateFlow<MatchesUiState> = _state
    private var request: Job? = null

    fun loadRange(from: LocalDate, to: LocalDate, force: Boolean = false) {
        if (!force && _state.value.from == from && _state.value.to == to && _state.value.result !is UiState.Loading) return
        request?.cancel()
        _state.value = MatchesUiState(result = UiState.Loading, from = from, to = to)
        request = viewModelScope.launch {
            _state.value = when (val response = repository.getMatchesInRange(from, to)) {
                is ApiResult.Success -> _state.value.copy(result = UiState.Success(response.data))
                is ApiResult.Error -> _state.value.copy(result = UiState.Error(response.message))
            }
        }
    }

    fun refresh() = loadRange(_state.value.from, _state.value.to, force = true)
}
