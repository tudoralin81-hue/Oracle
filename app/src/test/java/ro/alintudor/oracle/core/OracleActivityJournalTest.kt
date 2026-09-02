package ro.alintudor.oracle.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class OracleActivityJournalTest {
    @Test
    fun identicalJournalEntriesAreDeduplicated() {
        val action = OracleAction("NVDA", "BUY", 82.5, "Strong trend", 1000L)
        val result = OracleActivityJournal.merge(
            OracleActivityJournal.fromActions(listOf(action)),
            listOf(action)
        )
        assertEquals(1, result.size)
        assertEquals("NVDA", result.single().ticker)
        assertEquals("BUY", result.single().action)
    }

    @Test
    fun journalIsSortedNewestFirst() {
        val oldAction = OracleAction("AAPL", "HOLD", 50.0, "Neutral", 1000L)
        val newAction = OracleAction("MSFT", "BUY", 80.0, "Trend", 2000L)
        val result = OracleActivityJournal.fromActions(listOf(oldAction, newAction))
        assertEquals(listOf("MSFT", "AAPL"), result.map { it.ticker })
        assertTrue(result.first().timestamp > result.last().timestamp)
    }
}
