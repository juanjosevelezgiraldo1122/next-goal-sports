package com.nextgoal.app.data.repository

import com.nextgoal.app.data.model.EventKind
import com.nextgoal.app.data.model.MatchDetailUi
import com.nextgoal.app.data.model.MatchEventUi
import com.nextgoal.app.data.model.MatchStatus
import com.nextgoal.app.data.model.MatchUi
import com.nextgoal.app.data.model.PlayerMatchUi
import com.nextgoal.app.data.model.StandingRowUi
import com.nextgoal.app.data.model.TeamUi
import com.nextgoal.app.data.remote.dto.MatchDto
import com.nextgoal.app.data.remote.dto.TableRowDto
import com.nextgoal.app.data.remote.dto.TeamDto

fun TeamDto.toUi() = TeamUi(
    id = id,
    name = name,
    shortName = shortName ?: tla ?: name,
    crestUrl = crest
)

fun String.toMatchStatus(): MatchStatus = when (this) {
    "LIVE", "IN_PLAY", "EXTRA_TIME", "PENALTY_SHOOTOUT" -> MatchStatus.LIVE
    "PAUSED" -> MatchStatus.PAUSED
    "SCHEDULED", "TIMED" -> MatchStatus.SCHEDULED
    "FINISHED", "AWARDED" -> MatchStatus.FINISHED
    "POSTPONED", "SUSPENDED", "CANCELLED" -> MatchStatus.POSTPONED
    else -> MatchStatus.OTHER
}

fun MatchDto.toUi(): MatchUi = MatchUi(
    id = id,
    competitionId = competition.id,
    competitionName = competition.name,
    competitionEmblem = competition.emblem,
    stage = stage,
    matchday = matchday,
    status = status.toMatchStatus(),
    minute = minute,
    utcDate = utcDate,
    homeTeam = homeTeam.toUi(),
    awayTeam = awayTeam.toUi(),
    homeScore = score.fullTime.home,
    awayScore = score.fullTime.away,
    venue = venue
)

fun MatchDto.toDetailUi(): MatchDetailUi {
    val events = mutableListOf<MatchEventUi>()

    goals?.forEach { g ->
        events += MatchEventUi(
            minute = g.minute,
            isHomeTeam = g.team?.id == homeTeam.id,
            label = "¡GOL de ${if (g.team?.id == homeTeam.id) homeTeam.shortName ?: homeTeam.name else awayTeam.shortName ?: awayTeam.name}!",
            subLabel = buildString {
                append(g.scorer?.name ?: "Anotador desconocido")
                if (g.assist?.name != null) append(" • Asistencia de ${g.assist.name}")
            },
            kind = EventKind.GOAL
        )
    }

    bookings?.forEach { b ->
        events += MatchEventUi(
            minute = b.minute,
            isHomeTeam = b.team?.id == homeTeam.id,
            label = if (b.card == "RED_CARD") "Tarjeta Roja" else "Tarjeta Amarilla",
            subLabel = b.player?.name,
            kind = if (b.card == "RED_CARD") EventKind.RED_CARD else EventKind.YELLOW_CARD
        )
    }

    substitutions?.forEach { s ->
        events += MatchEventUi(
            minute = s.minute,
            isHomeTeam = s.team?.id == homeTeam.id,
            label = "Cambio",
            subLabel = buildString {
                append(s.playerIn?.name ?: "?")
                append(" ⇆ ")
                append(s.playerOut?.name ?: "?")
            },
            kind = EventKind.SUBSTITUTION
        )
    }

    val sortedEvents = events.sortedByDescending { it.minute ?: 0 }

    return MatchDetailUi(
        match = toUi(),
        events = sortedEvents,
        // football-data.org's free tier does not expose possession/shots/corners or full lineups.
        // These flags let the UI show a clear, honest fallback instead of invented numbers.
        statsAvailable = false,
        lineupsAvailable = false
    )
}

fun TableRowDto.toUi() = StandingRowUi(
    position = position,
    team = team.toUi(),
    played = playedGames,
    goalDifference = goalDifference,
    points = points,
    won = won,
    draw = draw,
    lost = lost,
    goalsFor = goalsFor
)

fun MatchDto.toPlayerMatchUi(personTeamId: Int?): PlayerMatchUi {
    val wasHome = homeTeam.id == personTeamId
    val opponent = if (wasHome) awayTeam.name else homeTeam.name
    return PlayerMatchUi(
        opponent = opponent,
        competitionName = competition.name,
        homeScore = score.fullTime.home,
        awayScore = score.fullTime.away,
        wasHome = wasHome,
        utcDate = utcDate
    )
}
