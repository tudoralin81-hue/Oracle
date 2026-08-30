package ro.alintudor.oracle.core

import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

/** Small market-data adapter used by Oracle technical indicators. It reads OHLCV directly and never uses WordPress. */
data class OracleOhlcvPoint(
    val timestamp: Long,
    val open: Double,
    val high: Double,
    val low: Double,
    val close: Double,
    val volume: Double
)

object OracleMarketData {
    private const val CONNECT_TIMEOUT_MS = 8_000
    private const val READ_TIMEOUT_MS = 12_000

    /** Fetches OHLCV at the requested Analysis timeframe. */
    fun fetchForMode(ticker: String, mode: String): List<OracleOhlcvPoint> {
        val symbol = ticker.trim().uppercase()
        if (symbol.isBlank()) return emptyList()
        val (range, interval) = when (mode) {
            "5M" -> "5d" to "5m"
            "30M" -> "5d" to "30m"
            "1H" -> "1mo" to "1h"
            "1D" -> "1y" to "1d"
            "5D" -> "5d" to "1d"
            "1M" -> "1mo" to "1d"
            "3M" -> "3mo" to "1d"
            "1Y" -> "1y" to "1d"
            else -> "1y" to "1d"
        }
        return fetch(symbol, range, interval)
    }

    /** Backward-compatible daily feed used by existing Oracle components. */
    fun fetchDaily(ticker: String, range: String = "6mo"): List<OracleOhlcvPoint> = fetch(ticker, range, "1d")

    private fun fetch(ticker: String, range: String, interval: String): List<OracleOhlcvPoint> {
        val symbol = ticker.trim().uppercase()
        if (symbol.isBlank()) return emptyList()
        val url = URL("https://query1.finance.yahoo.com/v8/finance/chart/$symbol?range=$range&interval=$interval&events=history")
        val connection = (url.openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = CONNECT_TIMEOUT_MS
            readTimeout = READ_TIMEOUT_MS
            setRequestProperty("User-Agent", "Oracle-Stock-Intelligence/1.0")
            setRequestProperty("Accept", "application/json")
        }
        return try {
            if (connection.responseCode !in 200..299) return emptyList()
            val body = connection.inputStream.bufferedReader().use { it.readText() }
            parse(body)
        } catch (_: Exception) {
            emptyList()
        } finally {
            connection.disconnect()
        }
    }

    private fun parse(body: String): List<OracleOhlcvPoint> {
        val root = JSONObject(body)
        val result = root.optJSONObject("chart")?.optJSONArray("result")?.optJSONObject(0) ?: return emptyList()
        val timestamps = result.optJSONArray("timestamp") ?: return emptyList()
        val quote = result.optJSONObject("indicators")?.optJSONArray("quote")?.optJSONObject(0) ?: return emptyList()
        val opens = quote.optJSONArray("open")
        val highs = quote.optJSONArray("high")
        val lows = quote.optJSONArray("low")
        val closes = quote.optJSONArray("close")
        val volumes = quote.optJSONArray("volume")
        if (opens == null || highs == null || lows == null || closes == null) return emptyList()

        val out = ArrayList<OracleOhlcvPoint>(timestamps.length())
        for (i in 0 until timestamps.length()) {
            val open = opens.optDouble(i, Double.NaN)
            val high = highs.optDouble(i, Double.NaN)
            val low = lows.optDouble(i, Double.NaN)
            val close = closes.optDouble(i, Double.NaN)
            val volume = volumes?.optDouble(i, 0.0) ?: 0.0
            if (!open.isFinite() || !high.isFinite() || !low.isFinite() || !close.isFinite()) continue
            if (high <= 0.0 || low <= 0.0 || close <= 0.0) continue
            out += OracleOhlcvPoint(timestamps.optLong(i) * 1000L, open, high, low, close, volume)
        }
        return out.sortedBy { it.timestamp }
    }
}
