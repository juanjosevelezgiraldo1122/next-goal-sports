package com.nextgoal.app

import android.os.Build
import android.util.Xml
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import org.xmlpull.v1.XmlPullParser
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.text.Normalizer
import java.text.SimpleDateFormat
import java.time.Instant
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Date
import java.util.Locale

class FootballRepository(private val token: String) {
    private val monitoredCompetitionCodes = listOf(
        "CL", "PL", "PD", "BL1", "SA", "FL1", "DED", "PPL", "ELC", "BSA", "LMX"
    )
    private val standingsCacheDurationMillis = 15 * 60 * 1_000L
    private val newsCacheDurationMillis = 5 * 60 * 1_000L
    private var cachedStandings: Map<String, List<StandingRow>> = emptyMap()
    private var standingsCachedAt = 0L
    private var cachedNews: List<NewsItem> = emptyList()
    private var newsCachedAt = 0L

    suspend fun loadDashboard(): Pair<DashboardData, String> = withContext(Dispatchers.IO) {
        if (token.isBlank()) {
            val publicResult = loadPublicMatches()
            val onlineNews = loadNewsIfStale()
            return@withContext DashboardData(
                matches = publicResult,
                standings = emptyMap(),
                news = onlineNews
            ) to if (publicResult.isEmpty()) "Sin datos reales disponibles" else "Actualizado desde fuente pública"
        }

        val onlineNews = loadNewsIfStale()
        runCatching {
            val today = LocalDate.now(NEXT_GOAL_TIME_ZONE)
            val rangeFrom = today.minusDays(7)
            val rangeUntil = today.plusDays(7)
            val competitionQuery = monitoredCompetitionCodes.joinToString(",")
            val dateRangeMatches = request(
                "https://api.football-data.org/v4/matches?dateFrom=$rangeFrom&dateTo=$rangeUntil&competitions=$competitionQuery"
            )
            val rangeMatches = parseMatches(dateRangeMatches)
            val liveMatches = runCatching {
                parseMatches(
                    request(
                        "https://api.football-data.org/v4/matches?status=IN_PLAY,PAUSED&competitions=$competitionQuery"
                    )
                )
            }.getOrDefault(emptyList())
            val merged = (liveMatches + rangeMatches).distinctBy { it.id }.sortedWith(matchComparator)
            val liveStandings = loadStandingsIfStale()
            val data = DashboardData(
                matches = merged,
                standings = liveStandings,
                news = onlineNews
            )
            data to if (merged.isEmpty()) "Jornada actualizada · sin partidos" else "Actualizado desde football-data.org"
        }.getOrElse {
            val publicMatches = loadPublicMatches()
            DashboardData(
                matches = publicMatches,
                standings = emptyMap(),
                news = onlineNews
            ) to if (publicMatches.isEmpty()) "Modo sin conexión" else "Actualizado desde fuente pública"
        }
    }

    suspend fun searchDirectory(query: String): List<DirectoryResult> = withContext(Dispatchers.IO) {
        val cleanQuery = query.trim()
        if (cleanQuery.length < 2) return@withContext emptyList()
        val encodedQuery = URLEncoder.encode(cleanQuery, "UTF-8")
        val players = runCatching {
            parseDirectoryPlayers(
                request(
                    "https://www.thesportsdb.com/api/v1/json/3/searchplayers.php?p=$encodedQuery",
                    includeToken = false
                )
            )
        }.getOrDefault(emptyList())
        val teams = runCatching {
            parseDirectoryTeams(
                request(
                    "https://www.thesportsdb.com/api/v1/json/3/searchteams.php?t=$encodedQuery",
                    includeToken = false
                )
            )
        }.getOrDefault(emptyList())
        (players + teams)
            .distinctBy { "${it.type}:${it.id}" }
            .take(30)
    }

    suspend fun loadDirectoryDetails(type: DirectoryEntityType, id: String): DirectoryDetails? = withContext(Dispatchers.IO) {
        val endpoint = when (type) {
            DirectoryEntityType.PLAYER -> "lookupplayer.php?id=$id"
            DirectoryEntityType.TEAM -> "lookupteam.php?id=$id"
        }
        runCatching {
            val payload = request(
                "https://www.thesportsdb.com/api/v1/json/3/$endpoint",
                includeToken = false
            )
            when (type) {
                DirectoryEntityType.PLAYER -> parseDirectoryPlayerDetails(payload)?.let { player ->
                    val teamId = player.currentTeamId ?: resolveTeamId(player.team)
                    val fixtures = loadTeamFixtures(teamId)
                    player.copy(
                        currentTeamId = teamId,
                        nextMatch = fixtures.first,
                        lastMatch = fixtures.second
                    )
                }
                DirectoryEntityType.TEAM -> parseDirectoryTeamDetails(payload)?.let { team ->
                    val fixtures = loadTeamFixtures(team.id)
                    team.copy(nextMatch = fixtures.first, lastMatch = fixtures.second)
                }
            }
        }.getOrNull()
    }

    private fun resolveTeamId(teamName: String): String? {
        if (teamName.isBlank()) return null
        val encodedName = URLEncoder.encode(teamName, "UTF-8")
        return runCatching {
            parseDirectoryTeams(
                request(
                    "https://www.thesportsdb.com/api/v1/json/3/searchteams.php?t=$encodedName",
                    includeToken = false
                )
            ).firstOrNull { it.name.equals(teamName, ignoreCase = true) }
                ?.id
                ?: parseDirectoryTeams(
                    request(
                        "https://www.thesportsdb.com/api/v1/json/3/searchteams.php?t=$encodedName",
                        includeToken = false
                    )
                ).firstOrNull()?.id
        }.getOrNull()
    }

    private fun loadTeamFixtures(teamId: String?): Pair<DirectoryFixture?, DirectoryFixture?> {
        if (teamId.isNullOrBlank()) return null to null
        val next = runCatching {
            parseDirectoryFixtures(
                request(
                    "https://www.thesportsdb.com/api/v1/json/3/eventsnext.php?id=$teamId",
                    includeToken = false
                ),
                isNext = true
            ).firstOrNull()
        }.getOrNull()
        val last = runCatching {
            parseDirectoryFixtures(
                request(
                    "https://www.thesportsdb.com/api/v1/json/3/eventslast.php?id=$teamId",
                    includeToken = false
                ),
                isNext = false
            ).firstOrNull()
        }.getOrNull()?.let(::loadDirectoryFixtureStats)
        return next to last
    }

