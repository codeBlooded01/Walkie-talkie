package com.talkie.app.data

import android.content.Context
import android.content.SharedPreferences
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class LocalSessionPreferencesTest {

    // ── Mocks ─────────────────────────────────────────────────────────────────
    private val mockEditor   : SharedPreferences.Editor = mockk(relaxed = true)
    private val mockPrefs    : SharedPreferences        = mockk()
    private val mockContext  : Context                  = mockk()

    private lateinit var preferences: LocalSessionPreferences

    @Before
    fun setUp() {
        every { mockContext.getSharedPreferences(any(), any()) } returns mockPrefs
        every { mockPrefs.edit() } returns mockEditor
        // Chainable editor stubs
        every { mockEditor.putString(any(), any()) } returns mockEditor
        every { mockEditor.remove(any()) } returns mockEditor

        preferences = LocalSessionPreferences(mockContext)
    }

    // ── deviceName ────────────────────────────────────────────────────────────

    @Test
    fun `deviceName returns default when no value stored`() {
        every { mockPrefs.getString("device_name", "Talkie User") } returns "Talkie User"
        assertEquals("Talkie User", preferences.deviceName)
    }

    @Test
    fun `deviceName returns stored value when one exists`() {
        every { mockPrefs.getString("device_name", "Talkie User") } returns "Darlene"
        assertEquals("Darlene", preferences.deviceName)
    }

    @Test
    fun `setting deviceName writes to SharedPreferences and applies`() {
        preferences.deviceName = "Nicole"
        verify { mockEditor.putString("device_name", "Nicole") }
        verify { mockEditor.apply() }
    }

    // ── lastPairedDeviceId ───────────────────────────────────────────────────

    @Test
    fun `lastPairedDeviceId returns null when nothing stored`() {
        every { mockPrefs.getString("last_paired_id", null) } returns null
        assertNull(preferences.lastPairedDeviceId)
    }

    @Test
    fun `lastPairedDeviceId returns stored address`() {
        every { mockPrefs.getString("last_paired_id", null) } returns "aa:bb:cc:dd:ee:ff"
        assertEquals("aa:bb:cc:dd:ee:ff", preferences.lastPairedDeviceId)
    }

    @Test
    fun `setting lastPairedDeviceId writes to SharedPreferences`() {
        preferences.lastPairedDeviceId = "11:22:33:44:55:66"
        verify { mockEditor.putString("last_paired_id", "11:22:33:44:55:66") }
        verify { mockEditor.apply() }
    }

    // ── clearSession ─────────────────────────────────────────────────────────

    @Test
    fun `clearSession removes lastPairedDeviceId but not deviceName`() {
        preferences.clearSession()
        verify { mockEditor.remove("last_paired_id") }
        // deviceName key must NOT be removed
        verify(exactly = 0) { mockEditor.remove("device_name") }
        verify { mockEditor.apply() }
    }
}
