package com.nextgoal.app

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.isActive
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

class NextGoalViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = FootballRepository(BuildConfig.FOOTBALL_DATA_TOKEN)
    private val authRepository = AuthRepository(application)
    private var refreshJob: Job? = null
    private var directorySearchJob: Job? = null
    private var directoryDetailsJob: Job? = null
    private var matchDetailsJob: Job? = null

    private val _dashboard = MutableStateFlow(DashboardState())
    val dashboard: StateFlow<DashboardState> = _dashboard.asStateFlow()

    private val _directory = MutableStateFlow(DirectoryState())
    val directory: StateFlow<DirectoryState> = _directory.asStateFlow()

    private val _profile = MutableStateFlow(authRepository.profile() ?: emptyProfile())
    val profile: StateFlow<UserProfile> = _profile.asStateFlow()

    private val _auth = MutableStateFlow(
        AuthState(isAuthenticated = authRepository.hasAccount() && authRepository.isSessionActive())
    )
    val auth: StateFlow<AuthState> = _auth.asStateFlow()

    init {
        refresh()
        viewModelScope.launch {
            while (isActive) {
                delay(30_000)
                refresh()
            }
        }
    }

    fun refresh() {
        if (refreshJob?.isActive == true) return
        refreshJob = viewModelScope.launch {
            _dashboard.update { it.copy(isRefreshing = true) }
            val result = repository.loadDashboard()
            _dashboard.value = DashboardState(
                data = result.first,
                isRefreshing = false,
                dataSourceLabel = result.second,
                lastUpdatedLabel = "Actualizado ${ZonedDateTime.now(NEXT_GOAL_TIME_ZONE).format(DateTimeFormatter.ofPattern("HH:mm", NEXT_GOAL_LOCALE))}",
                refreshingMatchId = null
            )
        }
    }

    fun refreshMatchDetails(matchId: Int) {
        if (matchDetailsJob?.isActive == true) return
        val currentMatch = _dashboard.value.data.matches.firstOrNull { it.id == matchId } ?: return
        matchDetailsJob = viewModelScope.launch {
            _dashboard.update { it.copy(refreshingMatchId = matchId) }
            val refreshed = repository.loadMatchDetails(currentMatch)
            _dashboard.update { state ->
                val matches = if (refreshed == null) {
                    state.data.matches
                } else {
                    state.data.matches.map { if (it.id == matchId) refreshed else it }
                }
                state.copy(
                    data = state.data.copy(matches = matches),
                    refreshingMatchId = null
                )
            }
        }
    }

    fun searchDirectory(query: String) {
        val cleanQuery = query.trim()
        directorySearchJob?.cancel()
        if (cleanQuery.length < 2) {
            _directory.value = DirectoryState(query = cleanQuery)
            return
        }
        _directory.update {
            it.copy(query = cleanQuery, isSearching = true, errorMessage = null, selected = null)
        }
        directorySearchJob = viewModelScope.launch {
            runCatching { repository.searchDirectory(cleanQuery) }
                .onSuccess { results ->
                    _directory.update {
                        it.copy(
                            query = cleanQuery,
                            isSearching = false,
                            results = results,
                            errorMessage = if (results.isEmpty()) "No encontramos jugadores ni equipos con ese nombre." else null
                        )
                    }
                }
                .onFailure { error ->
                    _directory.update {
                        it.copy(
                            isSearching = false,
                            results = emptyList(),
                            errorMessage = error.message ?: "No se pudo completar la búsqueda."
                        )
                    }
                }
        }
    }

    fun openDirectoryResult(result: DirectoryResult) {
        directoryDetailsJob?.cancel()
        _directory.update { it.copy(selected = null, isLoadingDetails = true, errorMessage = null) }
        directoryDetailsJob = viewModelScope.launch {
            val details = repository.loadDirectoryDetails(result.type, result.id)
            _directory.update {
                it.copy(
                    selected = details,
                    isLoadingDetails = false,
                    errorMessage = if (details == null) "No se pudo cargar la información actual." else null
                )
            }
        }
    }

    fun refreshDirectoryDetails() {
        val current = _directory.value.selected ?: return
        if (directoryDetailsJob?.isActive == true) return
        val typeAndId = when (current) {
            is DirectoryDetails.Player -> DirectoryEntityType.PLAYER to current.id
            is DirectoryDetails.Team -> DirectoryEntityType.TEAM to current.id
        }
        _directory.update { it.copy(isLoadingDetails = true) }
        directoryDetailsJob = viewModelScope.launch {
            val refreshed = repository.loadDirectoryDetails(typeAndId.first, typeAndId.second)
            _directory.update {
                it.copy(
                    selected = refreshed ?: it.selected,
                    isLoadingDetails = false,
                    errorMessage = if (refreshed == null) "No se pudo actualizar el calendario." else null
                )
            }
        }
    }

    fun closeDirectoryDetails() {
        directoryDetailsJob?.cancel()
        _directory.update { it.copy(selected = null, isLoadingDetails = false, errorMessage = null) }
    }

    fun clearDirectory() {
        directorySearchJob?.cancel()
        directoryDetailsJob?.cancel()
        _directory.value = DirectoryState()
    }

    fun updateProfileName(name: String) {
        val cleanName = name.trim().ifBlank { "Aficionado NextGoal" }
        authRepository.updateName(cleanName)
        _profile.update { it.copy(name = cleanName) }
    }

    fun updateProfilePhoto(uri: String?) {
        authRepository.updatePhoto(uri)
        _profile.update { it.copy(photoUri = uri) }
    }

    fun updateFavoriteTeam(teamName: String) {
        authRepository.updateFavoriteTeam(teamName)
        _profile.update { it.copy(favoriteTeam = teamName) }
    }

    fun toggleNotifications() {
        _profile.update { current ->
            val enabled = !current.notificationsEnabled
            authRepository.updateNotifications(enabled)
            current.copy(notificationsEnabled = enabled)
        }
    }

    fun register(name: String, email: String, phone: String, password: String, confirmation: String) {
        if (password != confirmation) {
            setAuthError("Las contraseñas no coinciden.")
            return
        }
        val error = authRepository.register(name, email, phone, password)
        if (error != null) {
            setAuthError(error)
            return
        }
        authRepository.setSessionActive(true)
        _profile.value = authRepository.profile() ?: emptyProfile()
        _auth.value = AuthState(isAuthenticated = true)
    }

    fun login(identifier: String, password: String) {
        val error = authRepository.login(identifier, password)
        if (error != null) {
            setAuthError(error)
            return
        }
        authRepository.setSessionActive(true)
        _profile.value = authRepository.profile() ?: emptyProfile()
        _auth.value = AuthState(isAuthenticated = true)
    }

    fun logout() {
        authRepository.clearSession()
        _auth.value = AuthState(isAuthenticated = false)
    }

    fun clearAuthError() {
        _auth.update { it.copy(errorMessage = null) }
    }

    fun requestVerification(kind: VerificationKind, newValue: String) {
        val value = newValue.trim()
        val current = _profile.value
        val error = when (kind) {
            VerificationKind.EMAIL -> when {
                !value.contains("@") -> "Escribe un correo válido."
                value.equals(current.email, ignoreCase = true) -> "Ese ya es tu correo actual."
                else -> null
            }
            VerificationKind.PHONE -> when {
                value.length < 7 -> "Escribe un número de teléfono válido."
                value == current.phoneNumber -> "Ese ya es tu teléfono actual."
                else -> null
            }
            VerificationKind.PASSWORD -> if (value.length < 6) "La nueva contraseña debe tener al menos 6 caracteres." else null
        }
        if (error != null) {
            _auth.update { it.copy(errorMessage = error) }
            return
        }

        val destination = when (kind) {
            VerificationKind.EMAIL -> current.email
            VerificationKind.PHONE, VerificationKind.PASSWORD -> current.phoneNumber.ifBlank { current.email }
        }
        val request = VerificationRequest(
            kind = kind,
            destination = destination,
            code = (100000..999999).random().toString(),
            newValue = value
        )
        deliverVerificationCode(request)
        _auth.update { it.copy(errorMessage = null, pendingVerification = request) }
    }

    fun confirmVerification(code: String) {
        val request = _auth.value.pendingVerification ?: return
        if (code.trim() != request.code) {
            _auth.update { it.copy(errorMessage = "El código de verificación no es correcto.") }
            return
        }
        when (request.kind) {
            VerificationKind.EMAIL -> authRepository.updateEmail(request.newValue)
            VerificationKind.PHONE -> authRepository.updatePhone(request.newValue)
            VerificationKind.PASSWORD -> authRepository.updatePassword(request.newValue)
        }
        _profile.value = authRepository.profile() ?: _profile.value
        _auth.update { it.copy(errorMessage = null, pendingVerification = null) }
    }

    fun cancelVerification() {
        _auth.update { it.copy(errorMessage = null, pendingVerification = null) }
    }

    private fun setAuthError(message: String) {
        _auth.update { it.copy(isBusy = false, errorMessage = message) }
    }

    private fun deliverVerificationCode(request: VerificationRequest) {
        // This is the local development gateway. Connect Firebase Auth or a backend here for real email/SMS delivery.
    }

    private fun emptyProfile() = UserProfile(
        name = "Aficionado NextGoal",
        email = "",
        favoriteTeam = "Real Madrid",
        notificationsEnabled = true
    )
}
