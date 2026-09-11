package com.nextgoal.app

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.EmojiEvents
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.MenuBook
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.NotificationsNone
import androidx.compose.material.icons.outlined.NotificationsOff
import androidx.compose.material.icons.outlined.NotificationsActive
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.outlined.SportsSoccer
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import kotlinx.coroutines.delay
import java.time.Instant
import java.time.LocalDate
import java.time.format.DateTimeFormatter

private enum class AppTab(val label: String) {
    HOME("Inicio"),
    MATCHES("Partidos"),
    LEAGUES("Ligas"),
    NEWS("Noticias"),
    PROFILE("Perfil")
}

private data class MatchSection(val title: String, val matches: List<Match>)

private data class CompetitionOption(val key: String, val label: String)

private fun competitionKey(match: Match): String {
    return match.competitionCode.trim().uppercase(NEXT_GOAL_LOCALE).ifBlank {
        match.competition.trim().uppercase(NEXT_GOAL_LOCALE)
            .replace(Regex("[^A-Z0-9ÁÉÍÓÚÜÑ]+"), "_")
    }
}

private fun competitionLabel(match: Match): String {
    return when (competitionKey(match)) {
        "CL" -> "LIGA DE CAMPEONES"
        "PL" -> "PREMIER LEAGUE"
        "PD" -> "LA LIGA"
        "BL1" -> "BUNDESLIGA"
        "SA" -> "SERIE A"
        "FL1" -> "LIGUE 1"
        "DED" -> "EREDIVISIE"
        "PPL" -> "PRIMEIRA LIGA"
        "ELC" -> "CHAMPIONSHIP"
        "BSA" -> "BRASILEIRÃO"
        "LMX" -> "LIGA MX"
        else -> spanishCompetitionName(match.competition).uppercase(NEXT_GOAL_LOCALE)
    }
}

private fun competitionOptions(matches: List<Match>): List<CompetitionOption> {
    val supported = listOf(
        CompetitionOption("CL", "LIGA DE CAMPEONES"),
        CompetitionOption("PL", "PREMIER LEAGUE"),
        CompetitionOption("PD", "LA LIGA"),
        CompetitionOption("BL1", "BUNDESLIGA"),
        CompetitionOption("SA", "SERIE A"),
        CompetitionOption("FL1", "LIGUE 1"),
        CompetitionOption("DED", "EREDIVISIE"),
        CompetitionOption("PPL", "PRIMEIRA LIGA"),
        CompetitionOption("ELC", "CHAMPIONSHIP"),
        CompetitionOption("BSA", "BRASILEIRÃO"),
        CompetitionOption("LMX", "LIGA MX")
    )
    val loaded = matches
        .distinctBy(::competitionKey)
        .map { CompetitionOption(competitionKey(it), competitionLabel(it)) }
    val options = (supported + loaded)
        .distinctBy { it.key }
        .sortedBy { it.label }
    return listOf(CompetitionOption("TODAS", "TODAS")) + options
}

private fun Match.belongsToCompetition(filter: String): Boolean {
    return filter == "TODAS" || competitionKey(this) == filter
}

private fun Match.localDate(): LocalDate? {
    if (kickoffMillis <= 0L) return null
    return runCatching {
        Instant.ofEpochMilli(kickoffMillis).atZone(NEXT_GOAL_TIME_ZONE).toLocalDate()
    }.getOrNull()
}

private fun matchesForDate(matches: List<Match>, date: LocalDate, today: LocalDate): List<Match> {
    return matches.filter { match ->
        match.localDate() == date || (match.localDate() == null && date == today && match.phase == MatchPhase.UPCOMING)
    }.sortedWith(compareBy { if (it.kickoffMillis <= 0L) Long.MAX_VALUE else it.kickoffMillis })
}

private fun finishedChronologically(matches: List<Match>): List<Match> {
    return matches
        .filter { it.phase == MatchPhase.FINISHED }
        .sortedWith(compareByDescending { if (it.kickoffMillis <= 0L) Long.MIN_VALUE else it.kickoffMillis })
}

private fun dateSectionTitle(date: LocalDate, today: LocalDate): String {
    return when (date) {
        today -> "PARTIDOS DE HOY"
        today.plusDays(1) -> "PARTIDOS MAÑANA"
        else -> date.format(DateTimeFormatter.ofPattern("EEEE d MMM", NEXT_GOAL_LOCALE)).uppercase(NEXT_GOAL_LOCALE)
    }
}

private fun competitionSections(matches: List<Match>, prefix: String? = null): List<MatchSection> {
    return matches
        .groupBy(::competitionKey)
        .values
        .sortedBy { group ->
            group.minOfOrNull { it.kickoffMillis.takeIf { time -> time > 0L } ?: Long.MAX_VALUE }
                ?: Long.MAX_VALUE
        }
        .map { group ->
            val ordered = group.sortedWith(compareBy { it.kickoffMillis.takeIf { time -> time > 0L } ?: Long.MAX_VALUE })
            val label = competitionLabel(group.first())
            MatchSection(prefix?.let { "$label · $it" } ?: label, ordered)
        }
}

private fun matchSectionsForFilter(
    matches: List<Match>,
    filter: String,
    competitionFilter: String = "TODAS"
): List<MatchSection> {
    val today = LocalDate.now(NEXT_GOAL_TIME_ZONE)
    val filteredMatches = matches.filter { it.belongsToCompetition(competitionFilter) }
    return when (filter) {
        "EN VIVO" -> competitionSections(filteredMatches.filter { it.phase == MatchPhase.LIVE }, "EN VIVO")
        "HOY" -> competitionSections(matchesForDate(filteredMatches, today, today), "PARTIDOS DE HOY")
        "MAÑANA" -> {
            val tomorrow = today.plusDays(1)
            competitionSections(
                matchesForDate(filteredMatches, tomorrow, today).filter { it.phase == MatchPhase.UPCOMING },
                "PARTIDOS MAÑANA"
            )
        }
        "PRÓXIMOS" -> {
            (1L..7L).mapNotNull { offset ->
                val date = today.plusDays(offset)
                val dayMatches = matchesForDate(filteredMatches, date, today).filter { it.phase == MatchPhase.UPCOMING }
                competitionSections(dayMatches, dateSectionTitle(date, today)).takeIf { it.isNotEmpty() }
            }.flatten()
        }
        "FINALIZADOS" -> competitionSections(finishedChronologically(filteredMatches), "PARTIDOS FINALIZADOS")
        else -> emptyList()
    }.filter { it.matches.isNotEmpty() }
}

@Composable
fun NextGoalApp(viewModel: NextGoalViewModel = viewModel()) {
    val dashboard by viewModel.dashboard.collectAsStateWithLifecycle()
    val directory by viewModel.directory.collectAsStateWithLifecycle()
    val profile by viewModel.profile.collectAsStateWithLifecycle()
    val auth by viewModel.auth.collectAsStateWithLifecycle()
    var showSplash by rememberSaveable { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        delay(1_700)
        showSplash = false
    }

    if (showSplash) {
        SplashScreen()
    } else if (!auth.isAuthenticated) {
        AuthScreen(
            authState = auth,
            onLogin = viewModel::login,
            onRegister = viewModel::register,
            onClearError = viewModel::clearAuthError
        )
    } else {
        MainShell(
            dashboardState = dashboard,
            directoryState = directory,
            profile = profile,
            onRefresh = viewModel::refresh,
            onUpdateName = viewModel::updateProfileName,
            onUpdatePhoto = viewModel::updateProfilePhoto,
            onUpdateFavoriteTeam = viewModel::updateFavoriteTeam,
            onToggleNotifications = viewModel::toggleNotifications,
            onRequestVerification = viewModel::requestVerification,
            pendingVerification = auth.pendingVerification,
            profileError = auth.errorMessage,
            onConfirmVerification = viewModel::confirmVerification,
            onCancelVerification = viewModel::cancelVerification,
            onLogout = viewModel::logout,
            onSearch = {
                viewModel.clearDirectory()
            },
            onDirectorySearch = viewModel::searchDirectory,
            onDirectoryResult = viewModel::openDirectoryResult,
            onDirectoryRefresh = viewModel::refreshDirectoryDetails,
            onDirectoryBack = viewModel::closeDirectoryDetails,
            onDirectoryClose = viewModel::clearDirectory
        )
    }
}

