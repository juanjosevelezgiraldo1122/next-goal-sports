package com.nextgoal.app.data.remote.dto

import com.google.gson.annotations.SerializedName

/**
 * All classes in this file map 1:1 onto the REAL response shapes returned by
 * https://api.football-data.org/v4 (free tier). Nothing here is invented data —
 * fields are nullable wherever the free tier may omit them (lineups, live "minute",
 * detailed in-match stats), and the UI layer is responsible for showing a graceful
 * fallback instead of fabricating numbers when a field is null.
 */

data class MatchesResponseDto(
    val matches: List<MatchDto>
)

data class MatchDto(
    val id: Int,
    val utcDate: String,
    val status: String, // SCHEDULED, TIMED, LIVE, IN_PLAY, PAUSED, FINISHED, POSTPONED, SUSPENDED, CANCELLED
    val matchday: Int?,
    val stage: String?,
    val group: String?,
    val minute: Int? = null, // present for LIVE/IN_PLAY matches on plans that expose it
    val venue: String? = null,
    val competition: CompetitionDto,
    val homeTeam: TeamDto,
    val awayTeam: TeamDto,
    val score: ScoreDto,
    val goals: List<GoalEventDto>? = null,
    val bookings: List<BookingEventDto>? = null,
    val substitutions: List<SubstitutionEventDto>? = null,
    val referees: List<RefereeDto>? = null
)

data class CompetitionDto(
    val id: Int,
    val name: String,
    val code: String?,
    val emblem: String?
)

data class TeamDto(
    val id: Int,
    val name: String,
    val shortName: String?,
    val tla: String?,
    val crest: String?
)

data class ScoreDto(
    val winner: String?,
    val duration: String?,
    val fullTime: ScoreLineDto,
    val halfTime: ScoreLineDto?
)

data class ScoreLineDto(
    val home: Int?,
    val away: Int?
)

data class GoalEventDto(
    val minute: Int?,
    val team: TeamRefDto?,
    val scorer: PersonRefDto?,
    val assist: PersonRefDto?,
    val type: String?
)

data class BookingEventDto(
    val minute: Int?,
    val team: TeamRefDto?,
    val player: PersonRefDto?,
    val card: String? // YELLOW_CARD, RED_CARD
)

data class SubstitutionEventDto(
    val minute: Int?,
    val team: TeamRefDto?,
    val playerOut: PersonRefDto?,
    val playerIn: PersonRefDto?
)

data class RefereeDto(
    val id: Int?,
    val name: String?,
    val type: String?
)

data class TeamRefDto(
    val id: Int?,
    val name: String?
)

data class PersonRefDto(
    val id: Int?,
    val name: String?
)

// ---- Standings ----

data class StandingsResponseDto(
    val competition: CompetitionDto,
    val standings: List<StandingGroupDto>
)

data class StandingGroupDto(
    val stage: String?,
    val type: String, // TOTAL, HOME, AWAY
    val group: String?,
    val table: List<TableRowDto>
)

data class TableRowDto(
    val position: Int,
    val team: TeamDto,
    val playedGames: Int,
    val won: Int,
    val draw: Int,
    val lost: Int,
    val points: Int,
    val goalsFor: Int,
    val goalsAgainst: Int,
    val goalDifference: Int
)

// ---- Persons / player profile ----

data class PersonDto(
    val id: Int,
    val name: String,
    val firstName: String?,
    val lastName: String?,
    val dateOfBirth: String?,
    val nationality: String?,
    val position: String?,
    val shirtNumber: Int?,
    @SerializedName("currentTeam") val currentTeam: TeamDto?
)

data class PersonMatchesResponseDto(
    val matches: List<MatchDto>,
    val resultSet: ResultSetDto?,
    val aggregations: PersonAggregationsDto? = null
)

data class PersonAggregationsDto(
    val goals: Int? = null,
    val assists: Int? = null,
    val matchesOnPitch: Int? = null,
    val minutesPlayed: Int? = null
)

data class ScorersResponseDto(
    val scorers: List<ScorerDto> = emptyList()
)

data class ScorerDto(
    val player: PersonDto,
    val team: TeamDto,
    val goals: Int? = null,
    val assists: Int? = null
)

data class ResultSetDto(
    val count: Int?
)

// ---- Teams / squad ----

data class TeamDetailDto(
    val id: Int,
    val name: String,
    val shortName: String?,
    val crest: String?,
    val squad: List<SquadMemberDto>?
)

data class TeamListResponseDto(
    val count: Int? = null,
    val teams: List<TeamDto> = emptyList()
)

data class SquadMemberDto(
    val id: Int,
    val name: String,
    val position: String?,
    val nationality: String?
)
