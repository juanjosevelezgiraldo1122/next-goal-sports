package com.nextgoal.app.ui.common

import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.nextgoal.app.di.AppGraph
import com.nextgoal.app.ui.home.HomeViewModel
import com.nextgoal.app.ui.leagues.LeaguesViewModel
import com.nextgoal.app.ui.matchdetail.MatchDetailViewModel
import com.nextgoal.app.ui.news.NewsViewModel
import com.nextgoal.app.ui.profile.ProfileViewModel
import com.nextgoal.app.ui.matches.MatchesViewModel
import com.nextgoal.app.ui.search.SearchViewModel

object ViewModelFactories {

    val Home = viewModelFactory {
        initializer { HomeViewModel(AppGraph.footballRepository) }
    }

    val Matches = viewModelFactory {
        initializer { MatchesViewModel(AppGraph.footballRepository) }
    }

    fun matchDetail(matchId: Int) = viewModelFactory {
        initializer { MatchDetailViewModel(AppGraph.footballRepository, matchId) }
    }

    val Leagues = viewModelFactory {
        initializer { LeaguesViewModel(AppGraph.footballRepository) }
    }

    val News = viewModelFactory {
        initializer { NewsViewModel(AppGraph.newsRepository) }
    }

    val Profile = viewModelFactory {
        initializer { ProfileViewModel(AppGraph.footballRepository, AppGraph.DEFAULT_TEAM_ID) }
    }

    val Search = viewModelFactory {
        initializer { SearchViewModel(AppGraph.footballRepository) }
    }
}