@Composable
private fun AuthScreen(
    authState: AuthState,
    onLogin: (String, String) -> Unit,
    onRegister: (String, String, String, String, String) -> Unit,
    onClearError: () -> Unit
) {
    var registerMode by rememberSaveable { mutableStateOf(false) }
    var name by rememberSaveable { mutableStateOf("") }
    var email by rememberSaveable { mutableStateOf("") }
    var phone by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var confirmation by rememberSaveable { mutableStateOf("") }
    var identifier by rememberSaveable { mutableStateOf("") }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(NextGoalBackground)
            .statusBarsPadding()
            .navigationBarsPadding(),
        contentPadding = PaddingValues(start = 22.dp, end = 22.dp, top = 30.dp, bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Image(
                painter = painterResource(R.drawable.nextgoal_logo),
                contentDescription = "NextGoal",
                modifier = Modifier.fillMaxWidth().height(120.dp),
                contentScale = ContentScale.Fit
            )
        }
        item {
            Text(
                if (registerMode) "CREA TU CUENTA" else "INICIA SESIÓN",
                style = MaterialTheme.typography.headlineMedium
            )
            Spacer(Modifier.height(4.dp))
            Text(
                if (registerMode) "Guarda tus preferencias, equipos y alertas." else "Continúa con tus resultados personalizados.",
                color = NextGoalMuted,
                style = MaterialTheme.typography.bodyMedium
            )
        }
        if (registerMode) {
            item {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it; onClearError() },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Nombre completo") },
                    singleLine = true
                )
            }
            item {
                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it; onClearError() },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Correo electrónico") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                    singleLine = true
                )
            }
            item {
                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it; onClearError() },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Teléfono vinculado (opcional)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    singleLine = true
                )
            }
        } else {
            item {
                OutlinedTextField(
                    value = identifier,
                    onValueChange = { identifier = it; onClearError() },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Correo o teléfono") },
                    singleLine = true
                )
            }
        }
        item {
            OutlinedTextField(
                value = password,
                onValueChange = { password = it; onClearError() },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Contraseña") },
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                singleLine = true
            )
        }
        if (registerMode) {
            item {
                OutlinedTextField(
                    value = confirmation,
                    onValueChange = { confirmation = it; onClearError() },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Repite la contraseña") },
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    singleLine = true
                )
            }
        }
        authState.errorMessage?.let { message ->
            item { Text(message, color = NextGoalRed, style = MaterialTheme.typography.bodyMedium) }
        }
        item {
            Button(
                onClick = {
                    if (registerMode) onRegister(name, email, phone, password, confirmation)
                    else onLogin(identifier, password)
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = !authState.isBusy,
                shape = RoundedCornerShape(10.dp)
            ) {
                Text(if (registerMode) "Registrarme" else "Entrar", fontWeight = FontWeight.ExtraBold)
            }
        }
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                Text(
                    if (registerMode) "¿Ya tienes cuenta?" else "¿Todavía no tienes cuenta?",
                    color = NextGoalMuted,
                    style = MaterialTheme.typography.bodyMedium
                )
                TextButton(onClick = { registerMode = !registerMode; onClearError() }) {
                    Text(if (registerMode) "Iniciar sesión" else "Registrarme", color = NextGoalCyan, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun SplashScreen() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(NextGoalBackground)
            .statusBarsPadding()
            .navigationBarsPadding(),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Image(
                painter = painterResource(R.drawable.nextgoal_logo),
                contentDescription = "NextGoal",
                modifier = Modifier
                    .fillMaxWidth(0.82f)
                    .height(210.dp),
                contentScale = ContentScale.Fit
            )
            Spacer(Modifier.height(54.dp))
            Image(
                painter = painterResource(R.drawable.nextgoal_badge),
                contentDescription = "Cargando NextGoal",
                modifier = Modifier.size(100.dp),
                contentScale = ContentScale.Fit
            )
            Spacer(Modifier.height(18.dp))
            CircularProgressIndicator(
                modifier = Modifier.size(24.dp),
                color = NextGoalCyan,
                strokeWidth = 2.dp
            )
        }
    }
}

@Composable
private fun MainShell(
    dashboardState: DashboardState,
    directoryState: DirectoryState,
    profile: UserProfile,
    onRefresh: () -> Unit,
    onUpdateName: (String) -> Unit,
    onUpdatePhoto: (String?) -> Unit,
    onUpdateFavoriteTeam: (String) -> Unit,
    onToggleNotifications: () -> Unit,
    onRequestVerification: (VerificationKind, String) -> Unit,
    pendingVerification: VerificationRequest?,
    profileError: String?,
    onConfirmVerification: (String) -> Unit,
    onCancelVerification: () -> Unit,
    onLogout: () -> Unit,
    onSearch: () -> Unit,
    onDirectorySearch: (String) -> Unit,
    onDirectoryResult: (DirectoryResult) -> Unit,
    onDirectoryRefresh: () -> Unit,
    onDirectoryBack: () -> Unit,
    onDirectoryClose: () -> Unit
) {
    var selectedTab by rememberSaveable { mutableStateOf(AppTab.HOME) }
    var selectedMatchId by rememberSaveable { mutableStateOf<Int?>(null) }
    var selectedNewsTitle by rememberSaveable { mutableStateOf<String?>(null) }
    var searchOpen by rememberSaveable { mutableStateOf(false) }

    val selectedMatch = dashboardState.data.matches.firstOrNull { it.id == selectedMatchId }
    val selectedNews = dashboardState.data.news.firstOrNull { it.title == selectedNewsTitle }
    val directoryDetailsKey = when (val details = directoryState.selected) {
        is DirectoryDetails.Player -> "PLAYER:${details.id}"
        is DirectoryDetails.Team -> "TEAM:${details.id}"
        null -> null
    }
    LaunchedEffect(searchOpen, directoryDetailsKey) {
        if (!searchOpen || directoryDetailsKey == null) return@LaunchedEffect
        while (true) {
            delay(30_000)
            onDirectoryRefresh()
        }
    }
    if (searchOpen) {
        val details = directoryState.selected
        if (details != null) {
            DirectoryDetailScreen(details = details, onBack = onDirectoryBack)
        } else {
            DirectorySearchScreen(
                state = directoryState,
                onSearch = onDirectorySearch,
                onResultClick = onDirectoryResult,
                onBack = {
                    searchOpen = false
                    onDirectoryClose()
                }
            )
        }
        return
    }
    if (selectedMatch != null) {
        MatchDetailScreen(
            match = selectedMatch,
            onBack = { selectedMatchId = null }
        )
        return
    }
    if (selectedNews != null) {
        NewsDetailScreen(
            news = selectedNews,
            onBack = { selectedNewsTitle = null }
        )
        return
    }

    Scaffold(
        containerColor = NextGoalBackground,
        bottomBar = {
            BottomNavigationBar(
                selectedTab = selectedTab,
                onSelected = { selectedTab = it }
            )
        }
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            when (selectedTab) {
                AppTab.HOME -> HomeScreen(
                    state = dashboardState,
                    onRefresh = onRefresh,
                    onMatchClick = { selectedMatchId = it },
                    onNewsClick = { selectedNewsTitle = it },
                    onSearch = { searchOpen = true; onSearch() }
                )
                AppTab.MATCHES -> MatchesScreen(
                    state = dashboardState,
                    onMatchClick = { selectedMatchId = it },
                    onSearch = { searchOpen = true; onSearch() }
                )
                AppTab.LEAGUES -> LeaguesScreen(state = dashboardState, onSearch = { searchOpen = true; onSearch() })
                AppTab.NEWS -> NewsScreen(
                    state = dashboardState,
                    onNewsClick = { selectedNewsTitle = it },
                    onSearch = { searchOpen = true; onSearch() }
                )
                AppTab.PROFILE -> ProfileScreen(
                    profile = profile,
                    onUpdateName = onUpdateName,
                    onUpdatePhoto = onUpdatePhoto,
                    onUpdateFavoriteTeam = onUpdateFavoriteTeam,
                    onToggleNotifications = onToggleNotifications,
                    onRequestVerification = onRequestVerification,
                    profileError = profileError,
                    onLogout = onLogout
                )
            }
        }
    }
    if (pendingVerification != null) {
        VerificationDialog(
            request = pendingVerification,
            errorMessage = profileError,
            onConfirm = onConfirmVerification,
            onDismiss = onCancelVerification
        )
    }
}