    private fun parseDirectoryFixtures(payload: JSONObject, isNext: Boolean): List<DirectoryFixture> {
        val events = payload.optJSONArray("events") ?: return emptyList()
        return buildList {
            for (index in 0 until events.length()) {
                val event = events.optJSONObject(index) ?: continue
                val homeTeam = event.optString("strHomeTeam").trim()
                val awayTeam = event.optString("strAwayTeam").trim()
                if (homeTeam.isBlank() || awayTeam.isBlank()) continue
                add(
                    DirectoryFixture(
                        id = event.optString("idEvent"),
                        competition = spanishCompetitionName(event.optString("strLeague").ifBlank { "Competición no publicada" }),
                        homeTeam = homeTeam,
                        awayTeam = awayTeam,
                        homeLogoUrl = firstNonBlank(event.optString("strHomeTeamBadge"), event.optString("strHomeTeamLogo")),
                        awayLogoUrl = firstNonBlank(event.optString("strAwayTeamBadge"), event.optString("strAwayTeamLogo")),
                        kickoffLabel = formatDirectoryKickoff(event),
                        statusLabel = directoryStatus(event.optString("strStatus"), isNext),
                        homeScore = event.nullableInt("intHomeScore"),
                        awayScore = event.nullableInt("intAwayScore"),
                        venue = event.optString("strVenue").ifBlank { "Estadio no publicado" }
                    )
                )
            }
        }
    }

    private fun loadDirectoryFixtureStats(fixture: DirectoryFixture): DirectoryFixture {
        if (fixture.id.isBlank()) return fixture
        return runCatching {
            val payload = request(
                "https://www.thesportsdb.com/api/v1/json/3/lookupevent.php?id=${fixture.id}",
                includeToken = false
            )
            val event = payload.optJSONArray("events")?.optJSONObject(0) ?: return@runCatching fixture
            fixture.copy(stats = parseDirectoryMatchStats(event))
        }.getOrDefault(fixture)
    }

    private fun parseDirectoryMatchStats(event: JSONObject): List<DirectoryMatchStat> {
        val statKeys = listOf(
            "Posesión" to listOf("intHomePossession" to "intAwayPossession"),
            "Tiros" to listOf("intHomeShots" to "intAwayShots"),
            "Tiros a puerta" to listOf("intHomeShotsOnGoal" to "intAwayShotsOnGoal", "intHomeShotsOnTarget" to "intAwayShotsOnTarget"),
            "Saques de esquina" to listOf("intHomeCorners" to "intAwayCorners"),
            "Faltas" to listOf("intHomeFouls" to "intAwayFouls")
        )
        return statKeys.mapNotNull { (label, candidates) ->
            val values = candidates.firstNotNullOfOrNull { (homeKey, awayKey) ->
                val home = event.optString(homeKey).trim()
                val away = event.optString(awayKey).trim()
                if (home.isBlank() || home == "null" || away.isBlank() || away == "null") null else home to away
            }
            values?.let { DirectoryMatchStat(label, it.first, it.second) }
        }
    }

    private fun directoryStatus(status: String, isNext: Boolean): String {
        val normalized = status.lowercase(NEXT_GOAL_LOCALE)
        return when {
            normalized.contains("postpon") -> "APLAZADO"
            normalized.contains("cancel") -> "CANCELADO"
            normalized.contains("finish") || normalized.contains("final") || normalized == "ft" -> "FINALIZADO"
            isNext && (normalized.isBlank() || normalized.contains("not started") || normalized.contains("scheduled")) -> "PRÓXIMO"
            normalized.contains("live") || normalized.contains("half") || normalized == "1h" || normalized == "2h" -> "EN VIVO"
            isNext -> "PRÓXIMO"
            else -> "FINALIZADO"
        }
    }

    private fun formatDirectoryKickoff(event: JSONObject): String {
        val timestamp = parseTimestamp(event.optString("strTimestamp"))
        if (timestamp > 0L) {
            return Instant.ofEpochMilli(timestamp)
                .atZone(NEXT_GOAL_TIME_ZONE)
                .format(DateTimeFormatter.ofPattern("EEE d MMM · HH:mm", NEXT_GOAL_LOCALE))
        }
        val date = event.optString("dateEvent").trim()
        val time = event.optString("strTime").take(5)
        return listOf(date, time).filter { it.isNotBlank() }.joinToString(" · ").ifBlank { "Hora no publicada" }
    }

    private fun parseDirectoryPlayers(payload: JSONObject): List<DirectoryResult> {
        val players = payload.optJSONArray("player") ?: return emptyList()
        return buildList {
            for (index in 0 until players.length()) {
                val player = players.optJSONObject(index) ?: continue
                val name = player.optString("strPlayer").trim()
                if (name.isBlank()) continue
                val subtitle = listOf(
                    player.optString("strTeam"),
                    spanishPosition(player.optString("strPosition")),
                    spanishCountry(player.optString("strNationality"))
                ).filter { it.isNotBlank() }.joinToString(" · ").ifBlank { "Jugador" }
                add(
                    DirectoryResult(
                        id = player.optString("idPlayer"),
                        type = DirectoryEntityType.PLAYER,
                        name = name,
                        subtitle = subtitle,
                        imageUrl = firstNonBlank(player.optString("strCutout"), player.optString("strThumb"))
                    )
                )
            }
        }.filter { it.id.isNotBlank() }
    }

    private fun parseDirectoryTeams(payload: JSONObject): List<DirectoryResult> {
        val teams = payload.optJSONArray("teams") ?: return emptyList()
        return buildList {
            for (index in 0 until teams.length()) {
                val team = teams.optJSONObject(index) ?: continue
                val name = team.optString("strTeam").trim()
                if (name.isBlank()) continue
                val subtitle = listOf(
                    spanishCompetitionName(team.optString("strLeague")),
                    spanishCountry(team.optString("strCountry"))
                ).filter { it.isNotBlank() }.joinToString(" · ").ifBlank { "Equipo" }
                add(
                    DirectoryResult(
                        id = team.optString("idTeam"),
                        type = DirectoryEntityType.TEAM,
                        name = name,
                        subtitle = subtitle,
                        imageUrl = firstNonBlank(
                            team.optString("strTeamBadge"),
                            team.optString("strBadge"),
                            team.optString("strLogo")
                        )
                    )
                )
            }
        }.filter { it.id.isNotBlank() }
    }

