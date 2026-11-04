package com.nextgoal.app.data.repository

import com.nextgoal.app.data.model.NewsArticleUi

/**
 * football-data.org (our live-scores provider) does not offer a news feed, and a
 * free, reliable, structured football-news API without its own signup key doesn't
 * exist. Rather than silently faking "live" news, this repository ships a small set
 * of clearly-labelled sample articles so the News screen is fully wired end-to-end
 * (UI, navigation, state) and ready to plug a real source into.
 *
 * To go live: implement this same function against e.g. NewsAPI.org, GNews, or an
 * RSS feed (footballitalia, ESPN, etc.) — the UI layer needs zero changes since it
 * only depends on NewsArticleUi.
 */
class NewsRepository {

    fun getSampleArticles(): List<NewsArticleUi> = listOf(
        NewsArticleUi(
            id = 1,
            category = "FÚTBOL MEXICANO",
            title = "¡Clásico de Clásicos! América vs Guadalajara encienden la jornada del fin de semana",
            summary = "Los dos colosos del fútbol nacional se ven las caras en el Estadio Azteca en un duelo que podría definir el liderato.",
            timeAgo = "Hace 2 horas",
            imageUrl = null,
            isFeatured = true
        ),
        NewsArticleUi(
            id = 2,
            category = "UCL",
            title = "Sorteo de Champions: así quedaron los emparejamientos de octavos",
            summary = "La UEFA realizó el sorteo de los octavos de final con duelos de alto voltaje entre los favoritos al título.",
            timeAgo = "Hace 5 horas",
            imageUrl = null
        ),
        NewsArticleUi(
            id = 3,
            category = "TRANSFERENCIAS",
            title = "Rumores: ¿nuevo destino en el mercado para una de las grandes estrellas?",
            summary = "Distintos medios europeos apuntan a movimientos importantes de cara a la próxima ventana de transferencias.",
            timeAgo = "Hace 8 horas",
            imageUrl = null
        ),
        NewsArticleUi(
            id = 4,
            category = "LIGA MX",
            title = "La tabla se aprieta rumbo a la recta final del torneo",
            summary = "Varios equipos siguen con vida en la lucha por el liguilla tras una jornada llena de sorpresas.",
            timeAgo = "Hace 1 día",
            imageUrl = null
        )
    )
}
