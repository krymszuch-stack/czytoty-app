package com.example.data

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class LocalStoreTest {
    private lateinit var store: LocalStore
    @Before fun prepare() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        store = LocalStore.get(context)
        store.notes().forEach { store.delete(it.phone) }
        store.clearEvents()
    }
    @Test fun startsEmptyAndPersistsAcrossReopening() {
        assertTrue(store.notes().isEmpty())
        val note = PhoneNote("+12025550123", "Test", "Prywatna notatka", true)
        store.save(note)
        store.close()
        assertEquals(note, store.notes().single())
        assertTrue(store.isBlocked(note.phone))
        store.save(note.copy(blocked = false))
        assertFalse(store.isBlocked(note.phone))
        store.delete(note.phone)
        assertTrue(store.notes().isEmpty())
    }
    @Test fun noGlobalReputationOrBlockingForUnknownNumber() {
        assertFalse(store.isBlocked("+12025550124"))
        store.record("+12025550124", false)
        assertFalse(store.events().single().blocked)
    }
    @Test fun eventHistoryIsBoundedAndClearable() {
        repeat(210) { store.record("+12025550123", it % 2 == 0) }
        assertEquals(200, store.events().size)
        store.clearEvents()
        assertTrue(store.events().isEmpty())
    }
}
