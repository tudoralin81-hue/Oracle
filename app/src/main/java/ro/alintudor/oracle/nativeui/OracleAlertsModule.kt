package ro.alintudor.oracle.nativeui

import android.graphics.Color
import android.graphics.Typeface
import android.widget.TextView
import ro.alintudor.oracle.core.OracleAlert

class OracleAlertsModule(private val host: OracleNativeModule) {
    fun render(alerts: List<OracleAlert>) {
        host.content.removeAllViews()
        host.addCard("SELL ALERTS", "Alerte active și istoricul alertelor")
        if (alerts.isEmpty()) { host.addCard("Fără alerte", "Nu există alerte active în datele locale."); return }
        alerts.forEach { a ->
            val v=TextView(host.root.context).apply { text="${a.level}  •  ${a.ticker}\n${a.title}\n${a.message}"; textSize=16f; typeface=Typeface.DEFAULT_BOLD; setTextColor(Color.WHITE); setPadding(18,18,18,18); setBackgroundColor(Color.rgb(9,13,26)) }
            host.content.addView(v, android.widget.LinearLayout.LayoutParams(-1,-2).apply{setMargins(0,0,0,10)})
        }
    }
}
