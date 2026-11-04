package com.nextgoal.app.data.remote

import com.nextgoal.app.data.remote.dto.MatchDto
import com.nextgoal.app.data.remote.dto.MatchesResponseDto
import com.nextgoal.app.data.remote.dto.PersonDto
import com.nextgoal.app.data.remote.dto.PersonMatchesResponseDto
import com.nextgoal.app.data.remote.dto.StandingsResponseDto
import com.nextgoal.app.data.remote.dto.TeamDetailDto
import com.nextgoal.app.data.remote.dto.TeamListResponseDto
import com.nextgoal.app.data.remote.dto.ScorersResponseDto
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

/**
 * Real, live endpoints of https://api.football-data.org/v4
 * Docs: https://docs.football-data.org/general/v4/index.html
 *
 * Auth is injected automatically by [AuthInterceptor] using the key configured
 * in local.properties -> BuildConfig.FOOTBALL_DATA_API_KEY, so callers never pass a token.
 */
interface FootballApiService {

    /** Every match (any status) happening today across every competition the API key covers. */
    @GET("v4/matches")
    suspend fun getMatchesToday(
        @Query("dateFrom") dateFrom: String,
        @Query("dateTo") dateTo: String
    ): MatchesResponseDto

    /** Matches for one competition (e.g. "PD" = La Liga, "PL" = Premier League, "CL" = Champions League). */
    @GET("v4/competitions/{code}/matches")
    suspend fun getCompetitionMatches(
        @Path("code") competitionCode: String,
        @Query("dateFrom") dateFrom: String? = null,
        @Query("dateTo") dateTo: String? = null,
        @Query("status") status: String? = null
    ): MatchesResponseDto

    @GET("v4/matches/{id}")
    suspend fun getMatchById(@Path("id") matchId: Int): MatchDto

    @GET("v4/competitions/{code}/standings")
    suspend fun getStandings(@Path("code") competitionCode: String): StandingsResponseDto

    @GET("v4/competitions/{code}/teams")
    suspend fun getCompetitionTeams(@Path("code") competitionCode: String): TeamListResponseDto

    @GET("v4/persons/{id}")
    suspend fun getPerson(@Path("id") personId: Int): PersonDto

    @GET("v4/persons/{id}/matches")
    suspend fun getPersonMatches(
        @Path("id") personId: Int,
        @Query("limit") limit: Int = 10
    ): PersonMatchesResponseDto

    @GET("v4/competitions/{code}/scorers")
    suspend fun getCompetitionScorers(
        @Path("code") competitionCode: String,
        @Query("limit") limit: Int = 100
    ): ScorersResponseDto

    @GET("v4/teams/{id}/matches")
    suspend fun getTeamMatches(
        @Path("id") teamId: Int,
        @Query("limit") limit: Int = 100
    ): MatchesResponseDto

    @GET("v4/teams/{id}")
    suspend fun getTeam(@Path("id") teamId: Int): TeamDetailDto
}
