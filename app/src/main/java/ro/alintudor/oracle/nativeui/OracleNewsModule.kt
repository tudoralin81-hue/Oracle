package ro.alintudor.oracle.nativeui

import android.graphics.Color
import android.graphics.Typeface
import android.widget.TextView
import ro.alintudor.oracle.core.OracleNews

class OracleNewsModule(private val host: OracleNativeModule) {
    fun render(news: List<OracleNews>) {
        host.content.removeAllViews(); host.addCard("NEWS", "Știri economice și breaking news")
        if (news.isEmpty()) { host.addCard("Aștept știri", "Nu există încă știri în cache."); return }
        news.forEach { n ->
            val v=TextView(host.root.context).apply { text="${if(n.breaking) "BREAKING • " else ""}${n.ticker}\n${n.title}\n${n.source}"; textSize=16f; typeface=Typeface.DEFAULT_BOLD; setTextColor(Color.WHITE); setPadding(18,18,18,18); setBackgroundColor(Color.rgb(9,13,26)) }
            host.content.addView(v, android.widget.LinearLayout.LayoutParams(-1,-2).apply{setMargins(0,0,0,10)})
        }
    }
}
