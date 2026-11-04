package com.nextgoal.app.di

import com.nextgoal.app.data.remote.RetrofitInstance
import com.nextgoal.app.data.repository.FootballRepository
import com.nextgoal.app.data.repository.NewsRepository

/** Tiny manual DI graph — keeps the project buildable without Hilt/KSP setup. */
object AppGraph {
    val footballRepository: FootballRepository by lazy { FootballRepository(RetrofitInstance.api) }
    val newsRepository: NewsRepository by lazy { NewsRepository() }

    /** football-data.org team id for Real Madrid — used as the default squad shown on the Perfil tab. */
    const val DEFAULT_TEAM_ID = 86
}
