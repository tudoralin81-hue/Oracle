package ro.alintudor.oracle.core

import android.content.Context

/** Persistent user-selected Analysis watchlist. Kept separate from Portfolio. */
class OracleWatchlistStore(context: Context) {
    private val prefs = context.getSharedPreferences("oracle_watchlist", Context.MODE_PRIVATE)

    fun load(): List<String> = prefs.getStringSet(KEY, emptySet())
        ?.map { it.trim().uppercase() }
        ?.filter { it.isNotBlank() }
        ?.distinct()
        ?.sorted()
        ?: emptyList()

    fun contains(ticker: String): Boolean = load().contains(ticker.trim().uppercase())

    fun add(ticker: String) {
        val t = ticker.trim().uppercase()
        if (t.isBlank()) return
        val next = load().toMutableSet().apply { add(t) }
        prefs.edit().putStringSet(KEY, next).apply()
    }

    fun remove(ticker: String) {
        val t = ticker.trim().uppercase()
        val next = load().toMutableSet().apply { remove(t) }
        prefs.edit().putStringSet(KEY, next).apply()
    }

    companion object { private const val KEY = "selected_tickers" }
}
