package com.talkie.app.data

import android.content.Context
import android.content.SharedPreferences
import com.talkie.app.domain.Contact
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * LocalSessionPreferences
 *
 * Provides persistent storage for device identity, session metadata, and the
 * full contacts list (serialised as JSON via kotlinx.serialization).
 */
class LocalSessionPreferences(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    // ── Authentication ───────────────────────────────────────────────────────

    var isLoggedIn: Boolean
        get() = prefs.getBoolean(KEY_IS_LOGGED_IN, false)
        set(value) = prefs.edit().putBoolean(KEY_IS_LOGGED_IN, value).apply()

    var loggedInUsername: String?
        get() = prefs.getString(KEY_LOGGED_IN_USERNAME, null)
        set(value) = prefs.edit().putString(KEY_LOGGED_IN_USERNAME, value).apply()

    var loggedInRole: String?
        get() = prefs.getString(KEY_LOGGED_IN_ROLE, null)
        set(value) = prefs.edit().putString(KEY_LOGGED_IN_ROLE, value).apply()

    // ── Device identity ──────────────────────────────────────────────────────

    var deviceName: String
        get() = prefs.getString(KEY_DEVICE_NAME, "Talkie User") ?: "Talkie User"
        set(value) = prefs.edit().putString(KEY_DEVICE_NAME, value).apply()

    var devicePhotoUri: String?
        get() = prefs.getString(KEY_DEVICE_PHOTO, null)
        set(value) = prefs.edit().putString(KEY_DEVICE_PHOTO, value).apply()

    var deviceBio: String
        get() = prefs.getString(KEY_DEVICE_BIO, "\"Truth is not just seen or heard—it is felt in the silence between questions.\"") ?: ""
        set(value) = prefs.edit().putString(KEY_DEVICE_BIO, value).apply()

    var lastPairedDeviceId: String?
        get() = prefs.getString(KEY_LAST_PAIRED_ID, null)
        set(value) = prefs.edit().putString(KEY_LAST_PAIRED_ID, value).apply()

    // ── Contact list ─────────────────────────────────────────────────────────

    fun loadContacts(): List<Contact> {
        val json = prefs.getString(KEY_CONTACTS, null) ?: return emptyList()
        return try {
            Json.decodeFromString(json)
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun saveContacts(contacts: List<Contact>) {
        prefs.edit().putString(KEY_CONTACTS, Json.encodeToString(contacts)).apply()
    }

    // ── Session helpers ───────────────────────────────────────────────────────

    fun clearSession() {
        prefs.edit()
            .remove(KEY_LAST_PAIRED_ID)
            .remove(KEY_IS_LOGGED_IN)
            .remove(KEY_LOGGED_IN_USERNAME)
            .remove(KEY_LOGGED_IN_ROLE)
            .apply()
    }

    companion object {
        private const val PREFS_NAME       = "talkie_prefs"
        private const val KEY_DEVICE_NAME  = "device_name"
        private const val KEY_DEVICE_PHOTO = "device_photo_uri"
        private const val KEY_DEVICE_BIO   = "device_bio"
        private const val KEY_LAST_PAIRED_ID = "last_paired_id"
        private const val KEY_CONTACTS     = "contacts_json"
        private const val KEY_IS_LOGGED_IN = "is_logged_in"
        private const val KEY_LOGGED_IN_USERNAME = "logged_in_username"
        private const val KEY_LOGGED_IN_ROLE = "logged_in_role"
    }
}