    private fun parseDirectoryPlayerDetails(payload: JSONObject): DirectoryDetails.Player? {
        val player = payload.optJSONArray("players")?.optJSONObject(0) ?: return null
        val name = player.optString("strPlayer").ifBlank { return null }
        return DirectoryDetails.Player(
            id = player.optString("idPlayer"),
            name = name,
            team = player.optString("strTeam").ifBlank { "Equipo no publicado" },
            position = spanishPosition(player.optString("strPosition")),
            nationality = spanishCountry(player.optString("strNationality")),
            birthDate = player.optString("dateBorn").ifBlank { "No publicada" },
            birthPlace = player.optString("strBirthLocation").ifBlank { "No publicado" },
            height = player.optString("strHeight").ifBlank { "No publicado" },
            weight = player.optString("strWeight").ifBlank { "No publicado" },
            photoUrl = firstNonBlank(player.optString("strCutout"), player.optString("strThumb")),
            teamBadgeUrl = firstNonBlank(player.optString("strTeamBadge"), player.optString("strBadge")),
            description = cleanMarkup(player.optString("strDescriptionES")).ifBlank {
                "No hay una biografía publicada por la fuente."
            },
            currentTeamId = firstNonBlank(player.optString("idTeam"), player.optString("idTeam1"))
        )
    }

    private fun parseDirectoryTeamDetails(payload: JSONObject): DirectoryDetails.Team? {
        val team = payload.optJSONArray("teams")?.optJSONObject(0) ?: return null
        val name = team.optString("strTeam").ifBlank { return null }
        return DirectoryDetails.Team(
            id = team.optString("idTeam"),
            name = name,
            league = spanishCompetitionName(team.optString("strLeague").ifBlank { "Competición no publicada" }),
            country = spanishCountry(team.optString("strCountry")),
            founded = team.optString("intFormedYear").ifBlank { "No publicado" },
            stadium = team.optString("strStadium").ifBlank { "Estadio no publicado" },
            stadiumCapacity = team.optString("intStadiumCapacity").ifBlank { "No publicada" },
            logoUrl = firstNonBlank(
                team.optString("strTeamBadge"),
                team.optString("strBadge"),
                team.optString("strLogo")
            ),
            bannerUrl = firstNonBlank(
                team.optString("strFanart1"),
                team.optString("strTeamFanart1"),
                team.optString("strFanart2"),
                team.optString("strTeamFanart2")
            ),
            description = cleanMarkup(team.optString("strDescriptionES")).ifBlank {
                "No hay una descripción publicada por la fuente."
            },
            website = team.optString("strWebsite").trim().takeIf { it.isNotBlank() }
        )
    }

    private fun firstNonBlank(vararg values: String): String? {
        return values.firstOrNull { it.isNotBlank() }?.trim()
    }

    private fun request(urlValue: String, includeToken: Boolean = true): JSONObject {
        val connection = URL(urlValue).openConnection() as HttpURLConnection
        connection.connectTimeout = 6_000
        connection.readTimeout = 8_000
        connection.requestMethod = "GET"
        if (includeToken && token.isNotBlank()) {
            connection.setRequestProperty("X-Auth-Token", token)
            connection.setRequestProperty("X-Unfold-Goals", "true")
            connection.setRequestProperty("X-Unfold-Bookings", "true")
            connection.setRequestProperty("X-Unfold-Subs", "true")
            connection.setRequestProperty("X-Unfold-Lineups", "true")
        }
        connection.setRequestProperty("Accept", "application/json")

        return try {
            if (connection.responseCode !in 200..299) {
                error("Football API respondió ${connection.responseCode}")
            }
            JSONObject(connection.inputStream.bufferedReader().use { it.readText() })
        } finally {
            connection.disconnect()
        }
    }

    private fun parseMatches(payload: JSONObject): List<Match> {
        val rawMatches = payload.optJSONArray("matches") ?: JSONArray()
        return buildList {
            for (index in 0 until rawMatches.length()) {
                val raw = rawMatches.optJSONObject(index) ?: continue
                val homeJson = raw.optJSONObject("homeTeam") ?: continue
                val awayJson = raw.optJSONObject("awayTeam") ?: continue
                val home = parseTeam(homeJson)
                val away = parseTeam(awayJson)
                val status = raw.optString("status", "SCHEDULED")
                val score = raw.optJSONObject("score")
                val fullTime = score?.optJSONObject("fullTime")
                val current = score?.optJSONObject("current")
                val phase = when (status) {
                    "IN_PLAY", "PAUSED", "LIVE" -> MatchPhase.LIVE
                    "FINISHED", "AWARDED" -> MatchPhase.FINISHED
                    else -> MatchPhase.UPCOMING
                }
                val utcDate = raw.optString("utcDate")
                val competitionJson = raw.optJSONObject("competition")
                val stats = parseMatchStats(homeJson, awayJson)
                val events = parseMatchEvents(raw)
                add(
                    Match(
                        id = raw.optInt("id", 0),
                        competition = spanishCompetitionName(competitionJson?.optString("name").orEmpty()),
                        competitionShort = competitionJson?.optString("code")
                            .orEmpty().ifBlank { "FÚTBOL" },
                        competitionCode = competitionJson?.optString("code")
                            .orEmpty().uppercase(NEXT_GOAL_LOCALE),
                        home = home,
                        away = away,
                        homeScore = fullTime?.nullableInt("home") ?: current?.nullableInt("home"),
                        awayScore = fullTime?.nullableInt("away") ?: current?.nullableInt("away"),
                        minute = raw.nullableInt("minute")
                            ?: score?.nullableInt("minute")
                            ?: if (phase == MatchPhase.LIVE) estimateLiveMinute(utcDate) else null,
                        kickoffLabel = formatKickoff(utcDate, phase),
                        phase = phase,
                        venue = raw.optString("venue").ifBlank { "Estadio por confirmar" },
                        kickoffMillis = parseTimestamp(utcDate),
                        stats = stats,
                        events = events
                    )
                )
            }
        }.filter { it.id != 0 }
    }

