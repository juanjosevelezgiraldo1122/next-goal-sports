package com.nextgoal.app.ui.matchdetail

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Circle
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.nextgoal.app.data.model.EventKind
import com.nextgoal.app.data.model.MatchDetailUi
import com.nextgoal.app.data.model.MatchEventUi
import com.nextgoal.app.data.model.MatchStatus
import com.nextgoal.app.ui.common.UiState
import com.nextgoal.app.ui.common.ViewModelFactories
import com.nextgoal.app.ui.components.FullScreenError
import com.nextgoal.app.ui.components.FullScreenLoading
import com.nextgoal.app.ui.components.InfoBanner
import com.nextgoal.app.ui.components.LiveBadge
import com.nextgoal.app.ui.components.TeamCrest
import com.nextgoal.app.ui.theme.BgCard
import com.nextgoal.app.ui.theme.BgCardAlt
import com.nextgoal.app.ui.theme.BorderSubtle
import com.nextgoal.app.ui.theme.NextGoalBlue
import com.nextgoal.app.ui.theme.PositiveGreen
import com.nextgoal.app.ui.theme.TextPrimary
import com.nextgoal.app.ui.theme.TextSecondary
import com.nextgoal.app.ui.theme.WarningYellow

@Composable
fun MatchDetailScreen(
    matchId: Int,
    onBack: () -> Unit,
    viewModel: MatchDetailViewModel = viewModel(factory = ViewModelFactories.matchDetail(matchId))
) {
    val state by viewModel.state.collectAsState()

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack, modifier = Modifier.background(BgCard, RoundedCornerShape(50))) {
                Icon(Icons.Filled.ArrowBack, contentDescription = "Volver", tint = TextPrimary)
            }
            Column {
                Text("DETALLE DEL PARTIDO", color = TextPrimary, style = androidx.compose.material3.MaterialTheme.typography.titleMedium)
                Text("Marcador y cobertura disponible", color = TextSecondary, style = androidx.compose.material3.MaterialTheme.typography.bodySmall)
            }
        }

        when (val s = state) {
            is UiState.Loading -> FullScreenLoading(Modifier.fillMaxSize())
            is UiState.Error -> FullScreenError(s.message, onRetry = { })
            is UiState.Success -> MatchDetailContent(detail = s.data)
        }
    }
}

@Composable
private fun MatchDetailContent(detail: MatchDetailUi) {
    val match = detail.match
    var tabIndex by remember { mutableIntStateOf(2) } // default to Cronología, the tab with real data
    val tabs = listOf("ESTADÍSTICAS", "ALINEACIONES", "CRONOLOGÍA")

    Column(modifier = Modifier.fillMaxSize()) {
        // Score header
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp)
                .clip(RoundedCornerShape(26.dp))
                .background(Brush.linearGradient(listOf(BgCardAlt, BgCard)))
                .border(BorderStroke(1.dp, BorderSubtle), RoundedCornerShape(26.dp))
                .padding(18.dp)
        ) {
            Text(
                listOfNotNull(match.stage, match.matchday?.let { "Jornada $it" }).joinToString(" • "),
                color = NextGoalBlue,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(Modifier.height(12.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                TeamHeaderColumn(match.homeTeam.shortName, match.homeTeam.crestUrl)
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.width(90.dp)) {
                    Text(
                        "${match.homeScore?.toString() ?: "—"} : ${match.awayScore?.toString() ?: "—"}",
                        color = TextPrimary,
                        style = androidx.compose.ui.text.TextStyle(fontWeight = FontWeight.ExtraBold, fontSize = 32.sp, lineHeight = 38.sp, fontFeatureSettings = "tnum")
                    )
                    Spacer(Modifier.height(4.dp))
                    when (match.status) {
                        MatchStatus.LIVE, MatchStatus.PAUSED -> match.minute?.let {
                            Text("$it' · EN VIVO", color = com.nextgoal.app.ui.theme.LiveRed, style = androidx.compose.material3.MaterialTheme.typography.labelLarge)
                        } ?: LiveBadge()
                        MatchStatus.FINISHED -> Text("FINALIZADO", color = TextSecondary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        MatchStatus.SCHEDULED -> Text("PROGRAMADO", color = TextSecondary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        else -> Text(match.status.name, color = TextSecondary, fontSize = 11.sp)
                    }
                }
                TeamHeaderColumn(match.awayTeam.shortName, match.awayTeam.crestUrl)
            }
        }

        TabRow(selectedTabIndex = tabIndex, containerColor = androidx.compose.ui.graphics.Color.Transparent, contentColor = NextGoalBlue) {
            tabs.forEachIndexed { index, title ->
                Tab(
                    selected = tabIndex == index,
                    onClick = { tabIndex = index },
                    text = { Text(title, style = androidx.compose.material3.MaterialTheme.typography.labelLarge) }
                )
            }
        }

        Crossfade(targetState = tabIndex, animationSpec = tween(180), label = "match-detail-tabs") { selected ->
            when (selected) {
                0 -> StatsTab(detail)
                1 -> LineupsTab(detail)
                else -> TimelineTab(detail.events)
            }
        }
    }
}

@Composable
private fun TeamHeaderColumn(name: String, crestUrl: String?) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.width(100.dp)) {
        TeamCrest(crestUrl, size = 48)
        Spacer(Modifier.height(6.dp))
        Text(name, color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
    }
}