@Composable
private fun BottomNavigationBar(
    selectedTab: AppTab,
    onSelected: (AppTab) -> Unit
) {
    Surface(
        color = NextGoalBackground,
        tonalElevation = 0.dp,
        modifier = Modifier.navigationBarsPadding()
    ) {
        Column {
            Divider(color = NextGoalCyan.copy(alpha = 0.75f), thickness = 1.dp)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(72.dp)
                    .padding(horizontal = 8.dp),
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                AppTab.entries.forEach { tab ->
                    val selected = tab == selectedTab
                    val icon = when (tab) {
                        AppTab.HOME -> Icons.Outlined.Home
                        AppTab.MATCHES -> Icons.Outlined.SportsSoccer
                        AppTab.LEAGUES -> Icons.Outlined.EmojiEvents
                        AppTab.NEWS -> Icons.Outlined.MenuBook
                        AppTab.PROFILE -> Icons.Outlined.Person
                    }
                    Column(
                        modifier = Modifier
                            .width(64.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { onSelected(tab) }
                            .padding(top = 9.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = tab.label,
                            tint = if (selected) NextGoalCyan else NextGoalMuted,
                            modifier = Modifier.size(23.dp)
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = tab.label,
                            color = if (selected) NextGoalCyan else NextGoalMuted,
                            fontSize = 11.sp,
                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
                        )
                        Spacer(Modifier.height(5.dp))
                        Box(
                            modifier = Modifier
                                .width(if (selected) 28.dp else 0.dp)
                                .height(2.dp)
                                .background(NextGoalCyan)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun HomeScreen(
    state: DashboardState,
    onRefresh: () -> Unit,
    onMatchClick: (Int) -> Unit,
    onNewsClick: (String) -> Unit,
    onSearch: () -> Unit
) {
    val liveMatch = state.data.matches.firstOrNull { it.phase == MatchPhase.LIVE }
    val today = LocalDate.now(NEXT_GOAL_TIME_ZONE)
    val tomorrow = today.plusDays(1)
    val upcomingToday = matchesForDate(state.data.matches, today, today).filter { it.phase == MatchPhase.UPCOMING }
    val upcomingTomorrow = matchesForDate(state.data.matches, tomorrow, today).filter { it.phase == MatchPhase.UPCOMING }
    val upcomingWeek = (2L..7L).flatMap { offset ->
        matchesForDate(state.data.matches, today.plusDays(offset), today).filter { it.phase == MatchPhase.UPCOMING }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 10.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            AppHeader(onRefresh = onRefresh, isRefreshing = state.isRefreshing, onSearch = onSearch)
        }
        item {
            Text(
                "${state.dataSourceLabel} · ${state.lastUpdatedLabel}",
                color = NextGoalMuted,
                fontSize = 10.sp,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.End
            )
        }
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        Modifier
                            .size(8.dp)
                            .background(NextGoalRed, CircleShape)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text("EN VIVO", style = MaterialTheme.typography.titleMedium)
                }
                Text(
                    text = if (liveMatch == null) "Sin partidos" else "Ver todos (${state.data.matches.count { it.phase == MatchPhase.LIVE }})",
                    color = NextGoalCyan,
                    style = MaterialTheme.typography.labelLarge
                )
            }
        }
        item {
            if (liveMatch != null) {
                LiveMatchCard(liveMatch, onClick = { onMatchClick(liveMatch.id) })
            } else {
                EmptyLiveCard()
            }
        }
        item { SectionTitle("LIGAS DESTACADAS") }
        item { CompetitionChips() }
        if (upcomingToday.isNotEmpty()) {
            item { SectionTitle("PARTIDOS DE HOY") }
            items(upcomingToday.take(4), key = { it.id }) { match ->
                UpcomingMatchCard(match, onClick = { onMatchClick(match.id) })
            }
        }
        if (upcomingTomorrow.isNotEmpty()) {
            item { SectionTitle("PARTIDOS MAÑANA") }
            items(upcomingTomorrow.take(4), key = { it.id }) { match ->
                UpcomingMatchCard(match, onClick = { onMatchClick(match.id) })
            }
        }
        if (upcomingWeek.isNotEmpty()) {
            item { SectionTitle("PRÓXIMOS 7 DÍAS") }
            items(upcomingWeek.take(6), key = { it.id }) { match ->
                UpcomingMatchCard(match, onClick = { onMatchClick(match.id) })
            }
        }
        if (upcomingToday.isEmpty() && upcomingTomorrow.isEmpty() && upcomingWeek.isEmpty()) {
            item { EmptyResults("No hay próximos partidos publicados para los siguientes 7 días.") }
        }
        item { SectionTitle("ÚLTIMA NOTICIA") }
        item {
            val latestNews = state.data.news.firstOrNull()
            if (latestNews == null) {
                EmptyResults("No hay noticias disponibles en la fuente actual.")
            } else {
                NewsHighlight(latestNews, onClick = { onNewsClick(latestNews.title) })
            }
        }
    }
}

@Composable
private fun AppHeader(
    onRefresh: (() -> Unit)?,
    isRefreshing: Boolean,
    onSearch: (() -> Unit)? = null
) {
    var activeDialog by remember { mutableStateOf<String?>(null) }
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .border(BorderStroke(1.dp, NextGoalCyan), RoundedCornerShape(8.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text("NG", color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 14.sp)
            }
            Spacer(Modifier.width(9.dp))
            Text("NEXT", color = NextGoalText, fontWeight = FontWeight.ExtraBold, fontSize = 18.sp)
            Text("GOAL", color = NextGoalCyan, fontWeight = FontWeight.ExtraBold, fontSize = 18.sp)
        }
        Row {
            if (onRefresh != null) {
                IconButton(onClick = { onRefresh() }) {
                    if (isRefreshing) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = NextGoalCyan,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Icon(Icons.Outlined.Refresh, contentDescription = "Actualizar", tint = NextGoalMuted)
                    }
                }
            }
            IconButton(onClick = { if (onSearch != null) onSearch() else activeDialog = "search" }) {
                Icon(Icons.Outlined.Search, contentDescription = "Buscar", tint = NextGoalText)
            }
            IconButton(onClick = { activeDialog = "notifications" }) {
                Icon(Icons.Outlined.NotificationsNone, contentDescription = "Notificaciones", tint = NextGoalText)
            }
        }
    }
    if (activeDialog != null) {
        AlertDialog(
            onDismissRequest = { activeDialog = null },
            containerColor = NextGoalSurface,
            title = { Text(if (activeDialog == "search") "Buscar en NextGoal" else "Notificaciones") },
            text = {
                Text(
                    if (activeDialog == "search") "Usa Partidos y Ligas para consultar la jornada y las competiciones disponibles."
                    else "Aquí aparecerán las alertas de tus equipos favoritos cuando actives la conexión de resultados.",
                    color = NextGoalMuted
                )
            },
            confirmButton = {
                TextButton(onClick = { activeDialog = null }) { Text("Cerrar", color = NextGoalCyan) }
            }
        )
    }
}

@Composable
private fun SectionTitle(title: String) {
    Text(
        text = title,
        color = NextGoalText,
        style = MaterialTheme.typography.titleMedium,
        modifier = Modifier.padding(top = 2.dp)
    )
}

@Composable
private fun CompetitionChips() {
    Row(
        modifier = Modifier.horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        listOf("Liga MX", "Liga de Campeones", "Premier League", "La Liga").forEachIndexed { index, label ->
            Surface(
                color = if (index == 0) NextGoalBackground else NextGoalSurface,
                shape = RoundedCornerShape(20.dp),
                border = if (index == 0) BorderStroke(1.dp, NextGoalCyan) else null
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 9.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(Modifier.size(17.dp).background(if (index == 0) NextGoalCyan else NextGoalMuted, CircleShape))
                    Spacer(Modifier.width(7.dp))
                    Text(
                        label,
                        color = if (index == 0) NextGoalText else NextGoalMuted,
                        style = MaterialTheme.typography.labelLarge,
                        maxLines = 1
                    )
                }
            }
        }
    }
}

@Composable
private fun DirectorySearchScreen(
    state: DirectoryState,
    onSearch: (String) -> Unit,
    onResultClick: (DirectoryResult) -> Unit,
    onBack: () -> Unit
) {
    var query by rememberSaveable { mutableStateOf(state.query) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(NextGoalBackground)
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.Outlined.ArrowBack, contentDescription = "Volver", tint = NextGoalText)
            }
            Text("BUSCAR", style = MaterialTheme.typography.headlineSmall)
        }
        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            label = { Text("Jugador o equipo") },
            placeholder = { Text("Ej. Mbappé, Real Madrid, América") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
            trailingIcon = {
                IconButton(onClick = { onSearch(query) }, enabled = query.trim().length >= 2) {
                    Icon(Icons.Outlined.Search, contentDescription = "Buscar", tint = NextGoalCyan)
                }
            }
        )
        Text(
            "Resultados actuales de jugadores y equipos de todo el mundo",
            color = NextGoalMuted,
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.padding(horizontal = 18.dp, vertical = 10.dp)
        )
        when {
            state.isSearching -> Box(Modifier.fillMaxWidth().padding(top = 26.dp), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = NextGoalCyan, modifier = Modifier.size(28.dp), strokeWidth = 2.dp)
            }
            state.errorMessage != null && state.results.isEmpty() -> EmptyResults(state.errorMessage)
            state.results.isEmpty() -> EmptyResults("Escribe al menos dos caracteres para buscar.")
            else -> LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(9.dp)
            ) {
                items(state.results, key = { "${it.type}-${it.id}" }) { result ->
                    DirectoryResultCard(result, onClick = { onResultClick(result) })
                }
            }
        }
    }
}

@Composable
private fun DirectoryResultCard(result: DirectoryResult, onClick: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        color = NextGoalSurface,
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, NextGoalLine)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier.size(56.dp).clip(RoundedCornerShape(10.dp)).background(Color.White.copy(alpha = 0.96f)),
                contentAlignment = Alignment.Center
            ) {
                AsyncImage(
                    model = result.imageUrl,
                    contentDescription = "Imagen de ${result.name}",
                    modifier = Modifier.size(48.dp),
                    contentScale = ContentScale.Fit,
                    placeholder = painterResource(R.drawable.team_placeholder),
                    error = painterResource(R.drawable.team_placeholder)
                )
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(result.name, color = NextGoalText, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(result.subtitle, color = NextGoalMuted, fontSize = 12.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Text(
                    if (result.type == DirectoryEntityType.PLAYER) "JUGADOR" else "EQUIPO",
                    color = NextGoalCyan,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.ExtraBold
                )
            }
            Icon(Icons.Outlined.ArrowBack, contentDescription = null, tint = NextGoalMuted, modifier = Modifier.size(18.dp).rotate(180f))
        }
    }
}

@Composable
private fun DirectoryDetailScreen(details: DirectoryDetails, onBack: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(NextGoalBackground)
            .statusBarsPadding()
            .navigationBarsPadding()
            .verticalScroll(rememberScrollState())
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.Outlined.ArrowBack, contentDescription = "Volver", tint = NextGoalText)
            }
            Text("INFORMACIÓN", style = MaterialTheme.typography.headlineSmall)
        }
        when (details) {
            is DirectoryDetails.Player -> PlayerDirectoryDetail(details)
            is DirectoryDetails.Team -> TeamDirectoryDetail(details)
        }
    }
}

@Composable
private fun PlayerDirectoryDetail(player: DirectoryDetails.Player) {
    Column(Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 8.dp)) {
        Box(
            modifier = Modifier.fillMaxWidth().height(210.dp).clip(RoundedCornerShape(16.dp)).background(NextGoalSurface),
            contentAlignment = Alignment.Center
        ) {
            AsyncImage(
                model = player.photoUrl,
                contentDescription = "Foto de ${player.name}",
                modifier = Modifier.fillMaxSize().padding(12.dp),
                contentScale = ContentScale.Fit,
                placeholder = painterResource(R.drawable.team_placeholder),
                error = painterResource(R.drawable.team_placeholder)
            )
        }
        Spacer(Modifier.height(16.dp))
        Text(player.name, style = MaterialTheme.typography.headlineMedium)
        Text(player.position, color = NextGoalCyan, fontWeight = FontWeight.Bold)
        if (player.teamBadgeUrl != null) {
            Row(Modifier.padding(top = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                AsyncImage(
                    model = player.teamBadgeUrl,
                    contentDescription = "Escudo de ${player.team}",
                    modifier = Modifier.size(30.dp),
                    contentScale = ContentScale.Fit,
                    placeholder = painterResource(R.drawable.team_placeholder),
                    error = painterResource(R.drawable.team_placeholder)
                )
                Spacer(Modifier.width(8.dp))
                Text(player.team, color = NextGoalText, fontWeight = FontWeight.SemiBold)
            }
        } else {
            Text(player.team, color = NextGoalText, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(top = 12.dp))
        }
        DirectoryScheduleSection(nextMatch = player.nextMatch, lastMatch = player.lastMatch)
        Spacer(Modifier.height(16.dp))
        DetailInfoRow("Nacionalidad", player.nationality)
        DetailInfoRow("Fecha de nacimiento", player.birthDate)
        DetailInfoRow("Lugar de nacimiento", player.birthPlace)
        DetailInfoRow("Altura", player.height)
        DetailInfoRow("Peso", player.weight)
        DetailSectionTitle("BIOGRAFÍA")
        Text(player.description, color = NextGoalMuted, style = MaterialTheme.typography.bodyMedium, lineHeight = 21.sp)
    }
}