    private fun loadStandings(): Map<String, List<StandingRow>> {
        val competitionMap = mapOf(
            "PL" to "PREMIER LEAGUE",
            "CL" to "LIGA DE CAMPEONES",
            "PD" to "LA LIGA",
            "BL1" to "BUNDESLIGA",
            "SA" to "SERIE A",
            "FL1" to "LIGUE 1",
            "LMX" to "LIGA MX"
        )
        return competitionMap.mapNotNull { (code, displayName) ->
            runCatching {
                val payload = request("https://api.football-data.org/v4/competitions/$code/standings")
                val tables = payload.optJSONArray("standings") ?: return@runCatching null
                val table = tables.optJSONObject(0)?.optJSONArray("table") ?: return@runCatching null
                val rows = buildList {
                    for (index in 0 until table.length()) {
                        val raw = table.optJSONObject(index) ?: continue
                        val rawTeam = raw.optJSONObject("team") ?: continue
                        val name = rawTeam.optString("name").ifBlank { "Equipo" }
                        add(
                            StandingRow(
                                position = raw.optInt("position", index + 1),
                                team = Team(
                                    id = rawTeam.optInt("id", 0),
                                    name = name,
                                    shortName = rawTeam.optString("shortName").ifBlank { name },
                                    crestUrl = rawTeam.optString("crest").ifBlank { null }
                                ),
                                played = raw.optInt("playedGames", 0),
                                goalDifference = raw.optInt("goalDifference", 0),
                                points = raw.optInt("points", 0)
                            )
                        )
                    }
                }
                if (rows.isEmpty()) null else displayName to rows
            }.getOrNull()
        }.filterNotNull().toMap()
    }

    private fun loadStandingsIfStale(): Map<String, List<StandingRow>> {
        val now = System.currentTimeMillis()
        if (cachedStandings.isNotEmpty() && now - standingsCachedAt < standingsCacheDurationMillis) {
            return cachedStandings
        }
        val fresh = loadStandings()
        if (fresh.isNotEmpty()) {
            cachedStandings = fresh
            standingsCachedAt = now
        }
        return cachedStandings
    }

    private fun parseSportsDb(payload: JSONObject): List<Match> {
        val events = payload.optJSONArray("events") ?: JSONArray()
        return buildList {
            for (index in 0 until events.length()) {
                val event = events.optJSONObject(index) ?: continue
                val homeName = event.optString("strHomeTeam")
                val awayName = event.optString("strAwayTeam")
                if (homeName.isBlank() || awayName.isBlank()) continue
            val status = event.optString("strStatus").lowercase(NEXT_GOAL_LOCALE)
                val progress = event.optString("strProgress")
                val phase = when {
                    status.contains("finished") || status.contains("final") || status == "ft" || status == "aet" -> MatchPhase.FINISHED
                    status.contains("live") || progress.contains("'") -> MatchPhase.LIVE
                    else -> MatchPhase.UPCOMING
                }
                val homeScore = event.nullableInt("intHomeScore")
                val awayScore = event.nullableInt("intAwayScore")
                add(
                    Match(
                        id = event.optString("idEvent").hashCode(),
                        competition = spanishCompetitionName(event.optString("strLeague").ifBlank { "Fútbol" }),
                        competitionShort = event.optString("strLeague").split(" ").joinToString("") { it.take(1) }.take(5).uppercase(NEXT_GOAL_LOCALE),
                        competitionCode = event.optString("idLeague").ifBlank { "PUBLIC" },
                        home = Team(
                            id = event.optInt("idHomeTeam", homeName.hashCode()),
                            name = homeName,
                            shortName = homeName,
                            crestUrl = event.optString("strHomeTeamBadge").ifBlank { null }
                        ),
                        away = Team(
                            id = event.optInt("idAwayTeam", awayName.hashCode()),
                            name = awayName,
                            shortName = awayName,
                            crestUrl = event.optString("strAwayTeamBadge").ifBlank { null }
                        ),
                        homeScore = homeScore,
                        awayScore = awayScore,
                        minute = progress.filter { it.isDigit() }.toIntOrNull(),
                        kickoffLabel = if (phase == MatchPhase.FINISHED) "FINALIZADO" else if (phase == MatchPhase.LIVE) "EN VIVO" else event.optString("strTimeLocal").take(5).ifBlank { event.optString("strTime").take(5).ifBlank { "20:00" } },
                        phase = phase,
                        venue = event.optString("strVenue").ifBlank { "Estadio por confirmar" },
                        kickoffMillis = parseTimestamp(event.optString("strTimestamp")),
                        externalEventId = event.optString("idEvent").ifBlank { null }
                    )
                )
            }
        }.sortedWith(matchComparator)
    }

    private fun loadPublicMatches(): List<Match> {
        val today = LocalDate.now(NEXT_GOAL_TIME_ZONE)
        return runCatching {
            parseSportsDb(
                request(
                    "https://www.thesportsdb.com/api/v1/json/3/eventsday.php?d=$today&s=Soccer",
                    includeToken = false
                )
            )
        }.getOrDefault(emptyList())
    }

    suspend fun loadMatchDetails(match: Match): Match? = withContext(Dispatchers.IO) {
        runCatching {
            val footballDataMatch = if (token.isNotBlank()) {
                loadFootballDataMatchDetails(match)
            } else null
            if (footballDataMatch?.stats != null) return@runCatching footballDataMatch

            val sourceMatch = footballDataMatch ?: match
            val event = resolveSportsDbEvent(sourceMatch) ?: return@runCatching footballDataMatch
            val eventId = event.optString("idEvent").ifBlank { return@runCatching null }
            val statsPayload = runCatching {
                request("https://www.thesportsdb.com/api/v1/json/3/lookupeventstats.php?id=$eventId", includeToken = false)
            }.getOrDefault(JSONObject())
            val timelinePayload = runCatching {
                request("https://www.thesportsdb.com/api/v1/json/3/lookuptimeline.php?id=$eventId", includeToken = false)
            }.getOrDefault(JSONObject())
            val updated = mergeSportsDbEvent(sourceMatch, event)
            updated.copy(
                stats = parseSportsDbStats(statsPayload) ?: sourceMatch.stats,
                events = parseSportsDbTimeline(timelinePayload).ifEmpty { sourceMatch.events },
                externalEventId = eventId
            )
        }.getOrNull()
    }

    private fun loadFootballDataMatchDetails(match: Match): Match? {
        return runCatching {
            val raw = request("https://api.football-data.org/v4/matches/${match.id}")
            val homeJson = raw.optJSONObject("homeTeam") ?: return@runCatching null
            val awayJson = raw.optJSONObject("awayTeam") ?: return@runCatching null
            val status = raw.optString("status", "SCHEDULED")
            val score = raw.optJSONObject("score")
            val fullTime = score?.optJSONObject("fullTime")
            val current = score?.optJSONObject("current")
            val phase = when (status) {
                "IN_PLAY", "PAUSED", "LIVE" -> MatchPhase.LIVE
                "FINISHED", "AWARDED" -> MatchPhase.FINISHED
                else -> MatchPhase.UPCOMING
            }
            val utcDate = raw.optString("utcDate")
            val competition = raw.optJSONObject("competition")
            match.copy(
                competition = spanishCompetitionName(competition?.optString("name").orEmpty()),
                competitionShort = competition?.optString("code").orEmpty().ifBlank { match.competitionShort },
                competitionCode = competition?.optString("code").orEmpty().ifBlank { match.competitionCode },
                home = parseTeam(homeJson),
                away = parseTeam(awayJson),
                homeScore = fullTime?.nullableInt("home") ?: current?.nullableInt("home") ?: match.homeScore,
                awayScore = fullTime?.nullableInt("away") ?: current?.nullableInt("away") ?: match.awayScore,
                minute = raw.nullableInt("minute") ?: score?.nullableInt("minute") ?: match.minute,
                kickoffLabel = formatKickoff(utcDate, phase),
                phase = phase,
                venue = raw.optString("venue").ifBlank { match.venue },
                kickoffMillis = parseTimestamp(utcDate).takeIf { it > 0L } ?: match.kickoffMillis,
                stats = parseMatchStats(homeJson, awayJson) ?: match.stats,
                events = parseMatchEvents(raw).ifEmpty { match.events },
                externalEventId = match.externalEventId
            )
        }.getOrNull()
    }

