package com.nextgoal.app.ui.search

import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.nextgoal.app.data.model.CompetitionOption
import com.nextgoal.app.data.model.MatchUi
import com.nextgoal.app.data.remote.dto.ScorerDto
import com.nextgoal.app.data.remote.dto.TeamDto
import com.nextgoal.app.data.repository.FEATURED_COMPETITIONS
import com.nextgoal.app.ui.common.UiState
import com.nextgoal.app.ui.common.ViewModelFactories
import com.nextgoal.app.ui.components.TeamCrest
import com.nextgoal.app.ui.components.InfoBanner
import com.nextgoal.app.ui.theme.*
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun SearchScreen(onBack: () -> Unit, viewModel: SearchViewModel = viewModel(factory = ViewModelFactories.Search)) {
    val query by viewModel.query.collectAsState()
    val competition by viewModel.competition.collectAsState()
    val teamsState by viewModel.teams.collectAsState()
    val scorersState by viewModel.scorers.collectAsState()
    val teamDetails by viewModel.teamDetails.collectAsState()
    val playerDetails by viewModel.playerDetails.collectAsState()
    val teams = (teamsState as? UiState.Success)?.data.orEmpty().filter {
        query.isBlank() || it.name.contains(query, true) || it.shortName.orEmpty().contains(query, true) || it.tla.orEmpty().contains(query, true)
    }
    val scorers = (scorersState as? UiState.Success)?.data.orEmpty().filter {
        query.isBlank() || it.player.name.contains(query, true) || it.team.name.contains(query, true)
    }
    val showingDetails = teamDetails != null || playerDetails != null

    Column(Modifier.fillMaxSize().background(BgDeep)) {
        Row(Modifier.fillMaxWidth().padding(start = 8.dp, end = 16.dp, top = 10.dp, bottom = 10.dp), verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = { if (showingDetails) viewModel.closeDetails() else onBack() }) {
                Icon(Icons.Default.ArrowBack, contentDescription = null, tint = TextPrimary)
                Text(if (showingDetails) "Resultados" else "Volver", color = TextPrimary, style = MaterialTheme.typography.labelLarge)
            }
            Column(Modifier.weight(1f)) {
                Text("Buscar fútbol", color = TextPrimary, style = MaterialTheme.typography.titleLarge)
                Text("Equipos, jugadores y estadísticas", color = TextSecondary, style = MaterialTheme.typography.bodySmall)
            }
        }
        OutlinedTextField(
            value = query,
            onValueChange = viewModel::setQuery,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            placeholder = { Text("Nombre de un jugador o equipo", color = TextSecondary) },
            leadingIcon = { Icon(Icons.Default.Search, null, tint = NextGoalBlue) },
            singleLine = true,
            shape = RoundedCornerShape(16.dp),
            colors = OutlinedTextFieldDefaults.colors(focusedTextColor = TextPrimary, unfocusedTextColor = TextPrimary, cursorColor = NextGoalBlue, focusedBorderColor = NextGoalBlue, unfocusedBorderColor = BgCardAlt, focusedContainerColor = BgCard, unfocusedContainerColor = BgCard)
        )

        if (!showingDetails) {
            Text("LIGA PARA BUSCAR", color = TextSecondary, style = MaterialTheme.typography.labelMedium, modifier = Modifier.padding(start = 16.dp, top = 16.dp, bottom = 8.dp))
            Column(verticalArrangement = Arrangement.spacedBy(7.dp), modifier = Modifier.padding(horizontal = 12.dp)) {
                FEATURED_COMPETITIONS.chunked(3).forEach { row ->
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                        row.forEach { LeagueChip(it, competition, viewModel::selectCompetition, Modifier.weight(1f)) }
                    }
                }
            }
            LazyColumn(contentPadding = PaddingValues(bottom = 24.dp)) {
                item { SectionLabel("EQUIPOS", "${competition.displayName}") }
                when (val state = teamsState) {
                    UiState.Loading -> item { LoadingRow() }
                    is UiState.Error -> item { MessageCard(state.message) }
                    is UiState.Success -> if (teams.isEmpty()) item { MessageCard("No encontramos equipos con ese nombre.") }
                }
                if (teamsState is UiState.Success) items(teams, key = { "team-${it.id}" }) { team ->
                    TeamRow(team) { viewModel.openTeam(team) }
                }

                item { SectionLabel("JUGADORES", "Goles y asistencias de la temporada") }
                when (val state = scorersState) {
                    UiState.Loading -> item { LoadingRow() }
                    is UiState.Error -> item { MessageCard("No fue posible cargar las estadísticas de jugadores: ${state.message}") }
                    is UiState.Success -> if (scorers.isEmpty()) item { MessageCard("No encontramos jugadores que coincidan en esta competición.") }
                }
                if (scorersState is UiState.Success) items(scorers, key = { "player-${it.player.id}" }) { scorer ->
                    ScorerRow(scorer) { viewModel.openPlayer(scorer.player.id) }
                }
            }
        } else {
            when {
                teamDetails != null -> when (val state = teamDetails) {
                    null -> Unit
                    UiState.Loading -> LoadingRow(Modifier.weight(1f))
                    is UiState.Error -> MessageCard(state.message)
                    is UiState.Success -> TeamDetailsContent(state.data, query, onPlayerClick = { member -> viewModel.openPlayer(member.id, member) })
                }
                playerDetails != null -> when (val state = playerDetails) {
                    null -> Unit
                    UiState.Loading -> LoadingRow(Modifier.weight(1f))
                    is UiState.Error -> MessageCard(state.message)
                    is UiState.Success -> PlayerDetailsContent(state.data)
                }
            }
        }
    }
}

