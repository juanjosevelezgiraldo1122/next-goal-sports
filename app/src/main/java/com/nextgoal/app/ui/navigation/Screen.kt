package com.nextgoal.app.ui.navigation

sealed class Screen(val route: String) {
    data object Splash : Screen("splash")
    data object Home : Screen("home")
    data object Matches : Screen("matches")
    data object Leagues : Screen("leagues")
    data object News : Screen("news")
    data object Profile : Screen("profile")
    data object Search : Screen("search")

    data object MatchDetail : Screen("match_detail/{matchId}") {
        fun createRoute(matchId: Int) = "match_detail/$matchId"
    }
}

data class BottomNavItem(
    val screen: Screen,
    val label: String
)

val bottomNavItems = listOf(
    BottomNavItem(Screen.Home, "Inicio"),
    BottomNavItem(Screen.Matches, "Partidos"),
    BottomNavItem(Screen.Leagues, "Ligas"),
    BottomNavItem(Screen.News, "Noticias"),
    BottomNavItem(Screen.Profile, "Perfil"),
)