    private fun resolveSportsDbEvent(match: Match): JSONObject? {
        match.externalEventId?.takeIf { it.isNotBlank() }?.let { eventId ->
            runCatching {
                request("https://www.thesportsdb.com/api/v1/json/3/lookupevent.php?id=$eventId", includeToken = false)
                    .optJSONArray("events")
                    ?.optJSONObject(0)
            }.getOrNull()?.let { return it }
        }

        val date = if (match.kickoffMillis > 0L) {
            Instant.ofEpochMilli(match.kickoffMillis).atZone(NEXT_GOAL_TIME_ZONE).toLocalDate()
        } else {
            LocalDate.now(NEXT_GOAL_TIME_ZONE)
        }
        val dayEvents = runCatching {
            request(
                "https://www.thesportsdb.com/api/v1/json/3/eventsday.php?d=$date&s=Soccer",
                includeToken = false
            ).optJSONArray("events")
        }.getOrNull()
        selectSportsDbEvent(dayEvents, match)?.let { return it }

        val query = URLEncoder.encode("${match.home.name}_vs_${match.away.name}", "UTF-8")
            .replace("+", "_")
        val searchEvents = runCatching {
            val payload = request(
                "https://www.thesportsdb.com/api/v1/json/3/searchevents.php?e=$query&d=$date&s=Soccer",
                includeToken = false
            )
            payload.optJSONArray("event") ?: payload.optJSONArray("events")
        }.getOrNull()
        return selectSportsDbEvent(searchEvents, match)
    }

    private fun selectSportsDbEvent(events: JSONArray?, match: Match): JSONObject? {
        if (events == null || events.length() == 0) return null
        val expectedDate = if (match.kickoffMillis > 0L) {
            Instant.ofEpochMilli(match.kickoffMillis).atZone(NEXT_GOAL_TIME_ZONE).toLocalDate()
        } else null
        val expectedTimestamp = match.kickoffMillis
        val candidates = buildList {
            for (index in 0 until events.length()) {
                val event = events.optJSONObject(index) ?: continue
                val teamScore = teamSimilarity(match.home.name, event.optString("strHomeTeam")) +
                    teamSimilarity(match.away.name, event.optString("strAwayTeam"))
                if (teamScore < 9) continue
                val eventTimestamp = parseTimestamp(event.optString("strTimestamp"))
                val sameDate = expectedDate != null && eventTimestamp > 0L &&
                    Instant.ofEpochMilli(eventTimestamp).atZone(NEXT_GOAL_TIME_ZONE).toLocalDate() == expectedDate
                val timeScore = when {
                    sameDate && expectedTimestamp > 0L && eventTimestamp > 0L &&
                        kotlin.math.abs(eventTimestamp - expectedTimestamp) <= 18 * 60 * 60 * 1000L -> 4
                    sameDate -> 2
                    else -> 0
                }
                add((teamScore * 10 + timeScore) to event)
            }
        }
        return candidates.maxByOrNull { it.first }?.second
    }

    private fun teamSimilarity(expected: String, actual: String): Int {
        val left = normalizeTeamName(expected)
        val right = normalizeTeamName(actual)
        if (left.isBlank() || right.isBlank()) return 0
        if (left == right) return 6
        if (left.contains(right) || right.contains(left)) return 5
        val leftTokens = left.split(" ").filter { it.length > 2 }.toSet()
        val rightTokens = right.split(" ").filter { it.length > 2 }.toSet()
        return (leftTokens intersect rightTokens).size * 3
    }

    private fun normalizeTeamName(value: String): String {
        return Normalizer.normalize(value.lowercase(Locale.ROOT), Normalizer.Form.NFD)
            .replace(Regex("\\p{InCombiningDiacriticalMarks}+"), "")
            .replace(Regex("\\b(fc|cf|afc|ac|club|football|futbol|cd|ca|sc|rc)\\b"), " ")
            .replace(Regex("[^a-z0-9 ]"), " ")
            .replace(Regex("\\s+"), " ")
            .trim()
    }

    private fun mergeSportsDbEvent(match: Match, event: JSONObject): Match {
        val status = event.optString("strStatus").lowercase(NEXT_GOAL_LOCALE)
        val progress = event.optString("strProgress").lowercase(NEXT_GOAL_LOCALE)
        val phase = when {
            status.contains("finished") || status.contains("final") || status == "ft" || status == "aet" -> MatchPhase.FINISHED
            status.contains("live") || status.contains("half") || progress.contains("'") || progress.matches(Regex(".*\\d+.*")) -> MatchPhase.LIVE
            else -> match.phase
        }
        val timestamp = parseTimestamp(event.optString("strTimestamp"))
        return match.copy(
            homeScore = event.nullableInt("intHomeScore") ?: match.homeScore,
            awayScore = event.nullableInt("intAwayScore") ?: match.awayScore,
            minute = parseSportsDbMinute(event) ?: match.minute,
            phase = phase,
            kickoffLabel = formatKickoff(
                event.optString("strTimestamp").ifBlank { formatTimestamp(timestamp) },
                phase
            ),
            venue = event.optString("strVenue").ifBlank { match.venue },
            kickoffMillis = timestamp.takeIf { it > 0L } ?: match.kickoffMillis,
            competition = spanishCompetitionName(event.optString("strLeague").ifBlank { match.competition })
        )
    }

    private fun formatTimestamp(timestamp: Long): String {
        if (timestamp <= 0L) return ""
        return Instant.ofEpochMilli(timestamp).toString()
    }

    private fun parseSportsDbMinute(event: JSONObject): Int? {
        return Regex("\\d+").find(event.optString("strProgress"))?.value?.toIntOrNull()
    }

