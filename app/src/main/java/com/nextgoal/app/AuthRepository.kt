package com.nextgoal.app

import android.content.Context
import java.security.MessageDigest

/** Local account store for the Android Studio prototype. A production build should replace this with a backend. */
class AuthRepository(context: Context) {
    private val preferences = context.getSharedPreferences("nextgoal_account", Context.MODE_PRIVATE)

    fun hasAccount(): Boolean = preferences.getBoolean(KEY_ACCOUNT_CREATED, false)

    fun profile(): UserProfile? {
        if (!hasAccount()) return null
        return UserProfile(
            name = preferences.getString(KEY_NAME, "Aficionado NextGoal") ?: "Aficionado NextGoal",
            email = preferences.getString(KEY_EMAIL, "") ?: "",
            favoriteTeam = preferences.getString(KEY_FAVORITE_TEAM, "Real Madrid") ?: "Real Madrid",
            notificationsEnabled = preferences.getBoolean(KEY_NOTIFICATIONS, true),
            phoneNumber = preferences.getString(KEY_PHONE, "") ?: "",
            photoUri = preferences.getString(KEY_PHOTO_URI, null)
        )
    }

    fun register(name: String, email: String, phone: String, password: String): String? {
        if (hasAccount()) return "Ya existe una cuenta local. Inicia sesión para continuar."
        if (name.trim().length < 2) return "Escribe tu nombre completo."
        if (!email.trim().contains("@")) return "Escribe un correo válido."
        if (password.length < 6) return "La contraseña debe tener al menos 6 caracteres."

        preferences.edit()
            .putBoolean(KEY_ACCOUNT_CREATED, true)
            .putString(KEY_NAME, name.trim())
            .putString(KEY_EMAIL, email.trim().lowercase())
            .putString(KEY_PHONE, phone.trim())
            .putString(KEY_PASSWORD_HASH, hash(password))
            .putString(KEY_FAVORITE_TEAM, "Real Madrid")
            .putBoolean(KEY_NOTIFICATIONS, true)
            .apply()
        return null
    }

    fun login(identifier: String, password: String): String? {
        val cleanIdentifier = identifier.trim().lowercase()
        val email = preferences.getString(KEY_EMAIL, "")?.lowercase().orEmpty()
        val phone = preferences.getString(KEY_PHONE, "").orEmpty()
        val storedHash = preferences.getString(KEY_PASSWORD_HASH, "").orEmpty()
        if (cleanIdentifier != email && identifier.trim() != phone) return "El correo o teléfono no coincide."
        if (hash(password) != storedHash) return "La contraseña no es correcta."
        return null
    }

    fun updateName(value: String) {
        preferences.edit().putString(KEY_NAME, value.trim()).apply()
    }

    fun updatePhoto(uri: String?) {
        preferences.edit().putString(KEY_PHOTO_URI, uri).apply()
    }

    fun updateFavoriteTeam(value: String) {
        preferences.edit().putString(KEY_FAVORITE_TEAM, value).apply()
    }

    fun updateNotifications(value: Boolean) {
        preferences.edit().putBoolean(KEY_NOTIFICATIONS, value).apply()
    }

    fun updateEmail(value: String) {
        preferences.edit().putString(KEY_EMAIL, value.trim().lowercase()).apply()
    }

    fun updatePhone(value: String) {
        preferences.edit().putString(KEY_PHONE, value.trim()).apply()
    }

    fun updatePassword(value: String) {
        preferences.edit().putString(KEY_PASSWORD_HASH, hash(value)).apply()
    }

    fun clearSession() {
        preferences.edit().putBoolean(KEY_SESSION_ACTIVE, false).apply()
    }

    fun setSessionActive(active: Boolean) {
        preferences.edit().putBoolean(KEY_SESSION_ACTIVE, active).apply()
    }

    fun isSessionActive(): Boolean = preferences.getBoolean(KEY_SESSION_ACTIVE, false)

    private fun hash(value: String): String {
        val digest = MessageDigest.getInstance("SHA-256").digest(value.toByteArray())
        return digest.joinToString("") { byte -> "%02x".format(byte) }
    }

    private companion object {
        const val KEY_ACCOUNT_CREATED = "accountCreated"
        const val KEY_SESSION_ACTIVE = "sessionActive"
        const val KEY_NAME = "name"
        const val KEY_EMAIL = "email"
        const val KEY_PHONE = "phone"
        const val KEY_PASSWORD_HASH = "passwordHash"
        const val KEY_FAVORITE_TEAM = "favoriteTeam"
        const val KEY_NOTIFICATIONS = "notifications"
        const val KEY_PHOTO_URI = "photoUri"
    }
}
