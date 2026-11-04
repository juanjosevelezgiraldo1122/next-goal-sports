package com.nextgoal.app.data.repository

import com.nextgoal.app.data.model.CompetitionOption
import com.nextgoal.app.data.model.MatchDetailUi
import com.nextgoal.app.data.model.MatchStatus
import com.nextgoal.app.data.model.MatchUi
import com.nextgoal.app.data.model.PlayerMatchUi
import com.nextgoal.app.data.model.PlayerUi
import com.nextgoal.app.data.model.StandingRowUi
import com.nextgoal.app.data.remote.FootballApiService
import com.nextgoal.app.data.remote.dto.TeamDto
import com.nextgoal.app.data.remote.dto.ScorerDto
import java.time.Instant
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import java.util.TimeZone

/**
 * Leagues covered by football-data.org's FREE tier. Liga MX is intentionally NOT
 * listed here: football-data.org does not provide Liga MX in its free plan, so we
 * only surface leagues we can back with real, live data. Swap/add codes here if you
 * upgrade your plan or plug in a different provider.
 */
val FEATURED_COMPETITIONS = listOf(
    CompetitionOption("CL", "Champions League"),
    CompetitionOption("PD", "La Liga"),
    CompetitionOption("PL", "Premier League"),
    CompetitionOption("BL1", "Bundesliga"),
    CompetitionOption("SA", "Serie A"),
    CompetitionOption("FL1", "Ligue 1"),
)

sealed class ApiResult<out T> {
    data class Success<T>(val data: T) : ApiResult<T>()
    data class Error(val message: String) : ApiResult<Nothing>()
}

class FootballRepository(private val api: FootballApiService) {

    /** Recent results, live matches, and the next six days across competitions covered by the account. */
    suspend fun getHomeFeed(): ApiResult<List<MatchUi>> {
        val today = LocalDate.now(TimeZone.getDefault().toZoneId())
        return getMatchesInRange(today.minusDays(3), today.plusDays(6))
    }

    /** football-data.org limits date windows; callers must keep each request below ten days. */
    suspend fun getMatchesInRange(from: LocalDate, to: LocalDate): ApiResult<List<MatchUi>> = safeCall {
        require(!to.isBefore(from)) { "El rango de fechas no es válido." }
        require(ChronoUnit.DAYS.between(from, to) < 10) { "Consulta un periodo máximo de diez días." }
        api.getMatchesToday(from.toString(), to.toString()).matches
            .map { it.toUi() }
            .sortedWith(
                compareBy<MatchUi>({
                    when (it.status) {
                        MatchStatus.LIVE, MatchStatus.PAUSED -> 0
                        MatchStatus.SCHEDULED -> 1
                        MatchStatus.FINISHED -> 2
                        else -> 3
                    }
                }, { it.utcDate })
            )
    }

    suspend fun getMatchDetail(matchId: Int): ApiResult<MatchDetailUi> = safeCall {
        api.getMatchById(matchId).toDetailUi()
    }

    suspend fun getStandings(competitionCode: String): ApiResult<List<StandingRowUi>> = safeCall {
        val response = api.getStandings(competitionCode)
        val totalTable = response.standings.firstOrNull { it.type == "TOTAL" } ?: response.standings.first()
        totalTable.table.map { it.toUi() }
    }

    suspend fun getTeamSquad(teamId: Int) = safeCall {
        api.getTeam(teamId)
    }

    suspend fun getCompetitionTeams(code: String): ApiResult<List<TeamDto>> = safeCall {
        api.getCompetitionTeams(code).teams
    }

    suspend fun getCompetitionScorers(code: String): ApiResult<List<ScorerDto>> = safeCall {
        api.getCompetitionScorers(code, limit = 100).scorers
    }

    suspend fun getNextTeamMatch(teamId: Int): ApiResult<MatchUi?> = safeCall {
        val now = Instant.now()
        api.getTeamMatches(teamId, limit = 100).matches
            .map { it.toUi() }
            .filter { it.status == MatchStatus.SCHEDULED && runCatching { Instant.parse(it.utcDate).isAfter(now) }.getOrDefault(false) }
            .minByOrNull { it.utcDate }
    }

    suspend fun getPlayerProfile(personId: Int): ApiResult<PlayerUi> = safeCall {
        coroutineScope {
            val personDeferred = async { api.getPerson(personId) }
            val matchesDeferred = async { runCatching { api.getPersonMatches(personId, limit = 100) }.getOrNull() }

            val person = personDeferred.await()
            val matchesResponse = matchesDeferred.await()

            val recentMatches: List<PlayerMatchUi> = matchesResponse?.matches
                ?.sortedByDescending { it.utcDate }
                ?.map { it.toPlayerMatchUi(person.currentTeam?.id) }
                ?: emptyList()

            PlayerUi(
                id = person.id,
                name = person.name,
                position = person.position,
                shirtNumber = person.shirtNumber,
                nationality = person.nationality,
                teamName = person.currentTeam?.name,
                teamCrestUrl = person.currentTeam?.crest,
                currentTeamId = person.currentTeam?.id,
                goals = matchesResponse?.aggregations?.goals,
                assists = matchesResponse?.aggregations?.assists,
                recentMatches = recentMatches
            )
        }
    }

    private inline fun <T> safeCallBlocking(block: () -> T): ApiResult<T> =
        try {
            ApiResult.Success(block())
        } catch (e: Exception) {
            ApiResult.Error(describeError(e))
        }

    private suspend inline fun <T> safeCall(crossinline block: suspend () -> T): ApiResult<T> =
        try {
            ApiResult.Success(block())
        } catch (e: Exception) {
            ApiResult.Error(describeError(e))
        }

    private fun describeError(e: Exception): String = when {
        e.message?.contains("401") == true ->
            "La API no aceptó la clave configurada. Revisa FOOTBALL_DATA_API_KEY en local.properties."
        e.message?.contains("403") == true ->
            "Tu cuenta de datos no tiene acceso a esta competición o recurso."
        e.message?.contains("429") == true ->
            "Se alcanzó el límite de peticiones gratuitas de la API. Espera un minuto e inténtalo de nuevo."
        e.message?.contains("404") == true ->
            "No se encontró ese recurso en la API."
        else -> "No se pudo conectar. Revisa tu conexión a internet. (${e.message ?: "error desconocido"})"
    }
}
