package com.nextgoal.app.ui.profile

import android.content.Context
import android.util.Base64
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.nextgoal.app.data.model.PlayerUi
import com.nextgoal.app.data.remote.dto.SquadMemberDto
import com.nextgoal.app.ui.common.UiState
import com.nextgoal.app.ui.common.ViewModelFactories
import com.nextgoal.app.ui.components.FullScreenError
import com.nextgoal.app.ui.components.FullScreenLoading
import com.nextgoal.app.ui.components.InfoBanner
import com.nextgoal.app.ui.components.NextGoalTopBar
import com.nextgoal.app.ui.components.TeamCrest
import com.nextgoal.app.ui.theme.BgCard
import com.nextgoal.app.ui.theme.BgCardAlt
import com.nextgoal.app.ui.theme.NextGoalBlue
import com.nextgoal.app.ui.theme.TextPrimary
import com.nextgoal.app.ui.theme.TextSecondary
import com.nextgoal.app.ui.theme.PositiveGreen
import com.nextgoal.app.ui.theme.DividerColor
import com.nextgoal.app.ui.theme.TextTertiary
import com.nextgoal.app.ui.theme.NegativeRed
import androidx.compose.ui.graphics.Color
import java.security.SecureRandom
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

@Composable
fun ProfileScreen(onSearchClick: () -> Unit, viewModel: ProfileViewModel = viewModel(factory = ViewModelFactories.Profile)) {
    val selectedPlayerId by viewModel.selectedPlayerId.collectAsState()
    val playerState by viewModel.playerState.collectAsState()
    var tab by remember { mutableStateOf(0) }

    if (selectedPlayerId != null) {
        Column(modifier = Modifier.fillMaxSize()) {
            Row(modifier = Modifier.fillMaxWidth().padding(4.dp), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = { viewModel.clearSelection() }) {
                    Icon(Icons.Filled.ArrowBack, contentDescription = "Volver", tint = TextPrimary)
                }
                Text("PERFIL DE JUGADOR", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            }
            when (val s = playerState) {
                is UiState.Loading, null -> FullScreenLoading(Modifier.fillMaxSize())
                is UiState.Error -> FullScreenError(s.message, onRetry = { selectedPlayerId?.let(viewModel::selectPlayer) })
                is UiState.Success -> PlayerProfileContent(s.data)
            }
        }
    } else {
        Column(modifier = Modifier.fillMaxSize()) {
            NextGoalTopBar(onSearchClick = onSearchClick)
            Text("Tu perfil", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 22.sp, modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp))
            Text("Tu cuenta y el mundo del fútbol, en un solo lugar.", color = TextSecondary, fontSize = 12.sp, modifier = Modifier.padding(horizontal = 16.dp, vertical = 2.dp))
            Row(Modifier.fillMaxWidth().padding(16.dp).clip(RoundedCornerShape(14.dp)).background(BgCard).padding(4.dp)) {
                listOf("Mi cuenta", "Jugadores").forEachIndexed { index, label ->
                    Box(Modifier.weight(1f).clip(RoundedCornerShape(11.dp)).background(if (tab == index) NextGoalBlue else Color.Transparent).clickable { tab = index }.padding(vertical = 11.dp), contentAlignment = Alignment.Center) {
                        Text(label, color = if (tab == index) Color.White else TextSecondary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
            if (tab == 0) OwnProfileContent(Modifier.weight(1f)) else Box(Modifier.weight(1f)) { SquadListScreen(viewModel) }
        }
    }
}

@Composable
private fun OwnProfileContent(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("nextgoal_local_account", Context.MODE_PRIVATE) }
    var isSignedIn by remember { mutableStateOf(prefs.getString("session_email", null) != null) }
    var isRegister by remember { mutableStateOf(prefs.getString("account_email", null) == null) }
    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }

    if (isSignedIn) {
        val savedName = prefs.getString("account_name", "Usuario NextGoal").orEmpty()
        val savedEmail = prefs.getString("account_email", "").orEmpty()
        Column(modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp).clip(RoundedCornerShape(22.dp)).background(BgCard).padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(54.dp).clip(CircleShape).background(BgCardAlt), contentAlignment = Alignment.Center) { Text(savedName.take(1).uppercase(), color = NextGoalBlue, fontSize = 22.sp, fontWeight = FontWeight.Bold) }
                Column(Modifier.padding(start = 14.dp)) {
                    Text(savedName, color = TextPrimary, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    Text("Cuenta personal", color = PositiveGreen, fontSize = 12.sp)
                }
            }
            Spacer(Modifier.fillMaxWidth().height(1.dp).background(DividerColor))
            Text("CORREO ELECTRÓNICO", color = TextSecondary, fontSize = 10.sp, fontWeight = FontWeight.Bold)
            Text(savedEmail, color = TextPrimary, fontSize = 14.sp)
            Text("Tu perfil se guarda en este dispositivo.", color = TextSecondary, fontSize = 12.sp)
            Button(onClick = { prefs.edit().remove("session_email").apply(); isSignedIn = false }, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = BgCardAlt)) { Text("Cerrar sesión", color = TextPrimary) }
        }
        return
    }

    Column(modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp).clip(RoundedCornerShape(22.dp)).background(BgCard).padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(if (isRegister) "Crea tu perfil" else "Qué bueno verte", color = TextPrimary, fontSize = 20.sp, fontWeight = FontWeight.Bold)
        Text(if (isRegister) "Guarda tus preferencias y personaliza tu experiencia." else "Inicia sesión para entrar a tu espacio personal.", color = TextSecondary, fontSize = 12.sp)
        if (isRegister) AccountField(name, { name = it }, "Nombre", Icons.Default.Person)
        AccountField(email, { email = it }, "Correo electrónico", Icons.Default.Email)
        AccountField(password, { password = it }, "Contraseña", Icons.Default.Lock, password = true)
        error?.let { Text(it, color = NegativeRed, fontSize = 12.sp) }
        Button(
            onClick = {
                error = null
                val normalizedEmail = email.trim().lowercase()
                when {
                    !normalizedEmail.matches(Regex("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$")) -> error = "Escribe un correo válido."
                    password.length < 8 -> error = "La contraseña debe tener al menos 8 caracteres."
                    isRegister && name.isBlank() -> error = "Escribe tu nombre para crear el perfil."
                    isRegister && prefs.getString("account_email", null) != null -> error = "Ya hay un perfil en este dispositivo. Inicia sesión."
                    isRegister -> {
                        val salt = ByteArray(16).also { SecureRandom().nextBytes(it) }
                        prefs.edit().putString("account_name", name.trim()).putString("account_email", normalizedEmail)
                            .putString("account_salt", Base64.encodeToString(salt, Base64.NO_WRAP))
                            .putString("account_hash", passwordDigest(salt, password)).putString("session_email", normalizedEmail).apply()
                        isSignedIn = true
                    }
                    prefs.getString("account_email", null) != normalizedEmail -> error = "No encontramos una cuenta con ese correo en este dispositivo."
                    else -> {
                        val salt = Base64.decode(prefs.getString("account_salt", ""), Base64.DEFAULT)
                        if (passwordDigest(salt, password) == prefs.getString("account_hash", null)) {
                            prefs.edit().putString("session_email", normalizedEmail).apply(); isSignedIn = true
                        } else error = "La contraseña no coincide."
                    }
                }
            }, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = NextGoalBlue)
        ) { Text(if (isRegister) "Crear mi perfil" else "Iniciar sesión", color = Color.White, fontWeight = FontWeight.SemiBold) }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
            Text(if (isRegister) "¿Ya tienes cuenta?" else "¿Eres nuevo?", color = TextSecondary, fontSize = 12.sp)
            Text(if (isRegister) "  Inicia sesión" else "  Regístrate", color = NextGoalBlue, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.clickable { isRegister = !isRegister; error = null })
        }
        Text("Acceso local de demostración. La cuenta no se sincroniza con otros dispositivos.", color = TextTertiary, fontSize = 10.sp)
    }
}