    private fun parseSportsDbStats(payload: JSONObject): MatchStats? {
        val rows = payload.optJSONArray("eventstats") ?: return null
        var possession: Pair<Int?, Int?>? = null
        var shotsOnTarget: Pair<Int?, Int?>? = null
        var shotsOffTarget: Pair<Int?, Int?>? = null
        var shots: Pair<Int?, Int?>? = null
        var blockedShots: Pair<Int?, Int?>? = null
        var corners: Pair<Int?, Int?>? = null
        var fouls: Pair<Int?, Int?>? = null
        var offsides: Pair<Int?, Int?>? = null
        var freeKicks: Pair<Int?, Int?>? = null
        var goalKicks: Pair<Int?, Int?>? = null
        var saves: Pair<Int?, Int?>? = null
        var throwIns: Pair<Int?, Int?>? = null
        var yellowCards: Pair<Int?, Int?>? = null
        var redCards: Pair<Int?, Int?>? = null
        for (index in 0 until rows.length()) {
            val row = rows.optJSONObject(index) ?: continue
            val label = row.optString("strStat").lowercase(NEXT_GOAL_LOCALE)
            val values = row.statPair()
            when {
                label.contains("possession") -> possession = values
                label.contains("shots on goal") || label.contains("shots on target") -> shotsOnTarget = values
                label.contains("shots off goal") || label.contains("shots off target") -> shotsOffTarget = values
                label == "total shots" || label.contains("total shots") -> shots = values
                label.contains("blocked shots") -> blockedShots = values
                label.contains("corner") -> corners = values
                label == "fouls" || label.contains("fouls") -> fouls = values
                label.contains("offside") -> offsides = values
                label.contains("free kick") -> freeKicks = values
                label.contains("goal kick") -> goalKicks = values
                label.contains("save") -> saves = values
                label.contains("throw") -> throwIns = values
                label.contains("yellow card") -> yellowCards = values
                label.contains("red card") -> redCards = values
            }
        }
        val stats = MatchStats(
            homePossession = possession?.first,
            awayPossession = possession?.second,
            homeShotsOnTarget = shotsOnTarget?.first,
            awayShotsOnTarget = shotsOnTarget?.second,
            homeShotsOffTarget = shotsOffTarget?.first,
            awayShotsOffTarget = shotsOffTarget?.second,
            homeShots = shots?.first,
            awayShots = shots?.second,
            homeBlockedShots = blockedShots?.first,
            awayBlockedShots = blockedShots?.second,
            homeCorners = corners?.first,
            awayCorners = corners?.second,
            homeFouls = fouls?.first,
            awayFouls = fouls?.second,
            homeOffsides = offsides?.first,
            awayOffsides = offsides?.second,
            homeFreeKicks = freeKicks?.first,
            awayFreeKicks = freeKicks?.second,
            homeGoalKicks = goalKicks?.first,
            awayGoalKicks = goalKicks?.second,
            homeSaves = saves?.first,
            awaySaves = saves?.second,
            homeThrowIns = throwIns?.first,
            awayThrowIns = throwIns?.second,
            homeYellowCards = yellowCards?.first,
            awayYellowCards = yellowCards?.second,
            homeRedCards = redCards?.first,
            awayRedCards = redCards?.second
        )
        return stats.takeIf { it.hasAnyPublishedValue() }
    }

    private fun JSONObject.statPair(): Pair<Int?, Int?> {
        fun parse(key: String): Int? = optString(key).replace("%", "").trim().toDoubleOrNull()?.toInt()
        return parse("intHome") to parse("intAway")
    }

    private fun MatchStats.hasAnyPublishedValue(): Boolean {
        return listOf(
            homePossession, awayPossession, homeShotsOnTarget, awayShotsOnTarget,
            homeShotsOffTarget, awayShotsOffTarget, homeShots, awayShots,
            homeBlockedShots, awayBlockedShots, homeCorners, awayCorners,
            homeFouls, awayFouls, homeOffsides, awayOffsides, homeFreeKicks,
            awayFreeKicks, homeGoalKicks, awayGoalKicks, homeSaves, awaySaves,
            homeThrowIns, awayThrowIns, homeYellowCards, awayYellowCards,
            homeRedCards, awayRedCards
        ).any { it != null }
    }

    private fun parseSportsDbTimeline(payload: JSONObject): List<MatchEvent> {
        val timeline = payload.optJSONArray("timeline") ?: return emptyList()
        return buildList {
            for (index in 0 until timeline.length()) {
                val item = timeline.optJSONObject(index) ?: continue
                val rawType = item.optString("strTimeline").lowercase(NEXT_GOAL_LOCALE)
                val detail = item.optString("strTimelineDetail").lowercase(NEXT_GOAL_LOCALE)
                val team = item.optString("strTeam").ifBlank { "Equipo" }
                val player = item.optString("strPlayer").ifBlank { "Jugador" }
                val assist = item.optString("strAssist").ifBlank { "" }
                val minute = item.optString("intTime").filter { it.isDigit() }.toIntOrNull()
                when {
                    rawType.contains("goal") -> add(
                        MatchEvent(
                            minute = minute,
                            kind = "GOAL",
                            title = if (detail.contains("penalty")) "Gol de penalti · $player" else "Gol · $player",
                            subtitle = if (assist.isBlank()) team else "$team · Asistencia de $assist",
                            teamName = team
                        )
                    )
                    rawType.contains("card") -> add(
                        MatchEvent(
                            minute = minute,
                            kind = if (detail.contains("red")) "RED_CARD" else "CARD",
                            title = if (detail.contains("red")) "Tarjeta roja" else "Tarjeta amarilla",
                            subtitle = "$player · $team",
                            teamName = team
                        )
                    )
                    rawType.contains("subst") -> add(
                        MatchEvent(
                            minute = minute,
                            kind = "SUBSTITUTION",
                            title = "Cambio",
                            subtitle = if (assist.isBlank()) "$team · $player" else "$team · Entra $assist, sale $player",
                            teamName = team
                        )
                    )
                }
            }
        }.sortedBy { it.minute ?: Int.MAX_VALUE }
    }

