package com.nextgoal.app.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import com.nextgoal.app.ui.components.InfoBanner
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

    Column(modifier = Modifier.fillMaxSize().background(com.nextgoal.app.ui.theme.BgDeep)) {
        NextGoalTopBar(onSearchClick = onSearchClick)
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp)
                .clip(RoundedCornerShape(17.dp)).background(BgCardAlt)
                .border(1.dp, com.nextgoal.app.ui.theme.BorderSubtle, RoundedCornerShape(17.dp))
                .clickable(onClick = onSearchClick).padding(horizontal = 15.dp, vertical = 15.dp),
            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
        ) {
            Icon(Icons.Default.Search, contentDescription = null, tint = NextGoalBlue)
            Text("Busca jugadores y equipos", color = com.nextgoal.app.ui.theme.TextSecondary, style = androidx.compose.material3.MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(start = 10.dp))
            androidx.compose.foundation.layout.Spacer(Modifier.weight(1f))
            Text("↗", color = com.nextgoal.app.ui.theme.NextGoalBlueDeep, fontSize = 18.sp, fontWeight = FontWeight.Bold)
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

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(top = 6.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        if (liveMatches.isNotEmpty()) {
            item {
                SectionHeader(
                    title = "EN VIVO",
                    actionText = "Ver todos (${liveMatches.size})",
                    onActionClick = onSeeAllMatches
                )
                FeaturedLiveMatchCard(match = liveMatches.first(), onClick = { onMatchClick(liveMatches.first().id) })
                Spacer(Modifier.height(20.dp))
            }
        } else {
            item {
                InfoBanner("No hay partidos en vivo en este momento.", Modifier.padding(horizontal = 16.dp, vertical = 6.dp))
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
                            .clip(RoundedCornerShape(50))
                            .background(BgCardAlt)
                            .border(1.dp, com.nextgoal.app.ui.theme.BorderSubtle, RoundedCornerShape(50))
                            .clickable { onLeagueChipClick(comp.code) }
                            .padding(horizontal = 14.dp, vertical = 11.dp),
                        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                    ) {
                        androidx.compose.foundation.layout.Box(Modifier.size(7.dp).background(NextGoalBlue, RoundedCornerShape(50)))
                        Text(comp.displayName, color = TextPrimary, style = androidx.compose.material3.MaterialTheme.typography.labelLarge, modifier = Modifier.padding(start = 8.dp))
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
                InfoBanner("No hay partidos programados en los próximos días.", Modifier.padding(horizontal = 16.dp, vertical = 4.dp))
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
                InfoBanner("No hay resultados recientes en las competiciones disponibles.", Modifier.padding(horizontal = 16.dp, vertical = 4.dp))
            }
        } else {
            items(recentResults, key = { "finished-${it.id}" }) { match ->
                MatchRowCard(match = match, onClick = { onMatchClick(match.id) })
            }
        }

        item { Spacer(Modifier.height(24.dp)) }
    }
}