@Composable
private fun AccountField(value: String, onValueChange: (String) -> Unit, label: String, icon: androidx.compose.ui.graphics.vector.ImageVector, password: Boolean = false) {
    OutlinedTextField(
        value = value, onValueChange = onValueChange, modifier = Modifier.fillMaxWidth(), label = { Text(label) },
        leadingIcon = { Icon(icon, null, tint = TextSecondary) }, singleLine = true,
        visualTransformation = if (password) androidx.compose.ui.text.input.PasswordVisualTransformation() else androidx.compose.ui.text.input.VisualTransformation.None,
        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = TextPrimary, unfocusedTextColor = TextPrimary, focusedLabelColor = NextGoalBlue, unfocusedLabelColor = TextSecondary, cursorColor = NextGoalBlue, focusedBorderColor = NextGoalBlue, unfocusedBorderColor = DividerColor)
    )
}

private fun passwordDigest(salt: ByteArray, password: String): String {
    val spec = PBEKeySpec(password.toCharArray(), salt, 120_000, 256)
    return try {
        Base64.encodeToString(SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).encoded, Base64.NO_WRAP)
    } finally {
        spec.clearPassword()
    }
}

@Composable
private fun SquadListScreen(viewModel: ProfileViewModel) {
    val squadState by viewModel.squadState.collectAsState()
    val teamName by viewModel.teamName.collectAsState()

    Column(modifier = Modifier.fillMaxSize()) {
        Text(
            teamName?.let { "Plantilla de $it" } ?: "Plantilla",
            color = TextPrimary,
            fontWeight = FontWeight.Bold,
            fontSize = 16.sp,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        )
        when (val s = squadState) {
            is UiState.Loading -> FullScreenLoading(Modifier.fillMaxSize())
            is UiState.Error -> FullScreenError(s.message, onRetry = { viewModel.loadSquad() })
            is UiState.Success -> {
                if (s.data.isEmpty()) {
                    Box(modifier = Modifier.padding(16.dp)) {
                        InfoBanner("Esta competición o equipo no expone la plantilla completa en el plan gratuito de la API.")
                    }
                } else {
                    LazyColumn {
                        items(s.data) { member ->
                            SquadRow(member, onClick = { viewModel.selectPlayer(member.id) })
                        }
                        item { Spacer(Modifier.height(24.dp)) }
                    }
                }
            }
        }
    }
}