    private fun parseMatchStats(home: JSONObject, away: JSONObject): MatchStats? {
        val homeStats = home.optJSONObject("statistics")
        val awayStats = away.optJSONObject("statistics")
        val stats = MatchStats(
            homePossession = homeStats?.nullableStatInt("ball_possession"),
            awayPossession = awayStats?.nullableStatInt("ball_possession"),
            homeShotsOnTarget = homeStats?.nullableStatInt("shots_on_goal"),
            awayShotsOnTarget = awayStats?.nullableStatInt("shots_on_goal"),
            homeShotsOffTarget = homeStats?.nullableStatInt("shots_off_goal"),
            awayShotsOffTarget = awayStats?.nullableStatInt("shots_off_goal"),
            homeShots = homeStats?.nullableStatInt("shots"),
            awayShots = awayStats?.nullableStatInt("shots"),
            homeBlockedShots = homeStats?.nullableStatInt("blocked_shots"),
            awayBlockedShots = awayStats?.nullableStatInt("blocked_shots"),
            homeCorners = homeStats?.nullableStatInt("corner_kicks"),
            awayCorners = awayStats?.nullableStatInt("corner_kicks"),
            homeFouls = homeStats?.nullableStatInt("fouls"),
            awayFouls = awayStats?.nullableStatInt("fouls"),
            homeOffsides = homeStats?.nullableStatInt("offsides"),
            awayOffsides = awayStats?.nullableStatInt("offsides"),
            homeFreeKicks = homeStats?.nullableStatInt("free_kicks"),
            awayFreeKicks = awayStats?.nullableStatInt("free_kicks"),
            homeGoalKicks = homeStats?.nullableStatInt("goal_kicks"),
            awayGoalKicks = awayStats?.nullableStatInt("goal_kicks"),
            homeSaves = homeStats?.nullableStatInt("saves"),
            awaySaves = awayStats?.nullableStatInt("saves"),
            homeThrowIns = homeStats?.nullableStatInt("throw_ins"),
            awayThrowIns = awayStats?.nullableStatInt("throw_ins"),
            homeYellowCards = homeStats?.nullableStatInt("yellow_cards"),
            awayYellowCards = awayStats?.nullableStatInt("yellow_cards"),
            homeRedCards = homeStats?.nullableStatInt("red_cards"),
            awayRedCards = awayStats?.nullableStatInt("red_cards")
        )
        return stats.takeIf { it.hasAnyPublishedValue() }
    }

    private fun parseMatchEvents(raw: JSONObject): List<MatchEvent> {
        val events = mutableListOf<MatchEvent>()
        val goals = raw.optJSONArray("goals") ?: JSONArray()
        for (index in 0 until goals.length()) {
            val goal = goals.optJSONObject(index) ?: continue
            val teamName = goal.optJSONObject("team")?.optString("name").orEmpty().ifBlank { "Equipo" }
            val scorer = goal.optJSONObject("scorer")?.optString("name").orEmpty().ifBlank { "Anotador" }
            val assist = goal.optJSONObject("assist")?.optString("name").orEmpty()
            val goalType = goal.optString("type").lowercase(NEXT_GOAL_LOCALE)
            val detail = when (goalType) {
                "penalty" -> "Gol de penalti"
                "own" -> "Gol en propia puerta"
                else -> "Gol"
            }
            events += MatchEvent(
                minute = goal.nullableInt("minute"),
                kind = "GOAL",
                title = "$detail · $scorer",
                subtitle = if (assist.isBlank()) teamName else "$teamName · Asistencia de $assist",
                teamName = teamName
            )
        }

        val bookings = raw.optJSONArray("bookings") ?: JSONArray()
        for (index in 0 until bookings.length()) {
            val booking = bookings.optJSONObject(index) ?: continue
            val teamName = booking.optJSONObject("team")?.optString("name").orEmpty().ifBlank { "Equipo" }
            val player = booking.optJSONObject("player")?.optString("name").orEmpty().ifBlank { "Jugador" }
            val card = booking.optString("card").lowercase(NEXT_GOAL_LOCALE)
            events += MatchEvent(
                minute = booking.nullableInt("minute"),
                kind = "CARD",
                title = if (card.contains("red")) "Tarjeta roja" else "Tarjeta amarilla",
                subtitle = "$player · $teamName",
                teamName = teamName
            )
        }

        val substitutions = raw.optJSONArray("substitutions") ?: JSONArray()
        for (index in 0 until substitutions.length()) {
            val substitution = substitutions.optJSONObject(index) ?: continue
            val teamName = substitution.optJSONObject("team")?.optString("name").orEmpty().ifBlank { "Equipo" }
            val playerIn = substitution.optJSONObject("playerIn")?.optString("name").orEmpty().ifBlank { "Jugador" }
            val playerOut = substitution.optJSONObject("playerOut")?.optString("name").orEmpty().ifBlank { "Jugador" }
            events += MatchEvent(
                minute = substitution.nullableInt("minute"),
                kind = "SUBSTITUTION",
                title = "Cambio",
                subtitle = "$teamName · Entra $playerIn, sale $playerOut",
                teamName = teamName
            )
        }
        return events.sortedBy { it.minute ?: Int.MAX_VALUE }
    }

    private fun loadNews(): List<NewsItem> {
        val feeds = listOf(
            "https://www.marca.com/rss/futbol.xml" to "MARCA"
        )
        return feeds.flatMap { (url, source) ->
            runCatching { parseNewsFeed(requestXml(url), source) }.getOrDefault(emptyList())
        }.distinctBy { it.title }.take(12)
    }

    private fun loadNewsIfStale(): List<NewsItem> {
        val now = System.currentTimeMillis()
        if (cachedNews.isNotEmpty() && now - newsCachedAt < newsCacheDurationMillis) {
            return cachedNews
        }
        val fresh = loadNews()
        if (fresh.isNotEmpty()) {
            cachedNews = fresh
            newsCachedAt = now
        }
        return cachedNews
    }

    private fun requestXml(urlValue: String): InputStream {
        val connection = URL(urlValue).openConnection() as HttpURLConnection
        connection.connectTimeout = 6_000
        connection.readTimeout = 8_000
        connection.requestMethod = "GET"
        connection.setRequestProperty("Accept", "application/rss+xml, application/xml, text/xml")
        connection.setRequestProperty("User-Agent", "NextGoal/1.0 Android")
        if (connection.responseCode !in 200..299) {
            connection.disconnect()
            error("Feed de noticias respondió ${connection.responseCode}")
        }
        return connection.inputStream
    }

