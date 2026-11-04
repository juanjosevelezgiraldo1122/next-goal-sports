package com.nextgoal.app.ui.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nextgoal.app.data.model.CompetitionOption
import com.nextgoal.app.data.model.MatchUi
import com.nextgoal.app.data.model.PlayerUi
import com.nextgoal.app.data.remote.dto.ScorerDto
import com.nextgoal.app.data.remote.dto.SquadMemberDto
import com.nextgoal.app.data.remote.dto.TeamDto
import com.nextgoal.app.data.repository.ApiResult
import com.nextgoal.app.data.repository.FootballRepository
import com.nextgoal.app.data.repository.FEATURED_COMPETITIONS
import com.nextgoal.app.ui.common.UiState
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class TeamSearchDetails(
    val team: TeamDto,
    val squad: List<SquadMemberDto>,
    val goals: Int?,
    val assists: Int?,
    val nextMatch: MatchUi?
)

data class PlayerSearchDetails(val player: PlayerUi, val nextMatch: MatchUi?, val competitionName: String)

class SearchViewModel(private val repository: FootballRepository) : ViewModel() {
    private val _competition = MutableStateFlow(FEATURED_COMPETITIONS.first())
    val competition: StateFlow<CompetitionOption> = _competition
    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query
    private val _teams = MutableStateFlow<UiState<List<TeamDto>>>(UiState.Loading)
    val teams: StateFlow<UiState<List<TeamDto>>> = _teams
    private val _scorers = MutableStateFlow<UiState<List<ScorerDto>>>(UiState.Loading)
    val scorers: StateFlow<UiState<List<ScorerDto>>> = _scorers
    private val _teamDetails = MutableStateFlow<UiState<TeamSearchDetails>?>(null)
    val teamDetails: StateFlow<UiState<TeamSearchDetails>?> = _teamDetails
    private val _playerDetails = MutableStateFlow<UiState<PlayerSearchDetails>?>(null)
    val playerDetails: StateFlow<UiState<PlayerSearchDetails>?> = _playerDetails

    init { loadCompetitionData() }

    fun setQuery(value: String) { _query.value = value }

    fun selectCompetition(value: CompetitionOption) {
        if (_competition.value.code == value.code) return
        _competition.value = value
        _teamDetails.value = null
        _playerDetails.value = null
        loadCompetitionData()
    }

    fun openTeam(team: TeamDto) {
        _query.value = ""
        _teamDetails.value = UiState.Loading
        _playerDetails.value = null
        viewModelScope.launch {
            coroutineScope {
                val squadCall = async { repository.getTeamSquad(team.id) }
                val nextCall = async { repository.getNextTeamMatch(team.id) }
                val standingsCall = async { repository.getStandings(_competition.value.code) }
                val squadResult = squadCall.await()
                val nextMatch = (nextCall.await() as? ApiResult.Success)?.data
                val standings = (standingsCall.await() as? ApiResult.Success)?.data.orEmpty()
                when (squadResult) {
                    is ApiResult.Success -> {
                        val scorers = (_scorers.value as? UiState.Success)?.data.orEmpty().filter { it.team.id == team.id }
                        val standing = standings.firstOrNull { it.team.id == team.id }
                        _teamDetails.value = UiState.Success(
                            TeamSearchDetails(
                                team = team,
                                squad = squadResult.data.squad.orEmpty(),
                                goals = standing?.goalsFor,
                                assists = scorers.sumOf { it.assists ?: 0 }.takeIf { scorers.isNotEmpty() },
                                nextMatch = nextMatch
                            )
                        )
                    }
                    is ApiResult.Error -> _teamDetails.value = UiState.Error(squadResult.message)
                }
            }
        }
    }

    fun openPlayer(playerId: Int, squadMember: SquadMemberDto? = null) {
        _query.value = ""
        val rosterTeam = (_teamDetails.value as? UiState.Success)?.data?.team
        _playerDetails.value = UiState.Loading
        _teamDetails.value = null
        viewModelScope.launch {
            val scorer = (_scorers.value as? UiState.Success)?.data?.firstOrNull { it.player.id == playerId }
            val playerData = when {
                scorer != null -> com.nextgoal.app.data.model.PlayerUi(
                    id = scorer.player.id,
                    name = scorer.player.name,
                    position = scorer.player.position,
                    shirtNumber = scorer.player.shirtNumber,
                    nationality = scorer.player.nationality,
                    teamName = scorer.team.name,
                    teamCrestUrl = scorer.team.crest,
                    currentTeamId = scorer.team.id,
                    goals = scorer.goals,
                    assists = scorer.assists,
                    recentMatches = emptyList()
                )
                else -> when (val profile = repository.getPlayerProfile(playerId)) {
                    is ApiResult.Success -> profile.data
                    is ApiResult.Error -> if (squadMember != null && rosterTeam != null) {
                        com.nextgoal.app.data.model.PlayerUi(
                            id = squadMember.id,
                            name = squadMember.name,
                            position = squadMember.position,
                            shirtNumber = null,
                            nationality = squadMember.nationality,
                            teamName = rosterTeam.name,
                            teamCrestUrl = rosterTeam.crest,
                            currentTeamId = rosterTeam.id,
                            goals = null,
                            assists = null,
                            recentMatches = emptyList()
                        )
                    } else {
                        _playerDetails.value = UiState.Error(profile.message)
                        return@launch
                    }
                }
            }
            val next = playerData.currentTeamId?.let { id ->
                (repository.getNextTeamMatch(id) as? ApiResult.Success)?.data
            }
            _playerDetails.value = UiState.Success(PlayerSearchDetails(playerData, next, _competition.value.displayName))
        }
    }

    fun closeDetails() { _teamDetails.value = null; _playerDetails.value = null }

    private fun loadCompetitionData() {
        _teams.value = UiState.Loading
        _scorers.value = UiState.Loading
        val code = _competition.value.code
        viewModelScope.launch {
            coroutineScope {
                val teamsCall = async { repository.getCompetitionTeams(code) }
                val scorersCall = async { repository.getCompetitionScorers(code) }
                _teams.value = teamsCall.await().toUiState()
                _scorers.value = scorersCall.await().toUiState()
            }
        }
    }

    private fun <T> ApiResult<T>.toUiState(): UiState<T> = when (this) {
        is ApiResult.Success -> UiState.Success(data)
        is ApiResult.Error -> UiState.Error(message)
    }
}