@Composable
private fun SquadRow(member: SquadMemberDto, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(BgCard)
            .clickable { onClick() }
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier.size(40.dp).clip(CircleShape).background(BgCardAlt),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Filled.Person, contentDescription = null, tint = TextSecondary)
        }
        Spacer(Modifier.width(12.dp))
        Column {
            Text(member.name, color = TextPrimary, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
            Text(
                listOfNotNull(member.position, member.nationality).joinToString(" • "),
                color = TextSecondary,
                fontSize = 12.sp
            )
        }
    }
}

@Composable
private fun PlayerProfileContent(player: PlayerUi) {
    LazyColumn(modifier = Modifier.fillMaxSize()) {
        item {
            Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier.size(64.dp).clip(CircleShape).background(BgCardAlt),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Filled.Person, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(32.dp))
                }
                Spacer(Modifier.width(14.dp))
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        player.shirtNumber?.let {
                            Text("#$it • ", color = NextGoalBlue, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }
                        Text(player.position ?: "Jugador", color = NextGoalBlue, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                    Text(player.name, color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 20.sp)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        player.teamCrestUrl?.let { TeamCrest(it, size = 16) }
                        Spacer(Modifier.width(6.dp))
                        Text(
                            listOfNotNull(player.teamName, player.nationality).joinToString(" / "),
                            color = TextSecondary,
                            fontSize = 13.sp
                        )
                    }
                }
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                StatBox(label = "PARTIDOS RECIENTES", value = "${player.recentMatches.size}", modifier = Modifier.weight(1f))
                StatBox(label = "EQUIPO ACTUAL", value = player.teamName ?: "—", modifier = Modifier.weight(1f))
                StatBox(label = "NACIONALIDAD", value = player.nationality ?: "—", modifier = Modifier.weight(1f))
            }
            Spacer(Modifier.height(16.dp))
            Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                InfoBanner(
                    "Goles, asistencias y calificación por partido requieren un proveedor de estadísticas " +
                        "avanzadas (no incluido en el plan gratuito de datos en vivo). El historial de partidos " +
                        "de abajo sí es 100% real y en vivo."
                )
            }
            Spacer(Modifier.height(16.dp))
            Text(
                "HISTORIAL RECIENTE",
                color = TextSecondary,
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
            Spacer(Modifier.height(8.dp))
        }

        if (player.recentMatches.isEmpty()) {
            item {
                Box(modifier = Modifier.padding(16.dp)) {
                    InfoBanner("No hay partidos recientes disponibles para este jugador todavía.")
                }
            }
        } else {
            items(player.recentMatches) { m ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(BgCard)
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            (if (m.wasHome) "vs " else "@ ") + m.opponent,
                            color = TextPrimary,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 13.sp
                        )
                        Text(m.competitionName, color = TextSecondary, fontSize = 11.sp)
                    }
                    Text(
                        "${m.homeScore ?: "-"} : ${m.awayScore ?: "-"}",
                        color = NextGoalBlue,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                }
            }
        }
        item { Spacer(Modifier.height(24.dp)) }
    }
}

@Composable
private fun StatBox(label: String, value: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(BgCard)
            .padding(vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(value, color = NextGoalBlue, fontWeight = FontWeight.Bold, fontSize = 15.sp, maxLines = 1)
        Spacer(Modifier.height(2.dp))
        Text(label, color = TextSecondary, fontSize = 9.sp, maxLines = 1)
    }
}
