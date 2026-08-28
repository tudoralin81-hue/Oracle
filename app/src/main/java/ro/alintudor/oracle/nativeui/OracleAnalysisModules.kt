package ro.alintudor.oracle.nativeui

import android.graphics.Color
import android.graphics.Typeface
import android.widget.TextView
import ro.alintudor.oracle.core.*

class OracleSimpleModule(private val host: OracleNativeModule, private val moduleTitle: String) {
    fun render(actions: List<OracleAction> = emptyList(), knowledge: List<OracleKnowledgeItem> = emptyList()) {
        host.content.removeAllViews(); host.addCard(moduleTitle, "Modul Oracle nativ")
        val items = if (moduleTitle == "KNOWLEDGE") knowledge.map { "${it.category}\n${it.title}\n${it.content}" } else actions.map { "${it.action}  •  ${it.ticker}  •  ${"%.1f".format(it.score)}\n${it.reason}" }
        if (items.isEmpty()) host.addCard("Aștept date", "Datele modulului vor fi încărcate în stratul nativ.")
        items.forEach { text ->
            host.content.addView(TextView(host.root.context).apply { this.text=text; textSize=16f; typeface=Typeface.DEFAULT_BOLD; setTextColor(Color.WHITE); setPadding(18,18,18,18); setBackgroundColor(Color.rgb(9,13,26)) }, android.widget.LinearLayout.LayoutParams(-1,-2).apply{setMargins(0,0,0,10)})
        }
    }
}
