package com.example.data.local

import android.content.Context
import android.content.SharedPreferences

class TokenStorage(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    companion object {
        private const val PREFS_NAME = "gh_compiler_secure_prefs"
        private const val KEY_PAT = "github_pat_token"
        private const val KEY_USERNAME = "github_username"
        private const val KEY_USER_NAME = "github_name"
        private const val KEY_AVATAR_URL = "github_avatar_url"
    }

    fun saveToken(token: String) {
        prefs.edit().putString(KEY_PAT, token.trim()).apply()
    }

    fun getToken(): String? {
        return prefs.getString(KEY_PAT, null)?.takeIf { it.isNotBlank() }
    }

    fun hasToken(): Boolean {
        return !getToken().isNullOrBlank()
    }

    fun getMaskedToken(): String {
        val token = getToken() ?: return ""
        return if (token.length > 8) {
            val prefix = token.take(4)
            val suffix = token.takeLast(4)
            "$prefix••••••••$suffix"
        } else {
            "••••••••"
        }
    }

    fun saveUserProfile(username: String, name: String?, avatarUrl: String?) {
        prefs.edit()
            .putString(KEY_USERNAME, username)
            .putString(KEY_USER_NAME, name ?: "")
            .putString(KEY_AVATAR_URL, avatarUrl ?: "")
            .apply()
    }

    fun getUsername(): String? = prefs.getString(KEY_USERNAME, null)
    fun getUserName(): String? = prefs.getString(KEY_USER_NAME, null)
    fun getAvatarUrl(): String? = prefs.getString(KEY_AVATAR_URL, null)

    fun clear() {
        prefs.edit().clear().apply()
    }
}
