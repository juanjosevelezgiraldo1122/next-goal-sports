package com.nextgoal.app.ui.leagues

import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.nextgoal.app.ui.common.UiState
import com.nextgoal.app.ui.common.ViewModelFactories
import com.nextgoal.app.ui.components.FullScreenError
import com.nextgoal.app.ui.components.FullScreenLoading
import com.nextgoal.app.ui.components.NextGoalTopBar
import com.nextgoal.app.ui.components.InfoBanner
import com.nextgoal.app.ui.components.TeamCrest
import com.nextgoal.app.ui.theme.BgCard
import com.nextgoal.app.ui.theme.BgCardAlt
import com.nextgoal.app.ui.theme.BorderSubtle
import com.nextgoal.app.ui.theme.NextGoalBlue
import com.nextgoal.app.ui.theme.PositiveGreen
import com.nextgoal.app.ui.theme.TextPrimary
import com.nextgoal.app.ui.theme.TextSecondary

@Composable
fun LeaguesScreen(onSearchClick: () -> Unit, viewModel: LeaguesViewModel = viewModel(factory = ViewModelFactories.Leagues)) {
    val selected by viewModel.selected.collectAsState()
    val state by viewModel.state.collectAsState()

    Column(modifier = Modifier.fillMaxSize().background(com.nextgoal.app.ui.theme.BgDeep)) {
        NextGoalTopBar(onSearchClick = onSearchClick)
        Text("Ligas y tablas", style = MaterialTheme.typography.headlineMedium, modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp))
        Text("Clasificación de las competiciones disponibles", color = TextSecondary, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(horizontal = 16.dp, vertical = 2.dp))
        Spacer(Modifier.height(14.dp))

        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(viewModel.competitions) { comp ->
                val isSelected = comp.code == selected
                Surface(
                    onClick = { viewModel.selectCompetition(comp.code) },
                    shape = RoundedCornerShape(50),
                    color = if (isSelected) NextGoalBlue else BgCard,
                    border = if (isSelected) null else BorderStroke(1.dp, BorderSubtle)
                ) {
                    Text(
                        comp.displayName,
                        color = if (isSelected) com.nextgoal.app.ui.theme.BgDeep else TextSecondary,
                        style = MaterialTheme.typography.labelLarge,
                        modifier = Modifier.padding(horizontal = 15.dp, vertical = 14.dp)
                    )
                }
            }
        }
        Spacer(Modifier.height(16.dp))

        when (val s = state) {
            is UiState.Loading -> FullScreenLoading(Modifier.fillMaxSize())
            is UiState.Error -> FullScreenError(s.message, onRetry = { viewModel.selectCompetition(selected) })
            is UiState.Success -> Column(Modifier.weight(1f)) {
                if (s.data.isEmpty()) {
                    InfoBanner("Esta competición no tiene una tabla publicada por la fuente actual.", Modifier.padding(16.dp))
                } else {
                    StandingsTable(rows = s.data, modifier = Modifier.weight(1f))
                }
                InfoBanner(
                    "La fuente actual no incluye zonas de clasificación o descenso; solo se muestran posiciones y datos publicados.",
                    Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }
        }
    }
}

@Composable
private fun StandingsTable(rows: List<com.nextgoal.app.data.model.StandingRowUi>, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(BgCard)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("#", color = TextSecondary, fontSize = 11.sp, modifier = Modifier.width(24.dp))
            Text("CLUB", color = TextSecondary, fontSize = 11.sp, modifier = Modifier.weight(1f))
            Text("PJ", color = TextSecondary, fontSize = 11.sp, modifier = Modifier.width(32.dp))
            Text("DG", color = TextSecondary, fontSize = 11.sp, modifier = Modifier.width(32.dp))
            Text("PTS", color = TextSecondary, fontSize = 11.sp, modifier = Modifier.width(36.dp))
        }
        LazyColumn(modifier = Modifier.weight(1f)) {
            items(rows) { row ->
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        "${row.position}",
                        color = if (row.position == 1) NextGoalBlue else TextPrimary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        modifier = Modifier.width(24.dp)
                    )
                    Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                        TeamCrest(row.team.crestUrl, size = 22)
                        Spacer(Modifier.width(10.dp))
                        Text(row.team.shortName, color = TextPrimary, fontSize = 14.sp, maxLines = 1)
                    }
                    Text("${row.played}", color = TextSecondary, fontSize = 13.sp, modifier = Modifier.width(32.dp))
                    Text(
                        if (row.goalDifference >= 0) "+${row.goalDifference}" else "${row.goalDifference}",
                        color = if (row.goalDifference >= 0) PositiveGreen else TextSecondary,
                        fontSize = 13.sp,
                        modifier = Modifier.width(32.dp)
                    )
                    Text(
                        "${row.points}",
                        color = NextGoalBlue,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        modifier = Modifier.width(36.dp)
                    )
                }
            }
            item { Spacer(Modifier.height(12.dp)) }
        }
    }
}