    private fun parseNewsFeed(input: InputStream, source: String): List<NewsItem> {
        val parser = Xml.newPullParser()
        parser.setInput(input, null)
        val news = mutableListOf<NewsItem>()
        var tag: String? = null
        var insideEntry = false
        var title = ""
        var description = ""
        var link = ""
        var published = ""
        var imageUrl = ""
        var event = parser.eventType
        try {
            while (event != XmlPullParser.END_DOCUMENT && news.size < 8) {
                when (event) {
                    XmlPullParser.START_TAG -> {
                        val name = parser.name.lowercase(NEXT_GOAL_LOCALE)
                        if (name == "item" || name == "entry") {
                            insideEntry = true
                            title = ""
                            description = ""
                            link = ""
                            published = ""
                            imageUrl = ""
                        } else if (insideEntry) {
                            if (name == "link") {
                                link = parser.getAttributeValue(null, "href").orEmpty().ifBlank { link }
                            }
                            if (name == "enclosure" || name.endsWith(":content") || name.endsWith(":thumbnail")) {
                                imageUrl = parser.getAttributeValue(null, "url").orEmpty().ifBlank { imageUrl }
                            }
                            tag = name
                        }
                    }
                    XmlPullParser.TEXT, XmlPullParser.CDSECT, XmlPullParser.ENTITY_REF -> {
                        if (insideEntry) {
                            when (tag) {
                                "title" -> title += parser.text
                                "description", "summary", "content" -> description += parser.text
                                "link", "guid" -> link += parser.text
                                "pubdate", "published", "updated" -> published += parser.text
                                else -> if (tag?.endsWith(":encoded") == true) description += parser.text
                            }
                        }
                    }
                    XmlPullParser.END_TAG -> {
                        val name = parser.name.lowercase(NEXT_GOAL_LOCALE)
                        if (name == "item" || name == "entry") {
                            if (title.isNotBlank()) {
                                val image = imageUrl.ifBlank { extractImageUrl(description).orEmpty() }
                                news += NewsItem(
                                    category = source,
                                    title = cleanMarkup(title),
                                    summary = cleanMarkup(description).ifBlank { "Última información del mundo del fútbol." },
                                    timeLabel = formatNewsDate(published),
                                    imageUrl = image,
                                    sourceUrl = link.trim()
                                )
                            }
                            insideEntry = false
                            tag = null
                        } else if (insideEntry) {
                            tag = null
                        }
                    }
                }
                event = parser.next()
            }
        } finally {
            input.close()
        }
        return news
    }

    private fun cleanMarkup(value: String): String {
        return value.replace(Regex("<[^>]*>"), " ")
            .replace(Regex("\\s+"), " ")
            .trim()
    }

    private fun extractImageUrl(value: String): String? {
        return Regex("<img[^>]+src=[\\\"']([^\\\"']+)", RegexOption.IGNORE_CASE)
            .find(value)
            ?.groupValues
            ?.getOrNull(1)
            ?.trim()
            ?.takeIf { it.startsWith("http://") || it.startsWith("https://") }
    }

    private fun formatNewsDate(value: String): String {
        if (value.isBlank()) return "Ahora"
        val patterns = listOf(
            "EEE, dd MMM yyyy HH:mm:ss z",
            "yyyy-MM-dd'T'HH:mm:ssXXX",
            "yyyy-MM-dd'T'HH:mm:ss'Z'"
        )
        val parsed = patterns.firstNotNullOfOrNull { pattern ->
            runCatching { SimpleDateFormat(pattern, Locale.ENGLISH).parse(value) }.getOrNull()
        }
        return if (parsed == null) "Ahora" else {
            val minutes = ((Date().time - parsed.time) / 60_000L).coerceAtLeast(0)
            when {
                minutes < 60 -> "Hace ${minutes.coerceAtLeast(1)} min"
                minutes < 1_440 -> "Hace ${minutes / 60} h"
                else -> "Hace ${minutes / 1_440} días"
            }
        }
    }

    private fun parseTeam(raw: JSONObject): Team {
        val id = raw.optInt("id", 0)
        val name = raw.optString("name").ifBlank { "Equipo" }
        return Team(
            id = id,
            name = name,
            shortName = raw.optString("shortName").ifBlank { name },
            crestUrl = raw.optString("crest").ifBlank { null }
        )
    }

    private fun formatKickoff(utcDate: String, phase: MatchPhase): String {
        if (phase == MatchPhase.FINISHED) return "FINALIZADO"
        if (phase == MatchPhase.LIVE) return "EN VIVO"
        if (utcDate.isBlank() || Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return "20:00"
        return runCatching {
            val zone = NEXT_GOAL_TIME_ZONE
            val timestamp = parseTimestamp(utcDate)
            if (timestamp <= 0L) return@runCatching "20:00"
            val kickoff = Instant.ofEpochMilli(timestamp).atZone(zone)
            val pattern = if (kickoff.toLocalDate() == LocalDate.now(zone)) "HH:mm" else "dd/MM · HH:mm"
            kickoff.format(DateTimeFormatter.ofPattern(pattern, NEXT_GOAL_LOCALE))
        }.getOrDefault("20:00")
    }
}

private fun parseTimestamp(value: String): Long {
    if (value.isBlank()) return 0L
    return runCatching { Instant.parse(value).toEpochMilli() }.getOrElse {
        val patterns = listOf(
            "yyyy-MM-dd'T'HH:mm:ss.SSSXXX",
            "yyyy-MM-dd'T'HH:mm:ssXXX",
            "yyyy-MM-dd HH:mm:ss"
        )
        patterns.firstNotNullOfOrNull { pattern ->
            runCatching { SimpleDateFormat(pattern, Locale.ENGLISH).parse(value)?.time }.getOrNull()
        } ?: 0L
    }
}

private fun estimateLiveMinute(utcDate: String): Int? {
    val kickoff = parseTimestamp(utcDate)
    if (kickoff <= 0L) return null
    val elapsedMinutes = ((System.currentTimeMillis() - kickoff) / 60_000L).toInt()
    return elapsedMinutes.coerceIn(1, 120)
}

private val matchComparator = compareBy<Match> {
    when (it.phase) {
        MatchPhase.LIVE -> 0
        MatchPhase.UPCOMING -> 1
        MatchPhase.FINISHED -> 2
    }
}.thenBy { if (it.kickoffMillis == 0L) Long.MAX_VALUE else it.kickoffMillis }

private fun JSONObject.nullableInt(key: String): Int? {
    if (!has(key) || isNull(key)) return null
    return optInt(key)
}

private fun JSONObject.nullableStatInt(key: String): Int? {
    if (!has(key) || isNull(key)) return null
    return when (val value = opt(key)) {
        is Number -> value.toInt()
        is JSONObject -> value.optString("value").replace("%", "").trim().toDoubleOrNull()?.toInt()
        else -> value?.toString()?.replace("%", "")?.trim()?.toDoubleOrNull()?.toInt()
    }
}
