package ro.alintudor.oracle

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

/** Local-first store. Keeps the application usable without a WordPress UI. */
class OracleStore(context: Context) {
    private val prefs = context.getSharedPreferences("oracle_state", Context.MODE_PRIVATE)

    fun load(): OracleModuleState {
        fun text(key: String) = prefs.getString(key, "[]") ?: "[]"
        val positions = JSONArray(text("positions")).let { a -> List(a.length()) { i ->
            val o=a.getJSONObject(i); OraclePosition(o.getString("ticker"),o.getDouble("shares"),o.getDouble("avgCost"),o.optDouble("currentPrice"),o.optDouble("pnl"),o.optDouble("pnlPct"),o.optString("status","ACTIVE"))
        }}
        val alerts = JSONArray(text("alerts")).let { a -> List(a.length()) { i -> val o=a.getJSONObject(i); OracleAlert(o.getString("ticker"),o.getString("type"),o.getString("message"),o.getLong("timestamp")) }}
        val news = JSONArray(text("news")).let { a -> List(a.length()) { i -> val o=a.getJSONObject(i); OracleNews(o.getString("ticker"),o.getString("title"),o.getString("source"),o.getLong("timestamp"),o.optBoolean("breaking")) }}
        val history = JSONArray(text("history")).let { a -> List(a.length()) { i -> val o=a.getJSONObject(i); OracleHistoricalPoint(o.getLong("timestamp"),o.getDouble("value")) }}
        return OracleModuleState(positions,alerts,news,history,prefs.getLong("lastSync",0L))
    }

    fun save(state: OracleModuleState) {
        fun array(values: List<JSONObject>) = JSONArray().also { a -> values.forEach(a::put) }.toString()
        val p=state.positions.map { JSONObject().put("ticker",it.ticker).put("shares",it.shares).put("avgCost",it.avgCost).put("currentPrice",it.currentPrice).put("pnl",it.pnl).put("pnlPct",it.pnlPct).put("status",it.status) }
        val al=state.alerts.map { JSONObject().put("ticker",it.ticker).put("type",it.type).put("message",it.message).put("timestamp",it.timestamp) }
        val n=state.news.map { JSONObject().put("ticker",it.ticker).put("title",it.title).put("source",it.source).put("timestamp",it.timestamp).put("breaking",it.breaking) }
        val h=state.history.map { JSONObject().put("timestamp",it.timestamp).put("value",it.value) }
        prefs.edit().putString("positions",array(p)).putString("alerts",array(al)).putString("news",array(n)).putString("history",array(h)).putLong("lastSync",state.lastSync).apply()
    }
}
