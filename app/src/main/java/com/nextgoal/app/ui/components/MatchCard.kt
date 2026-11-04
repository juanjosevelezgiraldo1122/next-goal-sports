package com.nextgoal.app.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.NotificationsNone
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.graphics.Shape
import coil.compose.AsyncImage
import com.nextgoal.app.data.model.MatchStatus
import com.nextgoal.app.data.model.MatchUi
import com.nextgoal.app.ui.theme.BgCard
import com.nextgoal.app.ui.theme.BgCardAlt
import com.nextgoal.app.ui.theme.BorderSubtle
import com.nextgoal.app.ui.theme.LiveRed
import com.nextgoal.app.ui.theme.NextGoalBlue
import com.nextgoal.app.ui.theme.TextPrimary
import com.nextgoal.app.ui.theme.TextSecondary
import com.nextgoal.app.ui.theme.TextTertiary
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException

@Composable
fun TeamCrest(url: String?, size: Int = 28) {
    Box(
        modifier = Modifier
            .size(size.dp)
            .clip(RoundedCornerShape((size / 4).coerceAtLeast(7).dp))
            .background(Color.White)
            .border(1.dp, Color(0x1F07111B), RoundedCornerShape((size / 4).coerceAtLeast(7).dp)),
        contentAlignment = Alignment.Center
    ) {
        if (!url.isNullOrBlank()) {
            AsyncImage(
                model = url,
                contentDescription = "Escudo del equipo",
                modifier = Modifier.size(size.dp * 0.82f),
                contentScale = ContentScale.Fit
            )
        }
    }
}

/** Large featured card for the single most relevant live match on Home. */
@Composable
fun FeaturedLiveMatchCard(match: MatchUi, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(24.dp),
        color = BgCardAlt,
        border = BorderStroke(1.dp, LiveRed.copy(alpha = 0.42f)),
        shadowElevation = 10.dp
    ) {
        Column(
            modifier = Modifier
                .background(Brush.linearGradient(listOf(BgCardAlt, BgCard, BgCard)))
                .padding(18.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    match.competitionName.uppercase(),
                    color = NextGoalBlue,
                    style = MaterialTheme.typography.labelLarge,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                Spacer(Modifier.width(8.dp))
                when (match.status) {
                    MatchStatus.LIVE -> match.minute?.let { MinuteBadge(it) } ?: LiveBadge()
                    MatchStatus.PAUSED -> LiveBadge(text = "PAUSADO")
                    else -> Unit
                }
            }
            Spacer(Modifier.height(20.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TeamColumn(name = match.homeTeam.shortName, crestUrl = match.homeTeam.crestUrl, modifier = Modifier.weight(1f))
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(horizontal = 8.dp)) {
                    ScoreText("${match.homeScore?.toString() ?: "—"} : ${match.awayScore?.toString() ?: "—"}", 32)
                    Text("MARCADOR", color = TextTertiary, style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(top = 3.dp))
                }
                TeamColumn(name = match.awayTeam.shortName, crestUrl = match.awayTeam.crestUrl, modifier = Modifier.weight(1f))
            }
            if (!match.venue.isNullOrBlank()) {
                Spacer(Modifier.height(16.dp))
                Text(match.venue, color = TextSecondary, style = MaterialTheme.typography.bodySmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}

@Composable
private fun TeamColumn(name: String, crestUrl: String?, modifier: Modifier = Modifier) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = modifier) {
        TeamCrest(crestUrl, size = 48)
        Spacer(Modifier.height(9.dp))
        Text(
            name,
            color = TextPrimary,
            style = MaterialTheme.typography.titleSmall,
            textAlign = TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
    }
}

/** Compact match card shared by upcoming, finished, live and date-range lists. */
@Composable
fun MatchRowCard(match: MatchUi, onClick: () -> Unit) {
    val statusLabel = when (match.status) {
        MatchStatus.LIVE -> match.minute?.let { "$it' · EN VIVO" } ?: "EN VIVO"
        MatchStatus.PAUSED -> "PAUSADO"
        MatchStatus.FINISHED -> "FINAL"
        MatchStatus.SCHEDULED -> formatKickoffTime(match.utcDate)
        MatchStatus.POSTPONED -> "APLAZADO"
        MatchStatus.OTHER -> "—"
    }
    val statusColor = when (match.status) {
        MatchStatus.LIVE, MatchStatus.PAUSED -> LiveRed
        MatchStatus.SCHEDULED -> NextGoalBlue
        else -> TextSecondary
    }

    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 5.dp),
        shape = RoundedCornerShape(18.dp),
        color = BgCard,
        border = BorderStroke(1.dp, if (match.status == MatchStatus.LIVE) LiveRed.copy(alpha = 0.38f) else BorderSubtle)
    ) {
        Column(Modifier.padding(horizontal = 14.dp, vertical = 13.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    match.competitionName,
                    color = NextGoalBlue,
                    style = MaterialTheme.typography.labelLarge,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                Text(statusLabel, color = statusColor, style = MaterialTheme.typography.labelMedium)
            }
            Spacer(Modifier.height(11.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                    TeamLine(match.homeTeam.shortName, match.homeTeam.crestUrl)
                    TeamLine(match.awayTeam.shortName, match.awayTeam.crestUrl)
                }
                if (match.status == MatchStatus.LIVE || match.status == MatchStatus.FINISHED || match.status == MatchStatus.PAUSED) {
                    Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        ScoreText(match.homeScore?.toString() ?: "—", 19)
                        ScoreText(match.awayScore?.toString() ?: "—", 19)
                    }
                } else if (match.status == MatchStatus.SCHEDULED) {
                    Icon(Icons.Filled.NotificationsNone, contentDescription = "Recordatorio", tint = TextTertiary, modifier = Modifier.size(22.dp))
                }
            }
        }
    }
}

@Composable
private fun TeamLine(name: String, crestUrl: String?) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        TeamCrest(crestUrl, size = 26)
        Spacer(Modifier.width(9.dp))
        Text(name, color = TextPrimary, style = MaterialTheme.typography.bodyMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
private fun ScoreText(value: String, size: Int) {
    Crossfade(targetState = value, animationSpec = tween(180), label = "score-change") { displayed ->
        Text(
            displayed,
            color = TextPrimary,
            style = TextStyle(fontSize = size.sp, lineHeight = (size + 5).sp, fontWeight = FontWeight.ExtraBold, fontFeatureSettings = "tnum")
        )
    }
}

fun formatKickoffTime(utcDate: String): String = try {
    val parsed = OffsetDateTime.parse(utcDate)
    parsed.format(DateTimeFormatter.ofPattern("HH:mm"))
} catch (e: DateTimeParseException) {
    "--:--"
}
