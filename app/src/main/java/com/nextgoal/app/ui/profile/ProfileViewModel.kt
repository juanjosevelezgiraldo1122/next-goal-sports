package com.nextgoal.app.ui.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nextgoal.app.data.model.PlayerUi
import com.nextgoal.app.data.remote.dto.SquadMemberDto
import com.nextgoal.app.data.repository.ApiResult
import com.nextgoal.app.data.repository.FootballRepository
import com.nextgoal.app.ui.common.UiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class ProfileViewModel(
    private val repository: FootballRepository,
    private val defaultTeamId: Int
) : ViewModel() {

    private val _teamName = MutableStateFlow<String?>(null)
    val teamName: StateFlow<String?> = _teamName

    private val _squadState = MutableStateFlow<UiState<List<SquadMemberDto>>>(UiState.Loading)
    val squadState: StateFlow<UiState<List<SquadMemberDto>>> = _squadState

    private val _selectedPlayerId = MutableStateFlow<Int?>(null)
    val selectedPlayerId: StateFlow<Int?> = _selectedPlayerId

    private val _playerState = MutableStateFlow<UiState<PlayerUi>?>(null)
    val playerState: StateFlow<UiState<PlayerUi>?> = _playerState

    init {
        loadSquad()
    }

    fun loadSquad() {
        _squadState.value = UiState.Loading
        viewModelScope.launch {
            when (val result = repository.getTeamSquad(defaultTeamId)) {
                is ApiResult.Success -> {
                    _teamName.value = result.data.name
                    _squadState.value = UiState.Success(result.data.squad.orEmpty())
                }
                is ApiResult.Error -> _squadState.value = UiState.Error(result.message)
            }
        }
    }

    fun selectPlayer(playerId: Int) {
        _selectedPlayerId.value = playerId
        _playerState.value = UiState.Loading
        viewModelScope.launch {
            when (val result = repository.getPlayerProfile(playerId)) {
                is ApiResult.Success -> _playerState.value = UiState.Success(result.data)
                is ApiResult.Error -> _playerState.value = UiState.Error(result.message)
            }
        }
    }

    fun clearSelection() {
        _selectedPlayerId.value = null
        _playerState.value = null
    }
}