@Composable
private fun TeamDetailsContent(details: TeamSearchDetails, query: String, onPlayerClick: (com.nextgoal.app.data.remote.dto.SquadMemberDto) -> Unit) {
    val members = details.squad.filter { query.isBlank() || it.name.contains(query, true) || it.position.orEmpty().contains(query, true) }
    LazyColumn(contentPadding = PaddingValues(bottom = 24.dp)) {
        item {
            Row(Modifier.fillMaxWidth().padding(16.dp).clip(RoundedCornerShape(24.dp)).background(BgCardAlt).border(1.dp, BorderSubtle, RoundedCornerShape(24.dp)).padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                TeamCrest(details.team.crest, size = 56)
                Column(Modifier.padding(start = 14.dp)) {
                    Text(details.team.name, color = TextPrimary, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    Text("${details.team.shortName ?: details.team.tla.orEmpty()} · ${details.squad.size} jugadores", color = TextSecondary, fontSize = 12.sp)
                }
            }
            Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                StatCard("Goles", details.goals?.toString() ?: "—", "Temporada actual", Modifier.weight(1f))
                StatCard("Asistencias", details.assists?.toString() ?: "—", "Registradas en la liga", Modifier.weight(1f))
            }
            Spacer(Modifier.height(12.dp))
            Text("PRÓXIMO PARTIDO", color = TextSecondary, fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp))
            details.nextMatch?.let { MatchInfo(it) } ?: MessageCard("No hay un próximo partido publicado para este equipo.")
            SectionLabel("PLANTILLA", "Toca un jugador para ver su ficha y estadísticas")
        }
        val visibleMembers = members
        if (visibleMembers.isEmpty()) item { MessageCard("No encontramos jugadores en esta plantilla con ese nombre.") }
        items(visibleMembers, key = { "squad-${it.id}" }) { member ->
            PlayerSquadRow(member.name, member.position, member.nationality) { onPlayerClick(member) }
        }
    }
}

@Composable
private fun PlayerDetailsContent(details: PlayerSearchDetails) {
    val player = details.player
    LazyColumn(contentPadding = PaddingValues(bottom = 24.dp)) {
        item {
            Column(Modifier.fillMaxWidth().padding(16.dp).clip(RoundedCornerShape(24.dp)).background(BgCardAlt).border(1.dp, BorderSubtle, RoundedCornerShape(24.dp)).padding(18.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(54.dp).clip(CircleShape).background(BgCardAlt), contentAlignment = Alignment.Center) {
                        Text(player.name.take(1).uppercase(), color = NextGoalBlue, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                    }
                    Column(Modifier.padding(start = 14.dp)) {
                        Text(player.name, color = TextPrimary, fontSize = 19.sp, fontWeight = FontWeight.Bold)
                        Text(listOfNotNull(player.position, player.teamName).joinToString(" · "), color = TextSecondary, fontSize = 12.sp)
                    }
                }
                player.nationality?.let { Text(it, color = TextSecondary, fontSize = 12.sp, modifier = Modifier.padding(top = 10.dp)) }
            }
            Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                StatCard("Goles", player.goals?.toString() ?: "—", details.competitionName, Modifier.weight(1f))
                StatCard("Asistencias", player.assists?.toString() ?: "—", details.competitionName, Modifier.weight(1f))
            }
            Spacer(Modifier.height(12.dp))
            Text("PRÓXIMO PARTIDO DEL EQUIPO", color = TextSecondary, fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp))
            details.nextMatch?.let { MatchInfo(it) } ?: MessageCard("No hay un próximo partido publicado para su equipo.")
            Text("Goles y asistencias de ${details.competitionName} cuando están disponibles; la raya indica que la API no entregó ese dato.", color = TextTertiary, fontSize = 11.sp, modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp))
        }
    }
}

@Composable
private fun SectionLabel(title: String, subtitle: String) {
    Column(Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, top = 18.dp, bottom = 8.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text(title, color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Bold)
            Text(subtitle, color = TextSecondary, fontSize = 10.sp, maxLines = 1)
        }
    }
}

