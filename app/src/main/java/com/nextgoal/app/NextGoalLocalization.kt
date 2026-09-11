package com.nextgoal.app

import java.time.ZoneId
import java.util.Locale

val NEXT_GOAL_TIME_ZONE: ZoneId = ZoneId.of("America/Bogota")
val NEXT_GOAL_LOCALE: Locale = Locale("es", "CO")

fun spanishCompetitionName(value: String): String {
    val normalized = value.trim().lowercase(Locale.ROOT)
    return when {
        normalized.contains("champions") -> "Liga de Campeones"
        normalized.contains("premier") -> "Premier League"
        normalized.contains("la liga") || normalized == "laliga" -> "La Liga"
        normalized.contains("bundesliga") -> "Bundesliga"
        normalized.contains("serie a") -> "Serie A"
        normalized.contains("ligue 1") -> "Ligue 1"
        normalized.contains("eredivisie") -> "Eredivisie"
        normalized.contains("primeira") -> "Primeira Liga"
        normalized.contains("championship") -> "Championship"
        normalized.contains("brasileir") -> "Brasileirao"
        normalized.contains("liga mx") -> "Liga MX"
        normalized.contains("copa libertadores") -> "Copa Libertadores"
        normalized.contains("copa sudamericana") -> "Copa Sudamericana"
        normalized.contains("club world") || normalized.contains("mundial de clubes") -> "Mundial de Clubes"
        normalized.contains("world cup") || normalized.contains("copa del mundo") -> "Copa del Mundo"
        else -> value.trim().ifBlank { "Competición" }
    }
}

fun spanishPosition(value: String): String {
    val normalized = value.trim().lowercase(Locale.ROOT)
    return when {
        normalized.contains("goalkeeper") || normalized == "keeper" -> "Portero"
        normalized.contains("centre back") || normalized.contains("center back") || normalized.contains("defender") -> "Defensa"
        normalized.contains("full back") || normalized.contains("wing back") -> "Lateral"
        normalized.contains("defensive midfield") -> "Mediocampista defensivo"
        normalized.contains("central midfield") || normalized.contains("midfielder") -> "Mediocampista"
        normalized.contains("attacking midfield") -> "Mediocampista ofensivo"
        normalized.contains("winger") || normalized.contains("left wing") || normalized.contains("right wing") -> "Extremo"
        normalized.contains("forward") || normalized.contains("striker") -> "Delantero"
        normalized.contains("manager") || normalized.contains("coach") -> "Entrenador"
        else -> value.trim().ifBlank { "Posición no publicada" }
    }
}

fun spanishCountry(value: String): String {
    return when (value.trim().lowercase(Locale.ROOT)) {
        "england" -> "Inglaterra"
        "scotland" -> "Escocia"
        "wales" -> "Gales"
        "spain" -> "España"
        "france" -> "Francia"
        "germany" -> "Alemania"
        "italy" -> "Italia"
        "netherlands" -> "Países Bajos"
        "belgium" -> "Bélgica"
        "portugal" -> "Portugal"
        "brazil" -> "Brasil"
        "argentina" -> "Argentina"
        "mexico" -> "México"
        "colombia" -> "Colombia"
        "uruguay" -> "Uruguay"
        "chile" -> "Chile"
        "croatia" -> "Croacia"
        "switzerland" -> "Suiza"
        "united states", "usa" -> "Estados Unidos"
        else -> value.trim().ifBlank { "País no publicado" }
    }
}
