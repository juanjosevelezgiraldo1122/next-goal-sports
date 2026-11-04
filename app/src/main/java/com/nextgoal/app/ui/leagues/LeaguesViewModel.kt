package com.nextgoal.app.ui.leagues

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nextgoal.app.data.model.CompetitionOption
import com.nextgoal.app.data.model.StandingRowUi
import com.nextgoal.app.data.repository.ApiResult
import com.nextgoal.app.data.repository.FEATURED_COMPETITIONS
import com.nextgoal.app.data.repository.FootballRepository
import com.nextgoal.app.ui.common.UiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class LeaguesViewModel(
    private val repository: FootballRepository,
    initialCompetitionCode: String = FEATURED_COMPETITIONS.first().code
) : ViewModel() {

    val competitions: List<CompetitionOption> = FEATURED_COMPETITIONS

    private val _selected = MutableStateFlow(initialCompetitionCode)
    val selected: StateFlow<String> = _selected

    private val _state = MutableStateFlow<UiState<List<StandingRowUi>>>(UiState.Loading)
    val state: StateFlow<UiState<List<StandingRowUi>>> = _state

    init {
        load(initialCompetitionCode)
    }

    fun selectCompetition(code: String) {
        if (code == _selected.value) return
        _selected.value = code
        load(code)
    }

    private fun load(code: String) {
        _state.value = UiState.Loading
        viewModelScope.launch {
            when (val result = repository.getStandings(code)) {
                is ApiResult.Success -> _state.value = UiState.Success(result.data)
                is ApiResult.Error -> _state.value = UiState.Error(result.message)
            }
        }
    }
}
