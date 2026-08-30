package ro.alintudor.oracle.core

import android.content.Context

/** Persistent user-selected Analysis watchlist. Kept separate from Portfolio. */
class OracleWatchlistStore(context: Context) {
    private val prefs = context.getSharedPreferences("oracle_watchlist", Context.MODE_PRIVATE)

    fun load(): List<String> = prefs.getString(KEY, "")
        .orEmpty()
        .split('|')
        .map { it.trim().uppercase() }
        .filter { it.isNotBlank() }
        .distinct()
        .sorted()

    fun contains(ticker: String): Boolean = load().contains(normalize(ticker))

    fun add(ticker: String) {
        val t = normalize(ticker)
        if (t.isBlank()) return
        save(load().toMutableList().apply { if (!contains(t)) add(t) })
    }

    fun remove(ticker: String) {
        val t = normalize(ticker)
        if (t.isBlank()) return
        save(load().filterNot { it == t })
    }

    private fun save(items: List<String>) {
        prefs.edit()
            .putString(KEY, items.map { normalize(it) }.filter { it.isNotBlank() }.distinct().sorted().joinToString("|"))
            .commit()
    }

    private fun normalize(ticker: String): String = ticker.trim().uppercase()

    companion object { private const val KEY = "selected_tickers_v2" }
}
