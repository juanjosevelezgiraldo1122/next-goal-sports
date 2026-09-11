package com.nextgoal.app

object SampleData {
    private const val crestBase = "https://crests.football-data.org"

    val realMadrid = Team(86, "Real Madrid", "Real Madrid", "$crestBase/86.svg")
    val barcelona = Team(81, "FC Barcelona", "Barcelona", "$crestBase/81.svg")
    val arsenal = Team(57, "Arsenal", "Arsenal", "$crestBase/57.svg")
    val chelsea = Team(61, "Chelsea", "Chelsea", "$crestBase/61.svg")
    val manCity = Team(65, "Manchester City", "Man City", "$crestBase/65.svg")
    val liverpool = Team(64, "Liverpool", "Liverpool", "$crestBase/64.svg")
    val bayern = Team(5, "Bayern München", "Bayern", "$crestBase/5.svg")
    val dortmund = Team(4, "Borussia Dortmund", "Dortmund", "$crestBase/4.svg")
    val america = Team(
        1001,
        "América",
        "América",
        "https://upload.wikimedia.org/wikipedia/commons/f/f4/Club_Am%C3%A9rica_Logo.svg"
    )
    val guadalajara = Team(
        1002,
        "Guadalajara",
        "Guadalajara",
        "https://upload.wikimedia.org/wikipedia/en/1/1c/Club_Deportivo_Guadalajara_crest.svg"
    )
    val tigres = Team(
        1003,
        "Tigres UANL",
        "Tigres",
        "https://upload.wikimedia.org/wikipedia/en/8/8e/Tigres_UANL_logo.svg"
    )
    val monterrey = Team(
        1004,
        "Monterrey",
        "Monterrey",
        "https://upload.wikimedia.org/wikipedia/en/4/4c/Club_de_F%C3%BAtbol_Monterrey_crest.svg"
    )
    val pumas = Team(
        1005,
        "Pumas UNAM",
        "Pumas",
        "https://upload.wikimedia.org/wikipedia/en/4/49/Club_Universidad_Nacional_A.C._logo.svg"
    )
    val cruzAzul = Team(
        1006,
        "Cruz Azul",
        "Cruz Azul",
        "https://upload.wikimedia.org/wikipedia/en/6/6e/Cruz_Azul_FC_logo.svg"
    )
    val toluca = Team(
        1007,
        "Toluca",
        "Toluca",
        "https://upload.wikimedia.org/wikipedia/en/1/1e/Deportivo_Toluca_F.C._logo.svg"
    )
    val leon = Team(
        1008,
        "León",
        "León",
        "https://upload.wikimedia.org/wikipedia/en/7/7e/Club_Le%C3%B3n_logo.svg"
    )

    val dashboard = DashboardData(
        matches = listOf(
            Match(
                id = 9001,
                competition = "Liga de Campeones",
                competitionShort = "UCL",
                home = realMadrid,
                away = barcelona,
                homeScore = 2,
                awayScore = 1,
                minute = 74,
                phase = MatchPhase.LIVE,
                venue = "Santiago Bernabéu",
                stats = MatchStats(
                    homePossession = 58,
                    awayPossession = 42,
                    homeShotsOnTarget = 6,
                    awayShotsOnTarget = 3,
                    homeShots = 14,
                    awayShots = 9,
                    homeCorners = 8,
                    awayCorners = 4,
                    homeFouls = 10,
                    awayFouls = 12
                ),
                events = listOf(
                    MatchEvent(61, "GOAL", "Gol de Jude Bellingham", "Asistencia de Modric", "Real Madrid"),
                    MatchEvent(45, "YELLOW_CARD", "Tarjeta amarilla", "Gavi por falta fuerte", "FC Barcelona")
                )
            ),
            Match(
                id = 9002,
                competition = "Liga MX",
                competitionShort = "LMX",
                home = america,
                away = guadalajara,
                kickoffLabel = "14:00",
                venue = "Estadio Azteca"
            ),
            Match(
                id = 9003,
                competition = "Premier League",
                competitionShort = "EPL",
                home = arsenal,
                away = chelsea,
                kickoffLabel = "16:30",
                venue = "Emirates Stadium"
            ),
            Match(
                id = 9004,
                competition = "La Liga",
                competitionShort = "LL",
                home = manCity,
                away = liverpool,
                kickoffLabel = "20:00",
                venue = "Etihad Stadium"
            ),
            Match(
                id = 9005,
                competition = "Bundesliga",
                competitionShort = "BL",
                home = bayern,
                away = dortmund,
                homeScore = 3,
                awayScore = 2,
                phase = MatchPhase.FINISHED,
                kickoffLabel = "FINALIZADO",
                venue = "Allianz Arena"
            )
        ),
        standings = mapOf(
            "LIGA MX" to listOf(
                StandingRow(1, america, 12, 15, 28, listOf("G", "G", "E", "G", "G")),
                StandingRow(2, tigres, 12, 11, 25, listOf("G", "G", "P", "G", "E")),
                StandingRow(3, monterrey, 12, 8, 24, listOf("G", "E", "G", "G", "P")),
                StandingRow(4, guadalajara, 12, 4, 21, listOf("E", "G", "G", "P", "G")),
                StandingRow(5, pumas, 12, 2, 18, listOf("P", "G", "E", "G", "E")),
                StandingRow(6, cruzAzul, 12, 0, 17, listOf("E", "P", "G", "E", "G")),
                StandingRow(7, toluca, 12, -2, 15, listOf("P", "E", "G", "P", "E")),
                StandingRow(8, leon, 12, -4, 14, listOf("E", "P", "P", "G", "E"))
            ),
            "PREMIER LEAGUE" to listOf(
                StandingRow(1, manCity, 12, 22, 31),
                StandingRow(2, liverpool, 12, 18, 29),
                StandingRow(3, arsenal, 12, 14, 26),
                StandingRow(4, chelsea, 12, 9, 23),
                StandingRow(5, bayern, 12, 12, 21)
            ),
            "LIGA DE CAMPEONES" to listOf(
                StandingRow(1, realMadrid, 4, 9, 12),
                StandingRow(2, barcelona, 4, 5, 9),
                StandingRow(3, bayern, 4, 2, 7),
                StandingRow(4, dortmund, 4, -3, 4),
                StandingRow(5, arsenal, 4, -6, 1)
            )
        ),
        news = listOf(
            NewsItem(
                "FÚTBOL MEXICANO",
                "¡Clásico de Clásicos! América vs Guadalajara encienden la jornada",
                "Los dos colosos del fútbol nacional se ven las caras en un duelo que definirá el fin de semana.",
                "Hace 2 horas",
                "https://images.unsplash.com/photo-1579952363873-27f3bade9f55?auto=format&fit=crop&w=1200&q=80"
            ),
            NewsItem(
                "UCL",
                "Sorteo de Champions: Así quedaron los emparejamientos de octavos",
                "El camino a la gran final ya tiene protagonistas.",
                "Hace 5 horas · Leer 3 min",
                "https://images.unsplash.com/photo-1522778119026-d647f0596c20?auto=format&fit=crop&w=600&q=80"
            ),
            NewsItem(
                "TRANSFERENCIA",
                "Rumores: ¿Nuevo destino para Mbappé? El astro francés redefine su futuro",
                "Las últimas novedades del mercado internacional.",
                "Hace 8 horas · Leer 4 min",
                "https://images.unsplash.com/photo-1579952363873-27f3bade9f55?auto=format&fit=crop&w=600&q=80"
            )
        )
    )
}
