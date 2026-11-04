package com.nextgoal.app.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.nextgoal.app.data.model.MatchStatus
import com.nextgoal.app.data.model.MatchUi
import com.nextgoal.app.data.repository.FEATURED_COMPETITIONS
import com.nextgoal.app.ui.common.UiState
import com.nextgoal.app.ui.common.ViewModelFactories
import com.nextgoal.app.ui.components.FeaturedLiveMatchCard
import com.nextgoal.app.ui.components.FullScreenError
import com.nextgoal.app.ui.components.FullScreenLoading
import com.nextgoal.app.ui.components.MatchRowCard
import com.nextgoal.app.ui.components.NextGoalTopBar
import com.nextgoal.app.ui.components.SectionHeader
import com.nextgoal.app.ui.theme.BgCardAlt
import com.nextgoal.app.ui.theme.BgCard
import com.nextgoal.app.ui.theme.NextGoalBlue
import com.nextgoal.app.ui.theme.TextPrimary

@Composable
fun HomeScreen(
    onMatchClick: (Int) -> Unit,
    onSeeAllMatches: () -> Unit,
    onLeagueChipClick: (String) -> Unit,
    onSearchClick: () -> Unit,
    viewModel: HomeViewModel = viewModel(factory = ViewModelFactories.Home)
) {
    val state by viewModel.state.collectAsState()

    Column(modifier = Modifier.fillMaxSize()) {
        NextGoalTopBar(onSearchClick = onSearchClick)
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 2.dp)
                .clip(RoundedCornerShape(16.dp)).background(BgCard)
                .clickable(onClick = onSearchClick).padding(horizontal = 14.dp, vertical = 13.dp),
            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
        ) {
            Icon(Icons.Default.Search, contentDescription = null, tint = NextGoalBlue)
            Text("Busca jugadores y equipos", color = com.nextgoal.app.ui.theme.TextSecondary, fontSize = 13.sp, modifier = Modifier.padding(start = 10.dp))
        }

        when (val s = state) {
            is UiState.Loading -> FullScreenLoading(Modifier.fillMaxSize())
            is UiState.Error -> FullScreenError(s.message, onRetry = { viewModel.refresh() })
            is UiState.Success -> HomeContent(
                matches = s.data,
                onMatchClick = onMatchClick,
                onSeeAllMatches = onSeeAllMatches,
                onLeagueChipClick = onLeagueChipClick
            )
        }
    }
}

@Composable
private fun HomeContent(
    matches: List<MatchUi>,
    onMatchClick: (Int) -> Unit,
    onSeeAllMatches: () -> Unit,
    onLeagueChipClick: (String) -> Unit
) {
    val liveMatches = matches.filter { it.status == MatchStatus.LIVE || it.status == MatchStatus.PAUSED }
    val upcoming = matches.filter { it.status == MatchStatus.SCHEDULED }.take(10)
    val recentResults = matches.filter { it.status == MatchStatus.FINISHED }
        .sortedByDescending { it.utcDate }
        .take(5)

    LazyColumn(modifier = Modifier.fillMaxSize()) {
        if (liveMatches.isNotEmpty()) {
            item {
                SectionHeader(
                    title = "● EN VIVO",
                    actionText = "Ver todos (${liveMatches.size})",
                    onActionClick = onSeeAllMatches
                )
                FeaturedLiveMatchCard(match = liveMatches.first(), onClick = { onMatchClick(liveMatches.first().id) })
                Spacer(Modifier.height(20.dp))
            }
        } else {
            item {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("No hay partidos en vivo en este momento", color = TextPrimary, fontWeight = FontWeight.SemiBold)
                }
                Spacer(Modifier.height(8.dp))
            }
        }

        item {
            SectionHeader(title = "LIGAS DESTACADAS")
            LazyRow(
                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(FEATURED_COMPETITIONS) { comp ->
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(BgCardAlt)
                            .clickable { onLeagueChipClick(comp.code) }
                            .padding(horizontal = 14.dp, vertical = 8.dp)
                    ) {
                        Text(comp.displayName, color = NextGoalBlue, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
            Spacer(Modifier.height(16.dp))
        }

        item {
            SectionHeader(title = "PARTIDOS PRÓXIMOS")
        }

        if (upcoming.isEmpty()) {
            item {
                Text(
                    "No hay partidos programados en los próximos días.",
                    color = TextPrimary,
                    modifier = Modifier.padding(16.dp)
                )
            }
        } else {
            items(upcoming) { match ->
                MatchRowCard(match = match, onClick = { onMatchClick(match.id) })
            }
        }

        item {
            SectionHeader(
                title = "ÚLTIMOS RESULTADOS",
                actionText = "Ver todos",
                onActionClick = onSeeAllMatches
            )
        }

        if (recentResults.isEmpty()) {
            item {
                Text(
                    "No hay resultados recientes en las competiciones disponibles.",
                    color = TextPrimary,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)
                )
            }
        } else {
            items(recentResults, key = { "finished-${it.id}" }) { match ->
                MatchRowCard(match = match, onClick = { onMatchClick(match.id) })
            }
        }

        item { Spacer(Modifier.height(24.dp)) }
    }
}
