package com.nextgoal.app.ui.splash

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.material3.Text
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.nextgoal.app.R
import com.nextgoal.app.ui.theme.BgDeep
import com.nextgoal.app.ui.theme.NextGoalBlue
import com.nextgoal.app.ui.theme.TextSecondary
import kotlinx.coroutines.delay

@Composable
fun SplashScreen(onFinished: () -> Unit) {
    var showBadge by remember { mutableStateOf(false) }
    val logoAlpha by animateFloatAsState(targetValue = 1f, animationSpec = tween(600), label = "logoAlpha")
    val badgeAlpha by animateFloatAsState(
        targetValue = if (showBadge) 1f else 0f,
        animationSpec = tween(500),
        label = "badgeAlpha"
    )

    LaunchedEffect(Unit) {
        delay(500)
        showBadge = true
        delay(1400)
        onFinished()
    }

    Column(
        modifier = Modifier.fillMaxSize().background(BgDeep),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Image(
            painter = painterResource(id = R.drawable.nextgoal_logo),
            contentDescription = "NextGoal",
            modifier = Modifier.size(196.dp).alpha(logoAlpha)
        )
        Spacer(Modifier.height(22.dp))
        Text("SIGUE CADA JUGADA", color = NextGoalBlue, fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.8.sp)
        Text("FÚTBOL, AL INSTANTE", color = TextSecondary, fontSize = 12.sp, letterSpacing = 1.2.sp, modifier = Modifier.padding(top = 5.dp))
        Spacer(Modifier.height(28.dp))
        Image(
            painter = painterResource(id = R.drawable.nextgoal_loading_badge),
            contentDescription = "Cargando",
            modifier = Modifier.size(72.dp).alpha(badgeAlpha)
        )
    }
}
