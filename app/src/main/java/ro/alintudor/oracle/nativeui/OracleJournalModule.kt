package ro.alintudor.oracle.nativeui

import android.graphics.Color
import android.graphics.Typeface
import android.widget.TextView
import ro.alintudor.oracle.core.*
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** Offline activity journal and history timeline. */
class OracleJournalModule(private val host: OracleNativeModule) {
    private val date = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault())

    fun render(journal: List<OracleJournalEntry>, history: List<OracleHistoryPoint>, alerts: List<OracleAlert>) {
        host.content.removeAllViews()
        host.addCard("JURNAL ACTIVITATE", "Toate acțiunile, alertele și mișcările locale Oracle")
        val timeline = OracleLocalTimeline.build(history, journal.map { OracleAction(it.ticker,it.action,it.score,it.reason,it.timestamp) }, alerts)
        if (timeline.isEmpty()) { host.addCard("Fără activitate", "Nu există încă evenimente locale."); return }
        timeline.take(150).forEach { item ->
            val label = if (item.severity != "INFO") "${item.type} • ${item.severity}" else item.type
            addItem("${date.format(Date(item.timestamp))}  •  ${item.ticker}", "$label\n${item.title}\n${item.detail}")
        }
    }

    private fun addItem(title: String, body: String) {
        host.content.addView(TextView(host.root.context).apply {
            text = "$title\n$body"
            textSize = 15f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(Color.WHITE)
            setPadding(18,16,18,16)
            setBackgroundColor(Color.rgb(9,13,26))
        }, android.widget.LinearLayout.LayoutParams(-1,-2).apply { setMargins(0,0,0,10) })
    }
}
