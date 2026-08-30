package ro.alintudor.oracle.core

import android.content.Context

/** Persistent user-selected Analysis watchlist. Kept separate from Portfolio. */
class OracleWatchlistStore(context: Context) {
    private val prefs = context.getSharedPreferences("oracle_watchlist", Context.MODE_PRIVATE)

    fun load(): List<String> {
        val current = prefs.getString(KEY, null)
        if (current != null) return decode(current)

        // Migrate the previous StringSet format once, without losing saved tickers.
        val legacy = prefs.getStringSet(LEGACY_KEY, emptySet()).orEmpty()
        val migrated = legacy.map(::normalize).filter { it.isNotBlank() }.distinct().sorted()
        if (migrated.isNotEmpty()) save(migrated)
        return migrated
    }

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

    private fun decode(value: String): List<String> = value
        .split('|')
        .map(::normalize)
        .filter { it.isNotBlank() }
        .distinct()
        .sorted()

    private fun save(items: List<String>) {
        prefs.edit()
            .putString(KEY, items.map(::normalize).filter { it.isNotBlank() }.distinct().sorted().joinToString("|"))
            .commit()
    }

    private fun normalize(ticker: String): String = ticker.trim().uppercase()

    companion object {
        private const val KEY = "selected_tickers_v2"
        private const val LEGACY_KEY = "selected_tickers"
    }
}