@Composable
private fun TeamDirectoryDetail(team: DirectoryDetails.Team) {
    val context = LocalContext.current
    Column(Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 8.dp)) {
        team.bannerUrl?.let { banner ->
            AsyncImage(
                model = banner,
                contentDescription = "Imagen de ${team.name}",
                modifier = Modifier.fillMaxWidth().height(150.dp).clip(RoundedCornerShape(16.dp)),
                contentScale = ContentScale.Crop
            )
            Spacer(Modifier.height(10.dp))
        }
        Box(
            modifier = Modifier.fillMaxWidth().height(150.dp).clip(RoundedCornerShape(16.dp)).background(Color.White.copy(alpha = 0.96f)),
            contentAlignment = Alignment.Center
        ) {
            AsyncImage(
                model = team.logoUrl,
                contentDescription = "Logo de ${team.name}",
                modifier = Modifier.size(116.dp),
                contentScale = ContentScale.Fit,
                placeholder = painterResource(R.drawable.team_placeholder),
                error = painterResource(R.drawable.team_placeholder)
            )
        }
        Spacer(Modifier.height(16.dp))
        Text(team.name, style = MaterialTheme.typography.headlineMedium)
        Text(team.league, color = NextGoalCyan, fontWeight = FontWeight.Bold)
        DetailInfoRow("País", team.country)
        DetailInfoRow("Fundado", team.founded)
        DetailInfoRow("Estadio", team.stadium)
        DetailInfoRow("Capacidad", team.stadiumCapacity)
        DirectoryScheduleSection(nextMatch = team.nextMatch, lastMatch = team.lastMatch)
        team.website?.let { website ->
            TextButton(onClick = {
                val url = if (website.startsWith("http")) website else "https://$website"
                context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
            }) {
                Text("VISITAR SITIO OFICIAL", color = NextGoalCyan, fontWeight = FontWeight.Bold)
            }
        }
        DetailSectionTitle("INFORMACIÓN DEL EQUIPO")
        Text(team.description, color = NextGoalMuted, style = MaterialTheme.typography.bodyMedium, lineHeight = 21.sp)
    }
}

@Composable
private fun DirectoryScheduleSection(
    nextMatch: DirectoryFixture?,
    lastMatch: DirectoryFixture?
) {
    DetailSectionTitle("CALENDARIO Y RESULTADOS")
    Text("PRÓXIMO PARTIDO", color = NextGoalMuted, fontSize = 11.sp, fontWeight = FontWeight.ExtraBold)
    if (nextMatch != null) {
        DirectoryFixtureCard(nextMatch)
    } else {
        Text("No hay un próximo partido publicado por la fuente.", color = NextGoalMuted, fontSize = 13.sp, modifier = Modifier.padding(vertical = 10.dp))
    }
    Spacer(Modifier.height(10.dp))
    Text("PARTIDO ANTERIOR", color = NextGoalMuted, fontSize = 11.sp, fontWeight = FontWeight.ExtraBold)
    if (lastMatch != null) {
        DirectoryFixtureCard(lastMatch)
    } else {
        Text("No hay un partido anterior publicado por la fuente.", color = NextGoalMuted, fontSize = 13.sp, modifier = Modifier.padding(vertical = 10.dp))
    }
}

@Composable
private fun DirectoryFixtureCard(fixture: DirectoryFixture) {
    val statusColor = when (fixture.statusLabel) {
        "EN VIVO" -> NextGoalRed
        "FINALIZADO" -> NextGoalMuted
        else -> NextGoalCyan
    }
    Surface(
        modifier = Modifier.fillMaxWidth().padding(top = 7.dp),
        color = NextGoalSurface,
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, if (fixture.statusLabel == "EN VIVO") NextGoalCyan else NextGoalLine)
    ) {
        Column(Modifier.padding(12.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(fixture.competition, color = NextGoalCyan, fontSize = 11.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                Spacer(Modifier.width(8.dp))
                Text(fixture.statusLabel, color = statusColor, fontSize = 10.sp, fontWeight = FontWeight.ExtraBold)
            }
            Text(fixture.kickoffLabel, color = NextGoalMuted, fontSize = 11.sp, modifier = Modifier.padding(top = 3.dp))
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                DirectoryFixtureTeam(fixture.homeTeam, fixture.homeLogoUrl, Modifier.weight(1f))
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.width(72.dp)) {
                    Text(
                        if (fixture.homeScore != null && fixture.awayScore != null) "${fixture.homeScore} - ${fixture.awayScore}" else "VS",
                        color = NextGoalText,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.ExtraBold,
                        textAlign = TextAlign.Center
                    )
                    if (fixture.venue.isNotBlank()) {
                        Text(fixture.venue, color = NextGoalMuted, fontSize = 9.sp, maxLines = 2, overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Center)
                    }
                }
                DirectoryFixtureTeam(fixture.awayTeam, fixture.awayLogoUrl, Modifier.weight(1f))
            }
            if (fixture.stats.isNotEmpty()) {
                Spacer(Modifier.height(12.dp))
                Divider(color = NextGoalLine)
                Text("ESTADÍSTICAS PUBLICADAS", color = NextGoalMuted, fontSize = 10.sp, fontWeight = FontWeight.ExtraBold, modifier = Modifier.padding(top = 10.dp, bottom = 4.dp))
                fixture.stats.forEach { stat ->
                    Row(Modifier.fillMaxWidth().padding(vertical = 3.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(stat.homeValue, color = NextGoalText, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        Text(stat.label, color = NextGoalMuted, fontSize = 11.sp)
                        Text(stat.awayValue, color = NextGoalText, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                }
            } else if (fixture.statusLabel == "FINALIZADO") {
                Text("La fuente no publicó estadísticas detalladas para este partido.", color = NextGoalMuted, fontSize = 11.sp, modifier = Modifier.padding(top = 10.dp))
            }
        }
    }
}

@Composable
private fun DirectoryFixtureTeam(name: String, logoUrl: String?, modifier: Modifier = Modifier) {
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        AsyncImage(
            model = logoUrl,
            contentDescription = "Escudo de $name",
            modifier = Modifier.size(40.dp),
            contentScale = ContentScale.Fit,
            placeholder = painterResource(R.drawable.team_placeholder),
            error = painterResource(R.drawable.team_placeholder)
        )
        Spacer(Modifier.height(5.dp))
        Text(name, color = NextGoalText, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center, maxLines = 2, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
private fun DetailSectionTitle(title: String) {
    Text(title, color = NextGoalText, fontWeight = FontWeight.ExtraBold, modifier = Modifier.padding(top = 18.dp, bottom = 8.dp))
}

@Composable
private fun DetailInfoRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 7.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top
    ) {
        Text(label, color = NextGoalMuted, fontSize = 13.sp)
        Spacer(Modifier.width(18.dp))
        Text(value, color = NextGoalText, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, textAlign = TextAlign.End)
    }
}

@Composable
private fun LiveMatchCard(match: Match, onClick: () -> Unit) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(10.dp, RoundedCornerShape(18.dp), spotColor = NextGoalCyan.copy(alpha = 0.4f))
            .clickable(onClick = onClick),
        color = NextGoalSurface,
        shape = RoundedCornerShape(18.dp),
        border = BorderStroke(1.dp, NextGoalCyan)
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(competitionLabel(match), color = NextGoalCyan, style = MaterialTheme.typography.labelLarge)
                Surface(color = NextGoalRed, shape = RoundedCornerShape(5.dp)) {
                    Text(
                        "${match.minute ?: 0}'",
                        color = Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.ExtraBold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }
            Spacer(Modifier.height(14.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                TeamColumn(match.home, Modifier.weight(1f))
                ScoreColumn(match.homeScore, match.awayScore, live = true)
                TeamColumn(match.away, Modifier.weight(1f))
            }
            Spacer(Modifier.height(12.dp))
            Divider(color = NextGoalLine)
            Spacer(Modifier.height(10.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("En directo · ${match.venue}", color = NextGoalMuted, style = MaterialTheme.typography.bodyMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text("›", color = NextGoalCyan, fontSize = 22.sp)
            }
        }
    }
}

@Composable
private fun EmptyLiveCard() {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = NextGoalSurface,
        shape = RoundedCornerShape(18.dp),
        border = BorderStroke(1.dp, NextGoalLine)
    ) {
        Row(
            modifier = Modifier.padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Outlined.CalendarMonth, contentDescription = null, tint = NextGoalCyan)
            Spacer(Modifier.width(12.dp))
            Column {
                Text("No hay partidos en vivo", style = MaterialTheme.typography.titleMedium)
                Text("Actualiza para consultar la jornada actual.", color = NextGoalMuted, style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}

@Composable
private fun UpcomingMatchCard(match: Match, onClick: () -> Unit) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        color = NextGoalSurface,
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, NextGoalLine)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 11.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.width(72.dp)) {
                Text(match.kickoffLabel, color = NextGoalCyan, fontWeight = FontWeight.ExtraBold, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(match.competitionShort, color = NextGoalMuted, fontSize = 10.sp)
            }
            Divider(Modifier.height(32.dp).width(1.dp), color = NextGoalLine)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                TeamLine(match.home)
                TeamLine(match.away)
            }
            Icon(
                Icons.Outlined.NotificationsNone,
                contentDescription = "Activar alerta",
                tint = NextGoalMuted,
                modifier = Modifier.size(21.dp)
            )
        }
    }
}

