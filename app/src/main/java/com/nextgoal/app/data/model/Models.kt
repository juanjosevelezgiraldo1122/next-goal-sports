package com.nextgoal.app.data.model

enum class MatchStatus { SCHEDULED, LIVE, PAUSED, FINISHED, POSTPONED, OTHER }

data class TeamUi(
    val id: Int,
    val name: String,
    val shortName: String,
    val crestUrl: String?
)

data class MatchUi(
    val id: Int,
    val competitionId: Int,
    val competitionName: String,
    val competitionEmblem: String?,
    val stage: String?,
    val matchday: Int?,
    val status: MatchStatus,
    val minute: Int?,
    val utcDate: String,
    val homeTeam: TeamUi,
    val awayTeam: TeamUi,
    val homeScore: Int?,
    val awayScore: Int?,
    val venue: String?
)

data class MatchEventUi(
    val minute: Int?,
    val isHomeTeam: Boolean,
    val label: String,
    val subLabel: String?,
    val kind: EventKind
)

enum class EventKind { GOAL, YELLOW_CARD, RED_CARD, SUBSTITUTION }

data class MatchDetailUi(
    val match: MatchUi,
    val events: List<MatchEventUi>,
    val statsAvailable: Boolean,
    val lineupsAvailable: Boolean
)

data class StandingRowUi(
    val position: Int,
    val team: TeamUi,
    val played: Int,
    val goalDifference: Int,
    val points: Int,
    val won: Int,
    val draw: Int,
    val lost: Int,
    val goalsFor: Int
)

data class CompetitionOption(
    val code: String,
    val displayName: String
)

data class PlayerUi(
    val id: Int,
    val name: String,
    val position: String?,
    val shirtNumber: Int?,
    val nationality: String?,
    val teamName: String?,
    val teamCrestUrl: String?,
    val currentTeamId: Int?,
    val goals: Int?,
    val assists: Int?,
    val recentMatches: List<PlayerMatchUi>
)

data class PlayerMatchUi(
    val opponent: String,
    val competitionName: String,
    val homeScore: Int?,
    val awayScore: Int?,
    val wasHome: Boolean,
    val utcDate: String
)

data class NewsArticleUi(
    val id: Int,
    val category: String,
    val title: String,
    val summary: String,
    val timeAgo: String,
    val imageUrl: String?,
    val isFeatured: Boolean = false
)
