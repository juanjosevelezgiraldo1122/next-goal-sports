package com.nextgoal.app.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.navigation.NavType
import com.nextgoal.app.ui.components.NextGoalBottomBar
import com.nextgoal.app.ui.home.HomeScreen
import com.nextgoal.app.ui.leagues.LeaguesScreen
import com.nextgoal.app.ui.matchdetail.MatchDetailScreen
import com.nextgoal.app.ui.matches.MatchesScreen
import com.nextgoal.app.ui.news.NewsScreen
import com.nextgoal.app.ui.profile.ProfileScreen
import com.nextgoal.app.ui.splash.SplashScreen
import com.nextgoal.app.ui.search.SearchScreen
import com.nextgoal.app.ui.common.ViewModelFactories

private val screensWithBottomBar = setOf(
    Screen.Home.route, Screen.Matches.route, Screen.Leagues.route, Screen.News.route, Screen.Profile.route
)

@Composable
fun NextGoalNavGraph(navController: NavHostController = rememberNavController()) {
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route

    Scaffold(
        bottomBar = {
            if (currentRoute in screensWithBottomBar) {
                NextGoalBottomBar(navController = navController, currentRoute = currentRoute)
            }
        }
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Splash.route,
            modifier = Modifier.padding(
                bottom = if (currentRoute in screensWithBottomBar) padding.calculateBottomPadding() else 0.dp
            )
        ) {
            composable(Screen.Splash.route) {
                SplashScreen(onFinished = {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Splash.route) { inclusive = true }
                    }
                })
            }
            composable(Screen.Home.route) {
                HomeScreen(
                    onMatchClick = { matchId -> navController.navigate(Screen.MatchDetail.createRoute(matchId)) },
                    onSeeAllMatches = { navController.navigate(Screen.Matches.route) },
                    onLeagueChipClick = { navController.navigate(Screen.Leagues.route) },
                    onSearchClick = { navController.navigate(Screen.Search.route) }
                )
            }
            composable(Screen.Matches.route) {
                MatchesScreen(
                    onBack = { if (!navController.popBackStack()) navController.navigate(Screen.Home.route) },
                    onMatchClick = { matchId -> navController.navigate(Screen.MatchDetail.createRoute(matchId)) }
                )
            }
            composable(Screen.Leagues.route) {
                LeaguesScreen(onSearchClick = { navController.navigate(Screen.Search.route) })
            }
            composable(Screen.News.route) {
                NewsScreen(onSearchClick = { navController.navigate(Screen.Search.route) })
            }
            composable(Screen.Profile.route) {
                ProfileScreen(onSearchClick = { navController.navigate(Screen.Search.route) })
            }
            composable(Screen.Search.route) {
                SearchScreen(onBack = { navController.popBackStack() })
            }
            composable(
                route = Screen.MatchDetail.route,
                arguments = listOf(navArgument("matchId") { type = NavType.IntType })
            ) { backStack ->
                val matchId = backStack.arguments?.getInt("matchId") ?: return@composable
                MatchDetailScreen(matchId = matchId, onBack = { navController.popBackStack() })
            }
        }
    }
}
