package com.nextgoal.app.ui.matches

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.TextButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.nextgoal.app.data.model.MatchStatus
import com.nextgoal.app.data.model.MatchUi
import com.nextgoal.app.ui.common.UiState
import com.nextgoal.app.ui.common.ViewModelFactories
import com.nextgoal.app.ui.components.FullScreenError
import com.nextgoal.app.ui.components.FullScreenLoading
import com.nextgoal.app.ui.components.MatchRowCard
import com.nextgoal.app.ui.theme.NextGoalBlue
import com.nextgoal.app.ui.theme.TextPrimary
import com.nextgoal.app.ui.theme.TextSecondary
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

private val filters = listOf("Todos", "En vivo", "Próximos", "Finalizados")

@Composable
fun MatchesScreen(
    onBack: () -> Unit,
    onMatchClick: (Int) -> Unit,
    viewModel: MatchesViewModel = viewModel(factory = ViewModelFactories.Matches)
) {
    val state by viewModel.state.collectAsState()
    var selectedFilter by rememberSaveable { mutableIntStateOf(0) }
    var rangeOffset by rememberSaveable { mutableIntStateOf(0) }
    // The API accepts at most ten calendar days per request. Adjacent ten-day
    // windows let the user browse the full available history without gaps.
    val from = remember(rangeOffset) { LocalDate.now().minusDays(3).plusDays(rangeOffset * 10L) }
    val to = remember(rangeOffset) { LocalDate.now().plusDays(6).plusDays(rangeOffset * 10L) }

    LaunchedEffect(from, to) { viewModel.loadRange(from, to) }

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(start = 16.dp, end = 8.dp, top = 14.dp, bottom = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            androidx.compose.material3.TextButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null, tint = NextGoalBlue)
                Text("Volver", color = NextGoalBlue, fontSize = 12.sp)
            }
            Column(Modifier.weight(1f)) {
                Text("Partidos", color = TextPrimary, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleLarge)
                Text("Resultados y calendario", color = TextSecondary, fontSize = 12.sp)
            }
            IconButton(onClick = viewModel::refresh) {
                Icon(Icons.Filled.Refresh, contentDescription = "Actualizar partidos", tint = NextGoalBlue)
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            TextButton(onClick = { rangeOffset -= 1; selectedFilter = 3 }) {
                Icon(Icons.Filled.ChevronLeft, contentDescription = null, tint = NextGoalBlue)
                Text("Anteriores", color = NextGoalBlue, fontSize = 11.sp)
            }
            Text(
                "${from.format(DateTimeFormatter.ofPattern("d MMM", Locale.forLanguageTag("es")))} — ${to.format(DateTimeFormatter.ofPattern("d MMM", Locale.forLanguageTag("es")))}",
                color = TextPrimary, fontWeight = FontWeight.SemiBold, fontSize = 12.sp
            )
            TextButton(onClick = { rangeOffset += 1; selectedFilter = 0 }) {
                Text("Siguientes", color = NextGoalBlue, fontSize = 11.sp)
                Icon(Icons.Filled.ChevronRight, contentDescription = null, tint = NextGoalBlue)
            }
        }

        ScrollableTabRow(
            selectedTabIndex = selectedFilter,
            edgePadding = 12.dp,
            containerColor = MaterialTheme.colorScheme.background,
            contentColor = NextGoalBlue
        ) {
            filters.forEachIndexed { index, label ->
                Tab(
                    selected = selectedFilter == index,
                    onClick = { selectedFilter = index },
                    text = { Text(label, maxLines = 1, fontSize = 12.sp) }
                )
            }
        }

        when (val result = state.result) {
            is UiState.Loading -> FullScreenLoading(Modifier.weight(1f).fillMaxWidth())
            is UiState.Error -> FullScreenError(result.message, onRetry = viewModel::refresh, modifier = Modifier.weight(1f).fillMaxWidth())
            is UiState.Success -> {
                val matches = result.data.filterForFilter(selectedFilter)
                if (matches.isEmpty()) {
                    val message = when (selectedFilter) {
                        1 -> "No hay partidos en vivo en este periodo."
                        2 -> "No hay próximos partidos programados en este periodo."
                        3 -> "No hay resultados finalizados en este periodo. Prueba con la semana anterior."
                        else -> "No hay partidos publicados para estas fechas."
                    }
                    Column(
                        modifier = Modifier.weight(1f).fillMaxWidth().padding(24.dp),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(message, color = TextSecondary, fontSize = 14.sp)
                    }
                } else {
                    LazyColumn(modifier = Modifier.weight(1f).fillMaxWidth()) {
                        items(matches, key = MatchUi::id) { match ->
                            MatchRowCard(match = match, onClick = { onMatchClick(match.id) })
                        }
                    }
                }
            }
        }
    }
}

private fun List<MatchUi>.filterForFilter(filter: Int): List<MatchUi> = when (filter) {
    1 -> filter { it.status == MatchStatus.LIVE || it.status == MatchStatus.PAUSED }
    2 -> filter { it.status == MatchStatus.SCHEDULED }
    3 -> filter { it.status == MatchStatus.FINISHED }
    else -> this
}
