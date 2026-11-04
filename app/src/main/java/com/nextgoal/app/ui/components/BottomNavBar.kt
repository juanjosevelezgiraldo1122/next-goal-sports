package com.nextgoal.app.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Timeline
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.nextgoal.app.ui.navigation.Screen
import com.nextgoal.app.ui.navigation.bottomNavItems
import com.nextgoal.app.ui.theme.BgDeep
import com.nextgoal.app.ui.theme.NextGoalBlue
import com.nextgoal.app.ui.theme.TextTertiary
import androidx.compose.ui.unit.dp

private fun iconFor(screen: Screen): ImageVector = when (screen) {
    Screen.Home -> Icons.Filled.Home
    Screen.Matches -> Icons.Filled.Timeline
    Screen.Leagues -> Icons.Filled.EmojiEvents
    Screen.News -> Icons.Filled.MenuBook
    Screen.Profile -> Icons.Filled.Person
    else -> Icons.Filled.Home
}

@Composable
fun NextGoalBottomBar(navController: NavHostController, currentRoute: String?) {
    NavigationBar(containerColor = BgDeep, tonalElevation = 2.dp) {
        bottomNavItems.forEach { item ->
            val selected = currentRoute == item.screen.route
            NavigationBarItem(
                selected = selected,
                onClick = {
                    if (!selected) {
                        navController.navigate(item.screen.route) {
                            popUpTo(Screen.Home.route)
                            launchSingleTop = true
                        }
                    }
                },
                icon = { Icon(iconFor(item.screen), contentDescription = item.label) },
                label = { Text(item.label) },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = NextGoalBlue,
                    selectedTextColor = NextGoalBlue,
                    unselectedIconColor = TextTertiary,
                    unselectedTextColor = TextTertiary,
                    indicatorColor = NextGoalBlue.copy(alpha = 0.14f)
                )
            )
        }
    }
}
