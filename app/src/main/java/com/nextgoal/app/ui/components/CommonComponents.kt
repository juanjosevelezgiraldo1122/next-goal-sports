package com.nextgoal.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.NotificationsNone
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.WifiOff
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nextgoal.app.ui.theme.BgCardAlt
import com.nextgoal.app.ui.theme.BgCard
import com.nextgoal.app.ui.theme.BgElevated
import com.nextgoal.app.ui.theme.BorderSubtle
import com.nextgoal.app.ui.theme.LiveRed
import com.nextgoal.app.ui.theme.NextGoalBlue
import com.nextgoal.app.ui.theme.TextPrimary
import com.nextgoal.app.ui.theme.TextSecondary

@Composable
fun LiveBadge(modifier: Modifier = Modifier, text: String = "EN VIVO") {
    val pulse by rememberInfiniteTransition(label = "live-indicator")
        .animateFloat(0.48f, 1f, infiniteRepeatable(tween(900), RepeatMode.Reverse), label = "live-dot")
    Row(
        modifier = modifier
            .background(LiveRed.copy(alpha = 0.12f), RoundedCornerShape(50))
            .border(1.dp, LiveRed.copy(alpha = 0.34f), RoundedCornerShape(50))
            .padding(horizontal = 10.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Box(
            modifier = Modifier
                .size(7.dp)
                .alpha(pulse)
                .background(LiveRed, CircleShape)
        )
        Text(text, color = LiveRed, style = MaterialTheme.typography.labelLarge)
    }
}

@Composable
fun MinuteBadge(minute: Int) {
    Box(
        modifier = Modifier
            .background(LiveRed, RoundedCornerShape(50))
            .padding(horizontal = 10.dp, vertical = 6.dp)
    ) {
        Text("$minute' · EN VIVO", color = Color(0xFF21080B), style = MaterialTheme.typography.labelLarge)
    }
}

@Composable
fun NextGoalTopBar(
    onSearchClick: (() -> Unit)? = null,
    onNotificationsClick: (() -> Unit)? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .background(BgCardAlt, RoundedCornerShape(12.dp))
                    .border(1.dp, NextGoalBlue.copy(alpha = 0.72f), RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text("NG", color = NextGoalBlue, fontSize = 13.sp, fontWeight = FontWeight.ExtraBold)
            }
            Text("NEXT", color = TextPrimary, fontWeight = FontWeight.ExtraBold, fontSize = 18.sp, letterSpacing = 0.5.sp)
            Text("GOAL", color = NextGoalBlue, fontWeight = FontWeight.ExtraBold, fontSize = 18.sp, letterSpacing = 0.5.sp)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            IconButton(onClick = { onSearchClick?.invoke() }) {
                Icon(Icons.Filled.Search, contentDescription = "Buscar", tint = TextPrimary)
            }
            IconButton(onClick = { onNotificationsClick?.invoke() }) {
                Icon(Icons.Filled.NotificationsNone, contentDescription = "Notificaciones", tint = TextPrimary)
            }
        }
    }
}

@Composable
fun SectionHeader(title: String, actionText: String? = null, onActionClick: (() -> Unit)? = null) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(9.dp)) {
            Box(Modifier.size(width = 3.dp, height = 18.dp).background(NextGoalBlue, RoundedCornerShape(50)))
            Text(title, color = TextPrimary, style = MaterialTheme.typography.titleMedium)
        }
        if (actionText != null) {
            Text(
                actionText,
                color = NextGoalBlue,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .clickable(enabled = onActionClick != null) { onActionClick?.invoke() }
                    .padding(horizontal = 10.dp, vertical = 8.dp)
            )
        }
    }
}

@Composable
fun FullScreenLoading(modifier: Modifier = Modifier) {
    val shimmer by rememberInfiniteTransition(label = "skeleton-loading")
        .animateFloat(-380f, 820f, infiniteRepeatable(tween(1500, easing = LinearEasing), RepeatMode.Restart), label = "skeleton-shimmer")
    val shimmerBrush = Brush.linearGradient(
        colors = listOf(BgCardAlt.copy(alpha = 0.5f), BgElevated.copy(alpha = 0.95f), BgCardAlt.copy(alpha = 0.5f)),
        start = Offset(shimmer, 0f),
        end = Offset(shimmer + 340f, 140f)
    )
    Column(
        modifier = modifier.fillMaxSize().padding(horizontal = 16.dp, vertical = 18.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        repeat(4) { index ->
            Column(
                Modifier.fillMaxWidth().background(BgCard, RoundedCornerShape(18.dp)).padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(Modifier.size(width = if (index % 2 == 0) 118.dp else 86.dp, height = 10.dp).background(shimmerBrush, RoundedCornerShape(50)))
                Box(Modifier.fillMaxWidth().height(14.dp).background(shimmerBrush, RoundedCornerShape(50)))
                Box(Modifier.fillMaxWidth(0.62f).height(10.dp).background(shimmerBrush, RoundedCornerShape(50)))
            }
        }
    }
}

@Composable
fun FullScreenError(message: String, onRetry: (() -> Unit)? = null, modifier: Modifier = Modifier) {
    Box(modifier = modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
        Column(
            modifier = Modifier.fillMaxWidth().background(BgCard, RoundedCornerShape(24.dp)).padding(22.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(Modifier.size(48.dp).background(BgCardAlt, CircleShape), contentAlignment = Alignment.Center) {
                Icon(Icons.Outlined.WifiOff, contentDescription = null, tint = NextGoalBlue)
            }
            Text("No se pudo cargar la información", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 17.sp)
            Text(message, color = TextSecondary, fontSize = 14.sp)
            if (onRetry != null) {
                Button(onClick = onRetry, shape = RoundedCornerShape(14.dp)) { Text("Reintentar") }
            }
        }
    }
}

@Composable
fun InfoBanner(message: String, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(BgCardAlt, RoundedCornerShape(16.dp))
            .border(1.dp, BorderSubtle, RoundedCornerShape(16.dp))
            .padding(14.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.Top
    ) {
        Icon(Icons.Outlined.Info, contentDescription = null, tint = NextGoalBlue, modifier = Modifier.size(18.dp))
        Text(message, color = TextSecondary, fontSize = 13.sp, lineHeight = 19.sp)
    }
}
