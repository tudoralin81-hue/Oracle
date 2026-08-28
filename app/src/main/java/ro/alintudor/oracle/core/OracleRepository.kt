package ro.alintudor.oracle.core

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

/** Fully local native data layer. No WordPress/API dependency. */
class OracleRepository(private val context: Context) {
    private val prefs = context.getSharedPreferences("oracle_data", Context.MODE_PRIVATE)
    fun cachedPositions(): List<OraclePosition> = parsePositions(prefs.getString("positions", "[]") ?: "[]")
    fun cachedAlerts(): List<OracleAlert> = parseAlerts(prefs.getString("alerts", "[]") ?: "[]")
    fun cachedNews(): List<OracleNews> = parseNews(prefs.getString("news", "[]") ?: "[]")
    fun cachedHistory(): List<OracleHistoryPoint> = parseHistory(prefs.getString("history", "[]") ?: "[]")
    fun cachedActions(): List<OracleAction> = parseActions(prefs.getString("actions", "[]") ?: "[]")
    fun cachedKnowledge(): List<OracleKnowledgeItem> = parseKnowledge(prefs.getString("knowledge", "[]") ?: "[]")
    fun savePositions(items: List<OraclePosition>) = prefs.edit().putString("positions", JSONArray().apply { items.forEach { put(it.toJson()) } }.toString()).apply()
    fun saveAlerts(items: List<OracleAlert>) = prefs.edit().putString("alerts", JSONArray().apply { items.forEach { put(it.toJson()) } }.toString()).apply()
    fun saveNews(items: List<OracleNews>) = prefs.edit().putString("news", JSONArray().apply { items.forEach { put(it.toJson()) } }.toString()).apply()
    fun saveHistory(items: List<OracleHistoryPoint>) = prefs.edit().putString("history", JSONArray().apply { items.forEach { put(it.toJson()) } }.toString()).apply()
    fun saveActions(items: List<OracleAction>) = prefs.edit().putString("actions", JSONArray().apply { items.forEach { put(it.toJson()) } }.toString()).apply()
    fun saveKnowledge(items: List<OracleKnowledgeItem>) = prefs.edit().putString("knowledge", JSONArray().apply { items.forEach { put(it.toJson()) } }.toString()).apply()

    private fun parsePositions(s: String) = runCatching { val a=JSONArray(s); List(a.length()){i->positionFromJson(a.getJSONObject(i))} }.getOrDefault(emptyList())
    private fun parseAlerts(s: String) = runCatching { val a=JSONArray(s); List(a.length()){i->alertFromJson(a.getJSONObject(i))} }.getOrDefault(emptyList())
    private fun parseNews(s: String) = runCatching { val a=JSONArray(s); List(a.length()){i->newsFromJson(a.getJSONObject(i))} }.getOrDefault(emptyList())
    private fun parseHistory(s: String) = runCatching { val a=JSONArray(s); List(a.length()){i->historyFromJson(a.getJSONObject(i))} }.getOrDefault(emptyList())
    private fun parseActions(s: String) = runCatching { val a=JSONArray(s); List(a.length()){i->actionFromJson(a.getJSONObject(i))} }.getOrDefault(emptyList())
    private fun parseKnowledge(s: String) = runCatching { val a=JSONArray(s); List(a.length()){i->knowledgeFromJson(a.getJSONObject(i))} }.getOrDefault(emptyList())
    private fun positionFromJson(o: JSONObject) = OraclePosition(o.optString("ticker"),o.optString("company"),o.optDouble("shares"),o.optDouble("avgCost"),o.optDouble("currentPrice"),o.optString("currency","USD"),o.optDouble("pnl"),o.optDouble("pnlPercent"),o.optDouble("marketValue"),o.optDouble("weight"),o.optString("status","ACTIVE"))
    private fun alertFromJson(o: JSONObject) = OracleAlert(o.optString("ticker"),o.optString("level"),o.optString("title"),o.optString("message"),o.optLong("timestamp"),o.optBoolean("active",true))
    private fun newsFromJson(o: JSONObject) = OracleNews(o.optString("ticker"),o.optString("title"),o.optString("source"),o.optString("url"),o.optLong("publishedAt"),o.optBoolean("breaking",false))
    private fun historyFromJson(o: JSONObject) = OracleHistoryPoint(o.optString("ticker"),o.optLong("timestamp"),o.optDouble("price"),o.optDouble("value"),o.optDouble("pnl"))
    private fun actionFromJson(o: JSONObject) = OracleAction(o.optString("ticker"),o.optString("action"),o.optDouble("score"),o.optString("reason"),o.optLong("timestamp"))
    private fun knowledgeFromJson(o: JSONObject) = OracleKnowledgeItem(o.optString("title"),o.optString("category"),o.optString("content"),o.optLong("publishedAt"))
}
private fun OraclePosition.toJson() = JSONObject().apply { put("ticker",ticker); put("company",company); put("shares",shares); put("avgCost",avgCost); put("currentPrice",currentPrice); put("currency",currency); put("pnl",pnl); put("pnlPercent",pnlPercent); put("marketValue",marketValue); put("weight",weight); put("status",status) }
private fun OracleAlert.toJson() = JSONObject().apply { put("ticker",ticker); put("level",level); put("title",title); put("message",message); put("timestamp",timestamp); put("active",active) }
private fun OracleNews.toJson() = JSONObject().apply { put("ticker",ticker); put("title",title); put("source",source); put("url",url); put("publishedAt",publishedAt); put("breaking",breaking) }
private fun OracleHistoryPoint.toJson() = JSONObject().apply { put("ticker",ticker); put("timestamp",timestamp); put("price",price); put("value",value); put("pnl",pnl) }
private fun OracleAction.toJson() = JSONObject().apply { put("ticker",ticker); put("action",action); put("score",score); put("reason",reason); put("timestamp",timestamp) }
private fun OracleKnowledgeItem.toJson() = JSONObject().apply { put("title",title); put("category",category); put("content",content); put("publishedAt",publishedAt) }