@Composable
private fun NewsHighlight(news: NewsItem, onClick: () -> Unit = {}) {
    Surface(
        modifier = Modifier.clickable(onClick = onClick),
        color = NextGoalSurface,
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, NextGoalLine)
    ) {
        Column {
            AsyncImage(
                model = news.imageUrl,
                contentDescription = news.title,
                modifier = Modifier.fillMaxWidth().height(155.dp),
                contentScale = ContentScale.Crop,
                error = painterResource(R.drawable.nextgoal_logo)
            )
            Column(Modifier.padding(14.dp)) {
                Text(news.category, color = NextGoalCyan, style = MaterialTheme.typography.labelLarge)
                Spacer(Modifier.height(7.dp))
                Text(news.title, style = MaterialTheme.typography.titleLarge)
                Spacer(Modifier.height(6.dp))
                Text(news.summary, color = NextGoalMuted, style = MaterialTheme.typography.bodyMedium, maxLines = 2, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}

@Composable
private fun TeamLine(team: Team) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        TeamCrest(team, 22.dp)
        Spacer(Modifier.width(8.dp))
        Text(team.shortName, color = NextGoalText, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun TeamColumn(team: Team, modifier: Modifier = Modifier) {
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        TeamCrest(team, 43.dp)
        Spacer(Modifier.height(7.dp))
        Text(
            team.shortName,
            color = NextGoalText,
            style = MaterialTheme.typography.labelLarge,
            textAlign = TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun ScoreColumn(homeScore: Int?, awayScore: Int?, live: Boolean) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(horizontal = 9.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(homeScore?.toString() ?: "-", fontSize = 32.sp, fontWeight = FontWeight.ExtraBold)
            Text(" : ", color = if (live) NextGoalCyan else NextGoalMuted, fontSize = 25.sp, fontWeight = FontWeight.Bold)
            Text(awayScore?.toString() ?: "-", fontSize = 32.sp, fontWeight = FontWeight.ExtraBold)
        }
        if (live) {
            Surface(color = NextGoalCyan.copy(alpha = 0.14f), shape = RoundedCornerShape(4.dp)) {
                Text("EN VIVO", color = NextGoalCyan, fontSize = 9.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp))
            }
        }
    }
}

@Composable
private fun TeamCrest(team: Team, size: androidx.compose.ui.unit.Dp) {
    Box(
        modifier = Modifier
            .size(size)
            .clip(RoundedCornerShape(9.dp))
            .background(Color.White.copy(alpha = 0.96f)),
        contentAlignment = Alignment.Center
    ) {
        AsyncImage(
            model = team.crestUrl,
            contentDescription = "Escudo de ${team.name}",
            modifier = Modifier.size(size * 0.82f),
            contentScale = ContentScale.Fit,
            placeholder = painterResource(R.drawable.team_placeholder),
            error = painterResource(R.drawable.team_placeholder)
        )
    }
}

@Composable
private fun MatchesScreen(
    state: DashboardState,
    onMatchClick: (Int) -> Unit,
    onSearch: () -> Unit
) {
    var selectedFilter by rememberSaveable { mutableStateOf("EN VIVO") }
    var selectedCompetition by rememberSaveable { mutableStateOf("TODAS") }
    val filters = listOf("EN VIVO", "HOY", "MAÑANA", "PRÓXIMOS", "FINALIZADOS")
    val competitions = competitionOptions(state.data.matches)
    val activeCompetition = competitions.firstOrNull { it.key == selectedCompetition }?.key ?: "TODAS"
    val sections = matchSectionsForFilter(state.data.matches, selectedFilter, activeCompetition)

    Column(Modifier.fillMaxSize().statusBarsPadding()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("PARTIDOS", style = MaterialTheme.typography.headlineMedium)
            Surface(
                modifier = Modifier.clickable { onSearch() },
                color = NextGoalSurface,
                shape = CircleShape
            ) {
                Icon(Icons.Outlined.Search, contentDescription = "Buscar jugadores y equipos", tint = NextGoalText, modifier = Modifier.padding(9.dp).size(20.dp))
            }
        }
        FilterChips(filters, selectedFilter) { selectedFilter = it }
        Text(
            text = "COMPETICIONES",
            color = NextGoalMuted,
            style = MaterialTheme.typography.labelMedium,
            modifier = Modifier.padding(start = 16.dp, top = 13.dp, bottom = 7.dp)
        )
        CompetitionChips(competitions, activeCompetition) { selectedCompetition = it }
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 18.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(11.dp)
        ) {
            if (sections.isEmpty()) {
                item { EmptyResults(message = "No hay partidos en esta vista.") }
            } else {
                sections.forEach { section ->
                    item(key = "heading-${section.title}") { SectionTitle(section.title) }
                    items(section.matches, key = { it.id }) { match ->
                        MatchListCard(match, onClick = { onMatchClick(match.id) })
                    }
                }
            }
            item {
                Text(
                    text = if (selectedFilter == "FINALIZADOS") "Más recientes primero · ${state.lastUpdatedLabel}" else state.lastUpdatedLabel,
                    color = NextGoalMuted,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
private fun CompetitionChips(
    options: List<CompetitionOption>,
    selected: String,
    onSelected: (String) -> Unit
) {
    Row(
        modifier = Modifier.horizontalScroll(rememberScrollState()).padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        options.forEach { option ->
            Surface(
                modifier = Modifier.clickable { onSelected(option.key) },
                color = if (option.key == selected) NextGoalCyan else NextGoalSurface,
                shape = RoundedCornerShape(18.dp),
                border = BorderStroke(1.dp, if (option.key == selected) NextGoalCyan else NextGoalLine)
            ) {
                Text(
                    option.label,
                    color = if (option.key == selected) Color(0xFF00151B) else NextGoalMuted,
                    style = MaterialTheme.typography.labelLarge,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 9.dp)
                )
            }
        }
    }
}

@Composable
private fun FilterChips(
    labels: List<String>,
    selected: String,
    onSelected: (String) -> Unit
) {
    Row(
        modifier = Modifier.horizontalScroll(rememberScrollState()).padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        labels.forEach { label ->
            Surface(
                modifier = Modifier.clickable { onSelected(label) },
                color = if (label == selected) NextGoalCyan else NextGoalSurface,
                shape = RoundedCornerShape(18.dp),
                border = BorderStroke(1.dp, if (label == selected) NextGoalCyan else NextGoalLine)
            ) {
                Text(
                    label,
                    color = if (label == selected) Color(0xFF00151B) else NextGoalMuted,
                    style = MaterialTheme.typography.labelLarge,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 9.dp)
                )
            }
        }
    }
}

@Composable
private fun MatchListCard(match: Match, onClick: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        color = NextGoalSurface,
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, if (match.phase == MatchPhase.LIVE) NextGoalCyan else NextGoalLine)
    ) {
        Column(Modifier.padding(14.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(competitionLabel(match), color = NextGoalCyan, style = MaterialTheme.typography.labelLarge)
                val status = when (match.phase) {
                    MatchPhase.LIVE -> "${match.minute ?: 0}' EN VIVO"
                    MatchPhase.FINISHED -> "FINAL"
                    MatchPhase.UPCOMING -> match.kickoffLabel
                }
                Text(status, color = if (match.phase == MatchPhase.LIVE) NextGoalRed else NextGoalMuted, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.height(12.dp))
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                TeamLine(match.home)
                Spacer(Modifier.weight(1f))
                ScoreColumn(match.homeScore, match.awayScore, match.phase == MatchPhase.LIVE)
                Spacer(Modifier.weight(1f))
                Row(horizontalArrangement = Arrangement.End, verticalAlignment = Alignment.CenterVertically) {
                    Text(match.away.shortName, color = NextGoalText, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, textAlign = TextAlign.End)
                    Spacer(Modifier.width(8.dp))
                    TeamCrest(match.away, 25.dp)
                }
            }
        }
    }
}

@Composable
private fun LeaguesScreen(state: DashboardState, onSearch: () -> Unit) {
    var selectedLeague by rememberSaveable { mutableStateOf("LIGA MX") }
    val leagues = listOf(
        "LIGA MX",
        "LIGA DE CAMPEONES",
        "PREMIER LEAGUE",
        "LA LIGA",
        "BUNDESLIGA",
        "SERIE A",
        "LIGUE 1",
        "EREDIVISIE",
        "PRIMEIRA LIGA",
        "CHAMPIONSHIP",
        "BRASILEIRÃO"
    )
    val rows = state.data.standings[selectedLeague].orEmpty()

    Column(Modifier.fillMaxSize().statusBarsPadding()) {
        AppHeader(onRefresh = null, isRefreshing = false, onSearch = onSearch)
        Spacer(Modifier.height(6.dp))
        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()).padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            leagues.forEach { league ->
                Surface(
                    modifier = Modifier.clickable { selectedLeague = league },
                    color = if (selectedLeague == league) NextGoalCyan else NextGoalSurface,
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Text(
                        league,
                        color = if (selectedLeague == league) Color(0xFF00151B) else NextGoalMuted,
                        style = MaterialTheme.typography.labelLarge,
                        modifier = Modifier.padding(horizontal = 15.dp, vertical = 10.dp)
                    )
                }
            }
        }
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(0.dp)
        ) {
            if (rows.isEmpty()) {
                item { EmptyResults("Esta competición no tiene tabla disponible en la fuente actual.") }
            } else {
                item {
                    Surface(
                        color = NextGoalSurface,
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(1.dp, NextGoalLine)
                    ) {
                        Column(Modifier.padding(8.dp)) {
                            StandingHeader()
                            rows.forEachIndexed { index, row ->
                                StandingRowItem(row, highlighted = index == 0)
                                if (index != rows.lastIndex) Divider(color = NextGoalLine.copy(alpha = 0.65f))
                            }
                        }
                    }
                }
            }
            item {
                Text(
                    "Tabla actualizada según la competición seleccionada",
                    color = NextGoalMuted,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.fillMaxWidth().padding(top = 14.dp),
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
private fun StandingHeader() {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text("#", color = NextGoalMuted, fontSize = 11.sp, modifier = Modifier.width(26.dp))
        Text("CLUB", color = NextGoalMuted, fontSize = 11.sp, modifier = Modifier.weight(1f))
        Text("PJ", color = NextGoalMuted, fontSize = 11.sp, modifier = Modifier.width(34.dp), textAlign = TextAlign.Center)
        Text("DG", color = NextGoalMuted, fontSize = 11.sp, modifier = Modifier.width(38.dp), textAlign = TextAlign.Center)
        Text("PTS", color = NextGoalMuted, fontSize = 11.sp, modifier = Modifier.width(38.dp), textAlign = TextAlign.Center)
    }
}

@Composable
private fun StandingRowItem(row: StandingRow, highlighted: Boolean) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(if (highlighted) NextGoalSurfaceRaised else Color.Transparent)
            .padding(horizontal = 8.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            row.position.toString(),
            color = if (row.position <= 4) NextGoalCyan else NextGoalText,
            fontWeight = FontWeight.Bold,
            fontSize = 13.sp,
            modifier = Modifier.width(26.dp)
        )
        Row(Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
            TeamCrest(row.team, 23.dp)
            Spacer(Modifier.width(8.dp))
            Text(row.team.shortName, color = NextGoalText, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        Text(row.played.toString(), color = NextGoalMuted, fontSize = 12.sp, modifier = Modifier.width(34.dp), textAlign = TextAlign.Center)
        Text(if (row.goalDifference >= 0) "+${row.goalDifference}" else row.goalDifference.toString(), color = NextGoalMuted, fontSize = 12.sp, modifier = Modifier.width(38.dp), textAlign = TextAlign.Center)
        Text(row.points.toString(), color = if (row.position == 1) NextGoalCyan else NextGoalText, fontSize = 13.sp, fontWeight = FontWeight.Bold, modifier = Modifier.width(38.dp), textAlign = TextAlign.Center)
    }
}

@Composable
private fun NewsScreen(
    state: DashboardState,
    onNewsClick: (String) -> Unit,
    onSearch: () -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize().statusBarsPadding(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 10.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item { AppHeader(onRefresh = null, isRefreshing = false, onSearch = onSearch) }
        item { SectionTitle("NOTICIAS") }
        item {
            val latestNews = state.data.news.firstOrNull()
            if (latestNews == null) {
                EmptyResults("No hay noticias disponibles en la fuente actual.")
            } else {
                NewsHighlight(latestNews, onClick = { onNewsClick(latestNews.title) })
            }
        }
        item { SectionTitle("MÁS NOTICIAS") }
        items(state.data.news.drop(1), key = { it.title }) { news -> NewsListItem(news, onClick = { onNewsClick(news.title) }) }
    }
}

@Composable
private fun NewsListItem(news: NewsItem, onClick: () -> Unit) {
    Surface(
        modifier = Modifier.clickable(onClick = onClick),
        color = NextGoalSurface,
        shape = RoundedCornerShape(13.dp),
        border = BorderStroke(1.dp, NextGoalLine)
    ) {
        Row(Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
            AsyncImage(
                model = news.imageUrl,
                contentDescription = news.title,
                modifier = Modifier.size(74.dp).clip(RoundedCornerShape(9.dp)),
                contentScale = ContentScale.Crop,
                error = painterResource(R.drawable.nextgoal_logo)
            )
            Spacer(Modifier.width(11.dp))
            Column(Modifier.weight(1f)) {
                Text(news.category, color = NextGoalCyan, fontSize = 10.sp, fontWeight = FontWeight.ExtraBold)
                Spacer(Modifier.height(3.dp))
                Text(news.title, color = NextGoalText, fontSize = 13.sp, fontWeight = FontWeight.Bold, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Spacer(Modifier.height(3.dp))
                Text(news.timeLabel, color = NextGoalMuted, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}

@Composable
private fun NewsDetailScreen(news: NewsItem, onBack: () -> Unit) {
    val context = LocalContext.current
    val sourceUrl = news.sourceUrl?.trim().orEmpty()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(NextGoalBackground)
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.Outlined.ArrowBack, contentDescription = "Volver", tint = NextGoalText)
            }
            Text("NOTICIA", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f), textAlign = TextAlign.Center)
            Spacer(Modifier.width(48.dp))
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 30.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                AsyncImage(
                    model = news.imageUrl,
                    contentDescription = news.title,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(220.dp)
                        .clip(RoundedCornerShape(16.dp)),
                    contentScale = ContentScale.Crop,
                    error = painterResource(R.drawable.nextgoal_logo)
                )
            }
            item {
                Text(news.category, color = NextGoalCyan, fontSize = 11.sp, fontWeight = FontWeight.ExtraBold)
                Spacer(Modifier.height(5.dp))
                Text(news.title, style = MaterialTheme.typography.headlineSmall)
                Spacer(Modifier.height(6.dp))
                Text(news.timeLabel, color = NextGoalMuted, fontSize = 12.sp)
            }
            item {
                Text(
                    news.summary,
                    color = NextGoalText,
                    style = MaterialTheme.typography.bodyLarge,
                    lineHeight = 24.sp
                )
            }
            if (sourceUrl.isNotBlank()) {
                item {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                runCatching {
                                    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(sourceUrl)))
                                }
                            },
                        color = NextGoalCyan,
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text(
                            "Leer artículo completo en ${news.category}",
                            color = Color(0xFF00151B),
                            fontWeight = FontWeight.ExtraBold,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 13.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ProfileScreen(
    profile: UserProfile,
    onUpdateName: (String) -> Unit,
    onUpdatePhoto: (String?) -> Unit,
    onUpdateFavoriteTeam: (String) -> Unit,
    onToggleNotifications: () -> Unit,
    onRequestVerification: (VerificationKind, String) -> Unit,
    profileError: String?,
    onLogout: () -> Unit
) {
    var showNameDialog by remember { mutableStateOf(false) }
    var showEmailDialog by remember { mutableStateOf(false) }
    var showPhoneDialog by remember { mutableStateOf(false) }
    var showPasswordDialog by remember { mutableStateOf(false) }
    val favoriteOptions = listOf("Real Madrid", "FC Barcelona", "América", "Arsenal")
    val context = LocalContext.current
    val photoLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let {
            runCatching {
                val flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
                context.contentResolver.takePersistableUriPermission(it, flags)
            }
        }
        onUpdatePhoto(uri?.toString())
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize().statusBarsPadding(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 10.dp, bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Spacer(Modifier.size(48.dp))
                Text("PERFIL", style = MaterialTheme.typography.titleLarge)
                IconButton(onClick = { showNameDialog = true }) { Icon(Icons.Outlined.Edit, contentDescription = "Editar nombre", tint = NextGoalCyan) }
            }
        }
        item {
            Surface(
                color = NextGoalSurface,
                shape = RoundedCornerShape(18.dp),
                border = BorderStroke(1.dp, NextGoalCyan.copy(alpha = 0.7f))
            ) {
                Row(Modifier.fillMaxWidth().padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
                    ProfilePhoto(profile = profile, onChange = { photoLauncher.launch(arrayOf("image/*")) })
                    Spacer(Modifier.width(14.dp))
                    Column(Modifier.weight(1f)) {
                        Text(profile.name, style = MaterialTheme.typography.titleLarge)
                        Spacer(Modifier.height(3.dp))
                        Text(profile.email, color = NextGoalMuted, style = MaterialTheme.typography.bodyMedium)
                        if (profile.phoneNumber.isNotBlank()) {
                            Spacer(Modifier.height(2.dp))
                            Text(profile.phoneNumber, color = NextGoalMuted, fontSize = 12.sp)
                        }
                        Spacer(Modifier.height(7.dp))
                            TextButton(onClick = { photoLauncher.launch(arrayOf("image/*")) }) {
                            Text("Cambiar foto", color = NextGoalCyan, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
        profileError?.let { error ->
            item { Text(error, color = NextGoalRed, style = MaterialTheme.typography.bodyMedium) }
        }
        item {
            SectionTitle("DATOS PERSONALES")
            Spacer(Modifier.height(8.dp))
            PreferenceRow(
                icon = Icons.Outlined.Edit,
                title = "Editar nombre",
                subtitle = "Actualiza cómo aparece tu perfil",
                enabled = true,
                onClick = { showNameDialog = true }
            )
        }
        item {
            SectionTitle("MI EQUIPO FAVORITO")
            Spacer(Modifier.height(8.dp))
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                favoriteOptions.forEach { team ->
                    Surface(
                        modifier = Modifier.clickable { onUpdateFavoriteTeam(team) },
                        color = if (profile.favoriteTeam == team) NextGoalCyan.copy(alpha = 0.16f) else NextGoalSurface,
                        shape = RoundedCornerShape(20.dp),
                        border = BorderStroke(1.dp, if (profile.favoriteTeam == team) NextGoalCyan else NextGoalLine)
                    ) {
                        Row(Modifier.padding(horizontal = 12.dp, vertical = 9.dp), verticalAlignment = Alignment.CenterVertically) {
                            if (profile.favoriteTeam == team) {
                                Icon(Icons.Outlined.Check, contentDescription = null, tint = NextGoalCyan, modifier = Modifier.size(15.dp))
                                Spacer(Modifier.width(4.dp))
                            }
                            Text(team, color = if (profile.favoriteTeam == team) NextGoalCyan else NextGoalMuted, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
        item {
            SectionTitle("SEGURIDAD DE LA CUENTA")
            Spacer(Modifier.height(8.dp))
            PreferenceRow(
                icon = Icons.Outlined.Edit,
                title = "Cambiar correo",
                subtitle = profile.email,
                enabled = true,
                onClick = { showEmailDialog = true }
            )
            Spacer(Modifier.height(8.dp))
            PreferenceRow(
                icon = Icons.Outlined.Person,
                title = "Cambiar teléfono",
                subtitle = profile.phoneNumber.ifBlank { "Sin teléfono vinculado" },
                enabled = true,
                onClick = { showPhoneDialog = true }
            )
            Spacer(Modifier.height(8.dp))
            PreferenceRow(
                icon = Icons.Outlined.MoreVert,
                title = "Cambiar contraseña",
                subtitle = "Se solicitará un código de verificación",
                enabled = true,
                onClick = { showPasswordDialog = true }
            )
        }
        item {
            SectionTitle("PREFERENCIAS")
            Spacer(Modifier.height(8.dp))
            PreferenceRow(
                icon = if (profile.notificationsEnabled) Icons.Outlined.NotificationsActive else Icons.Outlined.NotificationsOff,
                title = "Alertas de partidos",
                subtitle = if (profile.notificationsEnabled) "Recibirás avisos de tus equipos" else "Alertas desactivadas",
                enabled = profile.notificationsEnabled,
                onClick = onToggleNotifications
            )
        }
        item {
            OutlinedButton(onClick = onLogout, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(10.dp)) {
                Text("Cerrar sesión", color = NextGoalRed, fontWeight = FontWeight.Bold)
            }
        }
    }

    if (showNameDialog) {
        EditNameDialog(
            currentName = profile.name,
            onDismiss = { showNameDialog = false },
            onSave = {
                onUpdateName(it)
                showNameDialog = false
            }
        )
    }
    if (showEmailDialog) {
        ChangeAccountValueDialog(
            title = "Cambiar correo",
            label = "Nuevo correo",
            currentValue = profile.email,
            kind = VerificationKind.EMAIL,
            onDismiss = { showEmailDialog = false },
            onRequestVerification = { onRequestVerification(VerificationKind.EMAIL, it); showEmailDialog = false }
        )
    }
    if (showPhoneDialog) {
        ChangeAccountValueDialog(
            title = "Cambiar teléfono",
            label = "Nuevo teléfono",
            currentValue = profile.phoneNumber,
            kind = VerificationKind.PHONE,
            onDismiss = { showPhoneDialog = false },
            onRequestVerification = { onRequestVerification(VerificationKind.PHONE, it); showPhoneDialog = false }
        )
    }
    if (showPasswordDialog) {
        ChangePasswordDialog(
            onDismiss = { showPasswordDialog = false },
            onRequestVerification = { onRequestVerification(VerificationKind.PASSWORD, it); showPasswordDialog = false }
        )
    }
}

@Composable
private fun ProfilePhoto(profile: UserProfile, onChange: () -> Unit) {
    Box(
        modifier = Modifier
            .size(84.dp)
            .border(BorderStroke(2.dp, NextGoalCyan), RoundedCornerShape(20.dp))
            .clip(RoundedCornerShape(20.dp))
            .clickable(onClick = onChange),
        contentAlignment = Alignment.Center
    ) {
        if (profile.photoUri.isNullOrBlank()) {
            Text(
                profile.name.split(" ").take(2).mapNotNull { it.firstOrNull() }.joinToString(""),
                color = NextGoalCyan,
                fontSize = 25.sp,
                fontWeight = FontWeight.ExtraBold
            )
        } else {
            AsyncImage(
                model = profile.photoUri,
                contentDescription = "Foto de perfil",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
                error = painterResource(R.drawable.team_placeholder)
            )
        }
    }
}

@Composable
private fun PreferenceRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    enabled: Boolean,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        color = NextGoalSurface,
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, NextGoalLine)
    ) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(38.dp).background(if (enabled) NextGoalCyan.copy(alpha = 0.14f) else NextGoalSurfaceRaised, CircleShape), contentAlignment = Alignment.Center) {
                Icon(icon, contentDescription = null, tint = if (enabled) NextGoalCyan else NextGoalMuted, modifier = Modifier.size(20.dp))
            }
            Spacer(Modifier.width(11.dp))
            Column(Modifier.weight(1f)) {
                Text(title, color = NextGoalText, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Text(subtitle, color = NextGoalMuted, fontSize = 11.sp)
            }
            Box(Modifier.size(10.dp).background(if (enabled) NextGoalCyan else NextGoalMuted, CircleShape))
        }
    }
}

@Composable
private fun ProfileMetric(label: String, title: String, modifier: Modifier = Modifier) {
    Surface(modifier = modifier, color = NextGoalSurface, shape = RoundedCornerShape(13.dp), border = BorderStroke(1.dp, NextGoalLine)) {
        Column(Modifier.padding(vertical = 14.dp, horizontal = 6.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(label, color = NextGoalCyan, fontSize = 22.sp, fontWeight = FontWeight.ExtraBold)
            Spacer(Modifier.height(3.dp))
            Text(title, color = NextGoalMuted, fontSize = 10.sp, textAlign = TextAlign.Center, maxLines = 2)
        }
    }
}

@Composable
private fun EditNameDialog(
    currentName: String,
    onDismiss: () -> Unit,
    onSave: (String) -> Unit
) {
    var name by remember(currentName) { mutableStateOf(currentName) }
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = NextGoalSurface,
        title = { Text("Editar nombre", color = NextGoalText) },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Nombre") },
                singleLine = true
            )
        },
        confirmButton = {
            TextButton(onClick = { onSave(name) }) { Text("Guardar", color = NextGoalCyan, fontWeight = FontWeight.Bold) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar", color = NextGoalMuted) }
        }
    )
}

@Composable
private fun ChangeAccountValueDialog(
    title: String,
    label: String,
    currentValue: String,
    kind: VerificationKind,
    onDismiss: () -> Unit,
    onRequestVerification: (String) -> Unit
) {
    var value by remember(currentValue) { mutableStateOf(currentValue) }
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = NextGoalSurface,
        title = { Text(title, color = NextGoalText) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Se solicitará un código antes de guardar el cambio.", color = NextGoalMuted, fontSize = 12.sp)
                OutlinedTextField(
                    value = value,
                    onValueChange = { value = it },
                    label = { Text(label) },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = if (kind == VerificationKind.PHONE) KeyboardType.Phone else KeyboardType.Email
                    ),
                    singleLine = true
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onRequestVerification(value) }) {
                Text("Continuar", color = NextGoalCyan, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar", color = NextGoalMuted) }
        }
    )
}

@Composable
private fun ChangePasswordDialog(
    onDismiss: () -> Unit,
    onRequestVerification: (String) -> Unit
) {
    var password by remember { mutableStateOf("") }
    var confirmation by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = NextGoalSurface,
        title = { Text("Cambiar contraseña", color = NextGoalText) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("El código se enviará a tu canal vinculado.", color = NextGoalMuted, fontSize = 12.sp)
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it; error = null },
                    label = { Text("Nueva contraseña") },
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    singleLine = true
                )
                OutlinedTextField(
                    value = confirmation,
                    onValueChange = { confirmation = it; error = null },
                    label = { Text("Repite la contraseña") },
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    singleLine = true
                )
                error?.let { Text(it, color = NextGoalRed, fontSize = 12.sp) }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                when {
                    password.length < 6 -> error = "Usa al menos 6 caracteres."
                    password != confirmation -> error = "Las contraseñas no coinciden."
                    else -> onRequestVerification(password)
                }
            }) {
                Text("Enviar código", color = NextGoalCyan, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar", color = NextGoalMuted) }
        }
    )
}

@Composable
private fun VerificationDialog(
    request: VerificationRequest,
    errorMessage: String?,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var code by remember(request.code) { mutableStateOf("") }
    val destination = maskDestination(request.destination)
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = NextGoalSurface,
        title = { Text("Verificación requerida", color = NextGoalText) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Confirma el cambio con el código enviado a $destination.", color = NextGoalMuted)
                if (BuildConfig.DEBUG) {
                    Text("Modo local de Android Studio: código ${request.code}", color = NextGoalCyan, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
                OutlinedTextField(
                    value = code,
                    onValueChange = { code = it.take(6) },
                    label = { Text("Código de 6 dígitos") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true
                )
                errorMessage?.let { Text(it, color = NextGoalRed, fontSize = 12.sp) }
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(code) }) {
                Text("Verificar", color = NextGoalCyan, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar", color = NextGoalMuted) }
        }
    )
}

private fun maskDestination(destination: String): String {
    return if (destination.contains("@")) {
        val parts = destination.split("@", limit = 2)
        "${parts.firstOrNull()?.take(1).orEmpty()}***@${parts.getOrElse(1) { "" }}"
    } else if (destination.length > 4) {
        "****${destination.takeLast(4)}"
    } else {
        destination
    }
}

@Composable
private fun MatchDetailScreen(match: Match, onBack: () -> Unit) {
    var selectedTab by rememberSaveable(match.id) { mutableStateOf("ESTADÍSTICAS") }
    val detailTabs = listOf("ESTADÍSTICAS", "ALINEACIONES", "CRONOLOGÍA")
    val context = LocalContext.current

    Column(Modifier.fillMaxSize().background(NextGoalBackground).statusBarsPadding()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            IconButton(onClick = onBack) { Icon(Icons.Outlined.ArrowBack, contentDescription = "Volver", tint = NextGoalText) }
            Text("DETALLE DEL PARTIDO", style = MaterialTheme.typography.titleMedium)
            IconButton(onClick = { Toast.makeText(context, "Partido preparado para compartir", Toast.LENGTH_SHORT).show() }) {
                Icon(Icons.Outlined.Share, contentDescription = "Compartir", tint = NextGoalText)
            }
        }
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(competitionLabel(match), color = NextGoalCyan, fontSize = 12.sp, fontWeight = FontWeight.ExtraBold)
                    Spacer(Modifier.height(16.dp))
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceEvenly) {
                        TeamColumn(match.home, Modifier.weight(1f))
                        ScoreColumn(match.homeScore, match.awayScore, match.phase == MatchPhase.LIVE)
                        TeamColumn(match.away, Modifier.weight(1f))
                    }
                    Spacer(Modifier.height(10.dp))
                    if (match.phase == MatchPhase.LIVE) {
                        Surface(color = NextGoalCyan.copy(alpha = 0.12f), shape = RoundedCornerShape(4.dp), border = BorderStroke(1.dp, NextGoalCyan)) {
                            Text("${match.minute ?: 0}' EN VIVO", color = NextGoalCyan, fontSize = 10.sp, fontWeight = FontWeight.ExtraBold, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp))
                        }
                    } else {
                        Text(match.kickoffLabel, color = NextGoalMuted, fontSize = 12.sp)
                    }
                }
            }
            item {
                Row(Modifier.fillMaxWidth().border(BorderStroke(1.dp, NextGoalLine)).background(NextGoalSurface).padding(horizontal = 10.dp), horizontalArrangement = Arrangement.SpaceEvenly) {
                    detailTabs.forEach { tab ->
                        Column(
                            modifier = Modifier.clickable { selectedTab = tab }.padding(horizontal = 9.dp, vertical = 12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(tab, color = if (tab == selectedTab) NextGoalCyan else NextGoalMuted, fontSize = 11.sp, fontWeight = FontWeight.ExtraBold)
                            Spacer(Modifier.height(8.dp))
                            Box(Modifier.width(if (tab == selectedTab) 32.dp else 0.dp).height(2.dp).background(NextGoalCyan))
                        }
                    }
                }
            }
            when (selectedTab) {
                "ESTADÍSTICAS" -> {
                    item { MatchPitchAnalysis(match) }
                    item { MatchStats(match) }
                    item { KeyEvents(match) }
                }
                "ALINEACIONES" -> item { Lineups(match) }
                "CRONOLOGÍA" -> item { MatchTimeline(match) }
            }
        }
    }
}

private val PitchGreen = Color(0xFF315F16)

@Composable
private fun MatchPitchAnalysis(match: Match) {
    val stats = match.stats
    Surface(
        modifier = Modifier.padding(horizontal = 18.dp),
        color = NextGoalSurface,
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, NextGoalLine)
    ) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("ANÁLISIS EN CANCHA", style = MaterialTheme.typography.titleMedium)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1.58f)
                    .clip(RoundedCornerShape(10.dp)),
                contentAlignment = Alignment.Center
            ) {
                Canvas(Modifier.fillMaxSize()) {
                    val margin = size.minDimension * 0.055f
                    val fieldWidth = size.width - (margin * 2)
                    val fieldHeight = size.height - (margin * 2)
                    val left = margin
                    val top = margin
                    val right = left + fieldWidth
                    val bottom = top + fieldHeight
                    val centerX = left + fieldWidth / 2f
                    val centerY = top + fieldHeight / 2f
                    val lineWidth = 2.dp.toPx()
                    val lineColor = Color.White.copy(alpha = 0.92f)

                    drawRect(PitchGreen, size = size)
                    drawRect(
                        color = lineColor,
                        topLeft = Offset(left, top),
                        size = Size(fieldWidth, fieldHeight),
                        style = Stroke(lineWidth)
                    )
                    drawLine(lineColor, Offset(centerX, top), Offset(centerX, bottom), lineWidth)
                    drawCircle(lineColor, fieldWidth * 0.14f, Offset(centerX, centerY), style = Stroke(lineWidth))
                    drawCircle(lineColor, lineWidth * 1.2f, Offset(centerX, centerY))
                    drawRect(lineColor, Offset(left, top + fieldHeight * 0.25f), Size(fieldWidth * 0.17f, fieldHeight * 0.5f), style = Stroke(lineWidth))
                    drawRect(lineColor, Offset(left, top + fieldHeight * 0.38f), Size(fieldWidth * 0.07f, fieldHeight * 0.24f), style = Stroke(lineWidth))
                    drawRect(lineColor, Offset(right - fieldWidth * 0.17f, top + fieldHeight * 0.25f), Size(fieldWidth * 0.17f, fieldHeight * 0.5f), style = Stroke(lineWidth))
                    drawRect(lineColor, Offset(right - fieldWidth * 0.07f, top + fieldHeight * 0.38f), Size(fieldWidth * 0.07f, fieldHeight * 0.24f), style = Stroke(lineWidth))
                }
                Column(
                    modifier = Modifier.fillMaxSize().padding(10.dp),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        PitchTeamLabel(match.home)
                        PitchTeamLabel(match.away)
                    }
                    Surface(color = Color.Black.copy(alpha = 0.62f), shape = RoundedCornerShape(8.dp)) {
                        Column(Modifier.padding(horizontal = 10.dp, vertical = 7.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                if (stats == null) "DATOS NO PUBLICADOS" else "POSESIÓN",
                                color = Color.White,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.ExtraBold
                            )
                            Text(
                                "${stats?.homePossession?.let { "$it%" } ?: "-"}   -   ${stats?.awayPossession?.let { "$it%" } ?: "-"}",
                                color = Color.White,
                                fontSize = 17.sp,
                                fontWeight = FontWeight.ExtraBold
                            )
                        }
                    }
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        PitchStatLabel("Remates", stats?.homeShots, Alignment.Start)
                        PitchStatLabel("A puerta", stats?.homeShotsOnTarget, Alignment.End)
                        PitchStatLabel("Remates", stats?.awayShots, Alignment.End)
                    }
                }
            }
            Text(
                if (stats == null) "La fuente todavía no ha publicado datos de análisis para este partido."
                else "Remates ${stats.homeShots ?: "-"} - ${stats.awayShots ?: "-"} · A puerta ${stats.homeShotsOnTarget ?: "-"} - ${stats.awayShotsOnTarget ?: "-"}",
                color = NextGoalMuted,
                fontSize = 11.sp,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun PitchTeamLabel(team: Team) {
    Column(
        modifier = Modifier.width(86.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        TeamCrest(team, 26.dp)
        Text(
            team.shortName,
            color = Color.White,
            fontSize = 10.sp,
            fontWeight = FontWeight.ExtraBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun PitchStatLabel(label: String, value: Int?, alignment: Alignment.Horizontal) {
    Column(horizontalAlignment = alignment) {
        Text(value?.toString() ?: "-", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.ExtraBold)
        Text(label, color = Color.White.copy(alpha = 0.86f), fontSize = 9.sp)
    }
}

@Composable
private fun MatchStats(match: Match) {
    val stats = match.stats
    Column(Modifier.padding(horizontal = 18.dp), verticalArrangement = Arrangement.spacedBy(15.dp)) {
        if (stats == null) {
            EmptyResults("Esta fuente no ha publicado estadísticas avanzadas para este partido.")
        } else {
            val available = listOf(
                stats.homePossession to stats.awayPossession,
                stats.homeShotsOnTarget to stats.awayShotsOnTarget,
                stats.homeShots to stats.awayShots,
                stats.homeCorners to stats.awayCorners,
                stats.homeFouls to stats.awayFouls
            ).count { (home, away) -> home != null && away != null }
            if (available == 0) {
                EmptyResults("Esta fuente no ha publicado estadísticas avanzadas para este partido.")
            } else {
                MatchStatRow("POSESIÓN", stats.homePossession, stats.awayPossession, "%")
                MatchStatRow("REMATES AL ARCO", stats.homeShotsOnTarget, stats.awayShotsOnTarget)
                MatchStatRow("TOTAL REMATES", stats.homeShots, stats.awayShots)
                MatchStatRow("TIROS DE ESQUINA", stats.homeCorners, stats.awayCorners)
                MatchStatRow("FALTAS COMETIDAS", stats.homeFouls, stats.awayFouls)
            }
        }
    }
}

@Composable
private fun MatchStatRow(label: String, home: Int?, away: Int?, suffix: String = "") {
    if (home == null || away == null) return
    val total = (home + away).coerceAtLeast(1)
    StatBar(label, "$home$suffix", "$away$suffix", (home.toFloat() / total).coerceIn(0.02f, 0.98f))
}

@Composable
private fun StatBar(label: String, left: String, right: String, progress: Float) {
    Column {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text(left, color = NextGoalText, fontSize = 13.sp, fontWeight = FontWeight.Bold)
            Text(label, color = NextGoalMuted, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            Text(right, color = NextGoalText, fontSize = 13.sp, fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.height(6.dp))
        Row(Modifier.fillMaxWidth().height(6.dp), horizontalArrangement = Arrangement.spacedBy(5.dp)) {
            Box(Modifier.weight(progress).height(6.dp).clip(RoundedCornerShape(5.dp)).background(NextGoalCyan))
            Box(Modifier.weight(1f - progress).height(6.dp).clip(RoundedCornerShape(5.dp)).background(NextGoalMuted.copy(alpha = 0.45f)))
        }
    }
}

@Composable
private fun KeyEvents(match: Match) {
    Column(Modifier.padding(horizontal = 18.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) {
        Text("SUCESOS CLAVE", style = MaterialTheme.typography.titleMedium)
        if (match.events.isEmpty()) {
            EmptyResults("Todavía no hay sucesos publicados para este partido.")
        } else {
            match.events.forEach { event ->
                EventCard(
                    time = event.minute?.let { "$it'" } ?: "--",
                    title = event.title,
                    subtitle = event.subtitle,
                    tint = eventTint(event.kind)
                )
            }
        }
    }
}

private fun eventTint(kind: String): Color = when (kind) {
    "GOAL" -> NextGoalCyan
    "RED_CARD", "CARD_RED" -> NextGoalRed
    "CARD", "YELLOW_CARD" -> NextGoalYellow
    else -> NextGoalMuted
}

@Composable
private fun EventCard(time: String, title: String, subtitle: String, tint: Color) {
    Surface(color = NextGoalSurface, shape = RoundedCornerShape(12.dp), border = BorderStroke(1.dp, NextGoalLine)) {
        Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(time, color = tint, fontWeight = FontWeight.ExtraBold, fontSize = 13.sp, modifier = Modifier.width(38.dp))
            Box(Modifier.size(22.dp).background(tint.copy(alpha = 0.16f), RoundedCornerShape(6.dp)), contentAlignment = Alignment.Center) {
                Box(Modifier.size(9.dp).background(tint, CircleShape))
            }
            Spacer(Modifier.width(10.dp))
            Column {
                Text(title, color = NextGoalText, fontSize = 13.sp)
                Text(subtitle, color = NextGoalMuted, fontSize = 11.sp)
            }
        }
    }
}

@Composable
private fun Lineups(match: Match) {
    Column(Modifier.padding(horizontal = 18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text("ALINEACIONES", style = MaterialTheme.typography.titleMedium)
        EmptyResults("Las alineaciones oficiales todavía no están disponibles para este partido.")
    }
}

@Composable
private fun MatchTimeline(match: Match) {
    Column(Modifier.padding(horizontal = 18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("CRONOLOGÍA DEL PARTIDO", style = MaterialTheme.typography.titleMedium)
        if (match.events.isEmpty() && match.phase != MatchPhase.LIVE) {
            EmptyResults("Todavía no hay eventos publicados para este partido.")
        } else {
            match.events.forEach { event ->
                TimelineItem(
                    time = event.minute?.let { "$it'" } ?: "--",
                    title = event.title,
                    subtitle = event.subtitle,
                    tint = eventTint(event.kind)
                )
            }
            if (match.phase == MatchPhase.LIVE) {
                TimelineItem("${match.minute ?: 0}'", "Partido en juego", "Actualización en directo", NextGoalRed)
            }
        }
    }
}

@Composable
private fun TimelineItem(time: String, title: String, subtitle: String, tint: Color) {
    Row(Modifier.fillMaxWidth().padding(vertical = 5.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(time, color = tint, fontWeight = FontWeight.ExtraBold, fontSize = 12.sp, modifier = Modifier.width(42.dp))
        Box(Modifier.size(11.dp).background(tint, CircleShape))
        Spacer(Modifier.width(12.dp))
        Column {
            Text(title, fontWeight = FontWeight.Bold, fontSize = 13.sp)
            Text(subtitle, color = NextGoalMuted, fontSize = 11.sp)
        }
    }
}

@Composable
private fun EmptyResults(message: String) {
    Surface(color = NextGoalSurface, shape = RoundedCornerShape(15.dp), border = BorderStroke(1.dp, NextGoalLine)) {
        Column(Modifier.fillMaxWidth().padding(30.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(Icons.Outlined.SportsSoccer, contentDescription = null, tint = NextGoalCyan, modifier = Modifier.size(32.dp))
            Spacer(Modifier.height(10.dp))
            Text(message, color = NextGoalMuted, textAlign = TextAlign.Center)
        }
    }
}
