package ro.alintudor.oracle.nativeui

import android.graphics.Color
import android.graphics.Typeface
import android.widget.TextView
import ro.alintudor.oracle.core.OracleAlert
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** Native alert center. Everything is rendered from the local Oracle state. */
class OracleAlertsModule(private val host: OracleNativeModule) {
    private val date = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault())

    fun render(alerts: List<OracleAlert>) {
        host.content.removeAllViews()
        val active = alerts.filter { it.active }
        val high = active.count { it.level.equals("HIGH", true) }
        val medium = active.count { it.level.equals("MEDIUM", true) }

        host.addCard("SELL ALERTS", "Centrul local de alerte Oracle")
        host.addCard("SUMMARY", "ACTIVE ${active.size}  •  HIGH $high  •  MEDIUM $medium")
        if (alerts.isEmpty()) {
            host.addCard("Fără alerte", "Nu există alerte în datele locale.")
            return
        }

        alerts.sortedWith(compareByDescending<OracleAlert> { it.active }.thenByDescending { severityRank(it.level) }.thenByDescending { it.timestamp })
            .take(100)
            .forEach { a ->
                val state = if (a.active) "ACTIVE" else "CLOSED"
                val time = if (a.timestamp > 0L) date.format(Date(a.timestamp)) else ""
                addItem("${a.level.uppercase(Locale.getDefault())} • $state • ${a.ticker}", "${a.title}\n${a.message}\n$time")
            }
    }

    private fun severityRank(level: String) = when (level.uppercase(Locale.getDefault())) {
        "HIGH" -> 3
        "MEDIUM" -> 2
        else -> 1
    }

    private fun addItem(title: String, body: String) {
        host.content.addView(TextView(host.root.context).apply {
            text = "$title\n$body"
            textSize = 15f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(Color.WHITE)
            setPadding(18, 16, 18, 16)
            setBackgroundColor(Color.rgb(9, 13, 26))
        }, android.widget.LinearLayout.LayoutParams(-1, -2).apply { setMargins(0, 0, 0, 10) })
    }
}
