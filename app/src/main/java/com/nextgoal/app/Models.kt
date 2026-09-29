package com.nextgoal.app

enum class MatchPhase {
    LIVE,
    UPCOMING,
    FINISHED
}

data class Team(
    val id: Int,
    val name: String,
    val shortName: String = name,
    val crestUrl: String? = null
)

data class Match(
    val id: Int,
    val competition: String,
    val competitionShort: String,
    val competitionCode: String = "",
    val home: Team,
    val away: Team,
    val homeScore: Int? = null,
    val awayScore: Int? = null,
    val minute: Int? = null,
    val kickoffLabel: String = "20:00",
    val phase: MatchPhase = MatchPhase.UPCOMING,
    val venue: String = "Estadio por confirmar",
    val kickoffMillis: Long = 0L,
    val stats: MatchStats? = null,
    val events: List<MatchEvent> = emptyList(),
    val externalEventId: String? = null
)

data class MatchStats(
    val homePossession: Int? = null,
    val awayPossession: Int? = null,
    val homeShotsOnTarget: Int? = null,
    val awayShotsOnTarget: Int? = null,
    val homeShotsOffTarget: Int? = null,
    val awayShotsOffTarget: Int? = null,
    val homeShots: Int? = null,
    val awayShots: Int? = null,
    val homeBlockedShots: Int? = null,
    val awayBlockedShots: Int? = null,
    val homeCorners: Int? = null,
    val awayCorners: Int? = null,
    val homeFouls: Int? = null,
    val awayFouls: Int? = null,
    val homeOffsides: Int? = null,
    val awayOffsides: Int? = null,
    val homeFreeKicks: Int? = null,
    val awayFreeKicks: Int? = null,
    val homeGoalKicks: Int? = null,
    val awayGoalKicks: Int? = null,
    val homeSaves: Int? = null,
    val awaySaves: Int? = null,
    val homeThrowIns: Int? = null,
    val awayThrowIns: Int? = null,
    val homeYellowCards: Int? = null,
    val awayYellowCards: Int? = null,
    val homeRedCards: Int? = null,
    val awayRedCards: Int? = null
)

data class MatchEvent(
    val minute: Int? = null,
    val kind: String,
    val title: String,
    val subtitle: String,
    val teamName: String
)

data class StandingRow(
    val position: Int,
    val team: Team,
    val played: Int,
    val goalDifference: Int,
    val points: Int,
    val form: List<String> = emptyList()
)

data class NewsItem(
    val category: String,
    val title: String,
    val summary: String,
    val timeLabel: String,
    val imageUrl: String,
    val sourceUrl: String? = null
)

data class DashboardData(
    val matches: List<Match>,
    val standings: Map<String, List<StandingRow>>,
    val news: List<NewsItem>
)

enum class DirectoryEntityType {
    PLAYER,
    TEAM
}

data class DirectoryResult(
    val id: String,
    val type: DirectoryEntityType,
    val name: String,
    val subtitle: String,
    val imageUrl: String? = null
)

data class DirectoryMatchStat(
    val label: String,
    val homeValue: String,
    val awayValue: String
)

data class DirectoryFixture(
    val id: String,
    val competition: String,
    val homeTeam: String,
    val awayTeam: String,
    val homeLogoUrl: String? = null,
    val awayLogoUrl: String? = null,
    val kickoffLabel: String,
    val statusLabel: String,
    val homeScore: Int? = null,
    val awayScore: Int? = null,
    val venue: String = "Estadio no publicado",
    val stats: List<DirectoryMatchStat> = emptyList()
)

sealed interface DirectoryDetails {
    data class Player(
        val id: String,
        val name: String,
        val team: String,
        val position: String,
        val nationality: String,
        val birthDate: String,
        val birthPlace: String,
        val height: String,
        val weight: String,
        val photoUrl: String?,
        val teamBadgeUrl: String?,
        val description: String,
        val currentTeamId: String? = null,
        val nextMatch: DirectoryFixture? = null,
        val lastMatch: DirectoryFixture? = null
    ) : DirectoryDetails

    data class Team(
        val id: String,
        val name: String,
        val league: String,
        val country: String,
        val founded: String,
        val stadium: String,
        val stadiumCapacity: String,
        val logoUrl: String?,
        val bannerUrl: String?,
        val description: String,
        val website: String?,
        val nextMatch: DirectoryFixture? = null,
        val lastMatch: DirectoryFixture? = null
    ) : DirectoryDetails
}

data class DirectoryState(
    val query: String = "",
    val isSearching: Boolean = false,
    val results: List<DirectoryResult> = emptyList(),
    val selected: DirectoryDetails? = null,
    val isLoadingDetails: Boolean = false,
    val errorMessage: String? = null
)

data class UserProfile(
    val name: String,
    val email: String,
    val favoriteTeam: String,
    val notificationsEnabled: Boolean,
    val phoneNumber: String = "",
    val photoUri: String? = null
)

enum class VerificationKind {
    EMAIL,
    PHONE,
    PASSWORD
}

data class VerificationRequest(
    val kind: VerificationKind,
    val destination: String,
    val code: String,
    val newValue: String
)

data class AuthState(
    val isAuthenticated: Boolean = false,
    val isBusy: Boolean = false,
    val errorMessage: String? = null,
    val pendingVerification: VerificationRequest? = null
)

data class DashboardState(
    val data: DashboardData = DashboardData(
        matches = emptyList(),
        standings = emptyMap(),
        news = emptyList()
    ),
    val isRefreshing: Boolean = false,
    val dataSourceLabel: String = "Cargando datos en vivo",
    val lastUpdatedLabel: String = "Esperando actualización",
    val refreshingMatchId: Int? = null
)
