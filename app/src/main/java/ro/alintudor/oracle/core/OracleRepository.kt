package ro.alintudor.oracle.core

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

/** Native data layer. WordPress is treated as a data source, never as the app UI. */
class OracleRepository(private val context: Context) {
    private val prefs = context.getSharedPreferences("oracle_data", Context.MODE_PRIVATE)

    fun cachedPositions(): List<OraclePosition> = parsePositions(prefs.getString("positions", "[]") ?: "[]")
    fun cachedAlerts(): List<OracleAlert> = parseAlerts(prefs.getString("alerts", "[]") ?: "[]")
    fun cachedHistory(): List<OracleHistoryPoint> = parseHistory(prefs.getString("history", "[]") ?: "[]")

    fun savePositions(items: List<OraclePosition>) = prefs.edit().putString("positions", JSONArray(items.map { it.toJson() }).toString()).apply()
    fun saveAlerts(items: List<OracleAlert>) = prefs.edit().putString("alerts", JSONArray(items.map { it.toJson() }).toString()).apply()
    fun saveHistory(items: List<OracleHistoryPoint>) = prefs.edit().putString("history", JSONArray(items.map { it.toJson() }).toString()).apply()

    fun getJson(url: String, timeoutMs: Int = 12000): String? = runCatching {
        val c = URL(url).openConnection() as HttpURLConnection
        c.connectTimeout = timeoutMs; c.readTimeout = timeoutMs
        c.requestMethod = "GET"
        c.setRequestProperty("Accept", "application/json")
        c.setRequestProperty("User-Agent", "AIStockOracleApp/2.0")
        if (c.responseCode !in 200..299) return null
        c.inputStream.bufferedReader().use { it.readText() }
    }.getOrNull()

    private fun parsePositions(s: String): List<OraclePosition> = runCatching {
        val a = JSONArray(s); List(a.length()) { i -> OraclePosition.fromJson(a.getJSONObject(i)) }
    }.getOrDefault(emptyList())
    private fun parseAlerts(s: String): List<OracleAlert> = runCatching {
        val a = JSONArray(s); List(a.length()) { i -> OracleAlert.fromJson(a.getJSONObject(i)) }
    }.getOrDefault(emptyList())
    private fun parseHistory(s: String): List<OracleHistoryPoint> = runCatching {
        val a = JSONArray(s); List(a.length()) { i -> OracleHistoryPoint.fromJson(a.getJSONObject(i)) }
    }.getOrDefault(emptyList())
}

private fun OraclePosition.toJson() = JSONObject().apply { put("ticker",ticker); put("company",company); put("shares",shares); put("avgCost",avgCost); put("currentPrice",currentPrice); put("currency",currency); put("pnl",pnl); put("pnlPercent",pnlPercent); put("marketValue",marketValue); put("weight",weight); put("status",status) }
private fun OracleAlert.toJson() = JSONObject().apply { put("ticker",ticker); put("level",level); put("title",title); put("message",message); put("timestamp",timestamp); put("active",active) }
private fun OracleHistoryPoint.toJson() = JSONObject().apply { put("ticker",ticker); put("timestamp",timestamp); put("price",price); put("value",value); put("pnl",pnl) }
private fun OraclePosition.Companion.fromJson(o: JSONObject) = OraclePosition(o.optString("ticker"),o.optString("company"),o.optDouble("shares"),o.optDouble("avgCost"),o.optDouble("currentPrice"),o.optString("currency","USD"),o.optDouble("pnl"),o.optDouble("pnlPercent"),o.optDouble("marketValue"),o.optDouble("weight"),o.optString("status","ACTIVE"))
private fun OracleAlert.Companion.fromJson(o: JSONObject) = OracleAlert(o.optString("ticker"),o.optString("level"),o.optString("title"),o.optString("message"),o.optLong("timestamp"),o.optBoolean("active",true))
private fun OracleHistoryPoint.Companion.fromJson(o: JSONObject) = OracleHistoryPoint(o.optString("ticker"),o.optLong("timestamp"),o.optDouble("price"),o.optDouble("value"),o.optDouble("pnl"))
private val OraclePosition.Companion: OraclePosition.Companion get() = OraclePosition
private val OracleAlert.Companion: OracleAlert.Companion get() = OracleAlert
private val OracleHistoryPoint.Companion: OracleHistoryPoint.Companion get() = OracleHistoryPoint