@Composable
private fun StatsTab(detail: MatchDetailUi) {
    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        if (!detail.statsAvailable) {
            InfoBanner(
                "Las estadísticas detalladas del partido (posesión, remates, tiros de esquina) no están " +
                    "disponibles en el plan gratuito de la API de datos en vivo. Puedes conectar un proveedor " +
                    "de estadísticas avanzadas (p. ej. API-Football) en FootballRepository.kt para habilitar esta pestaña."
            )
        }
    }
}

@Composable
private fun LineupsTab(detail: MatchDetailUi) {
    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        if (!detail.lineupsAvailable) {
            InfoBanner(
                "Las alineaciones y formaciones no están disponibles para esta competición en el plan " +
                    "gratuito actual. Se mostrarán automáticamente en cuanto la API las incluya en la respuesta."
            )
        }
    }
}

@Composable
private fun TimelineTab(events: List<MatchEventUi>) {
    if (events.isEmpty()) {
        Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
            InfoBanner("Aún no hay goles, tarjetas o cambios registrados para este partido.")
        }
        return
    }
    LazyColumn(modifier = Modifier.fillMaxSize()) {
        item { Spacer(Modifier.height(8.dp)) }
        item {
            Text("SUCESOS CLAVE", color = TextSecondary, fontWeight = FontWeight.Bold, fontSize = 12.sp, modifier = Modifier.padding(horizontal = 16.dp))
            Spacer(Modifier.height(8.dp))
        }
        items(events) { event -> EventRow(event) }
        item { Spacer(Modifier.height(24.dp)) }
    }
}

@Composable
private fun EventRow(event: MatchEventUi) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(BgCard)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            event.minute?.let { "$it'" } ?: "—",
            color = NextGoalBlue,
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp,
            modifier = Modifier.width(36.dp)
        )
        val (icon, tint) = when (event.kind) {
            EventKind.GOAL -> "⚽" to PositiveGreen
            EventKind.YELLOW_CARD -> "🟨" to WarningYellow
            EventKind.RED_CARD -> "🟥" to androidx.compose.ui.graphics.Color(0xFFEB5757)
            EventKind.SUBSTITUTION -> "🔄" to TextSecondary
        }
        Text(icon, fontSize = 16.sp, modifier = Modifier.width(28.dp))
        Column {
            Text(event.label, color = TextPrimary, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
            event.subLabel?.let {
                Text(it, color = TextSecondary, fontSize = 12.sp)
            }
        }
    }
}