@Composable
private fun LeagueChip(option: CompetitionOption, selected: CompetitionOption, onClick: (CompetitionOption) -> Unit, modifier: Modifier = Modifier) {
    val active = selected.code == option.code
    Surface(onClick = { onClick(option) }, shape = RoundedCornerShape(50), color = if (active) NextGoalBlue else BgCard, border = if (active) null else BorderStroke(1.dp, BorderSubtle), modifier = modifier.defaultMinSize(minHeight = 48.dp)) {
        Text(option.displayName, color = if (active) BgDeep else TextSecondary, style = MaterialTheme.typography.labelLarge, maxLines = 1, modifier = Modifier.padding(horizontal = 10.dp, vertical = 10.dp))
    }
}

@Composable
private fun TeamRow(team: TeamDto, onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 5.dp).clip(RoundedCornerShape(18.dp)).background(BgCard).border(1.dp, BorderSubtle, RoundedCornerShape(18.dp)).clickable(onClick = onClick).padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
        TeamCrest(team.crest, size = 36)
        Column(Modifier.weight(1f).padding(start = 12.dp)) {
            Text(team.name, color = TextPrimary, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
            Text(team.shortName ?: team.tla.orEmpty(), color = TextSecondary, fontSize = 11.sp)
        }
        Text("Ver ficha  ›", color = NextGoalBlue, fontSize = 11.sp)
    }
}

@Composable
private fun ScorerRow(scorer: ScorerDto, onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 5.dp).clip(RoundedCornerShape(18.dp)).background(BgCard).border(1.dp, BorderSubtle, RoundedCornerShape(18.dp)).clickable(onClick = onClick).padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(36.dp).clip(CircleShape).background(BgCardAlt), contentAlignment = Alignment.Center) {
            Text(scorer.player.name.take(1).uppercase(), color = NextGoalBlue, fontWeight = FontWeight.Bold)
        }
        Column(Modifier.weight(1f).padding(start = 10.dp)) {
            Text(scorer.player.name, color = TextPrimary, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
            Text(scorer.team.shortName ?: scorer.team.name, color = TextSecondary, fontSize = 11.sp)
        }
        Column(horizontalAlignment = Alignment.End) {
            Text("${scorer.goals ?: "—"} G  ·  ${scorer.assists ?: "—"} A", color = NextGoalBlue, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            Text("Ficha  ›", color = TextSecondary, fontSize = 10.sp)
        }
    }
}

@Composable
private fun PlayerSquadRow(name: String, position: String?, nationality: String?, onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 5.dp).clip(RoundedCornerShape(18.dp)).background(BgCard).border(1.dp, BorderSubtle, RoundedCornerShape(18.dp)).clickable(onClick = onClick).padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(36.dp).clip(CircleShape).background(BgCardAlt), contentAlignment = Alignment.Center) { Text(name.take(1).uppercase(), color = NextGoalBlue, fontWeight = FontWeight.Bold) }
        Column(Modifier.weight(1f).padding(start = 10.dp)) {
            Text(name, color = TextPrimary, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
            Text(listOfNotNull(position, nationality).joinToString(" · ").ifBlank { "Jugador" }, color = TextSecondary, fontSize = 11.sp)
        }
        Text("Ficha  ›", color = NextGoalBlue, fontSize = 11.sp)
    }
}

@Composable
private fun StatCard(title: String, value: String, caption: String, modifier: Modifier = Modifier) {
    Column(modifier.clip(RoundedCornerShape(18.dp)).background(BgCard).border(1.dp, BorderSubtle, RoundedCornerShape(18.dp)).padding(14.dp)) {
        Text(title.uppercase(), color = TextSecondary, style = MaterialTheme.typography.labelMedium)
        Text(value, color = TextPrimary, fontSize = 23.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 4.dp))
        Text(caption, color = TextTertiary, style = MaterialTheme.typography.labelSmall)
    }
}

@Composable
private fun MatchInfo(match: MatchUi) {
    val date = runCatching {
        Instant.parse(match.utcDate).atZone(ZoneId.systemDefault())
            .format(DateTimeFormatter.ofPattern("EEE d MMM · HH:mm", Locale.forLanguageTag("es")))
    }.getOrDefault(match.utcDate)
    Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp).clip(RoundedCornerShape(18.dp)).background(BgCard).border(1.dp, BorderSubtle, RoundedCornerShape(18.dp)).padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text("${match.homeTeam.shortName}  vs  ${match.awayTeam.shortName}", color = TextPrimary, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
            Text(match.competitionName, color = TextSecondary, fontSize = 11.sp)
        }
        Text(date, color = NextGoalBlue, fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun LoadingRow(modifier: Modifier = Modifier) {
    Column(modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp).clip(RoundedCornerShape(18.dp)).background(BgCard).padding(16.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) {
        Box(Modifier.fillMaxWidth(0.42f).height(10.dp).background(BgCardAlt, RoundedCornerShape(50)))
        Box(Modifier.fillMaxWidth(0.82f).height(13.dp).background(BgCardAlt, RoundedCornerShape(50)))
        Box(Modifier.fillMaxWidth(0.58f).height(10.dp).background(BgCardAlt, RoundedCornerShape(50)))
    }
}

@Composable
private fun MessageCard(message: String) {
    InfoBanner(message, Modifier.padding(horizontal = 16.dp, vertical = 6.dp))
}
