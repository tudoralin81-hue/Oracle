package ro.alintudor.oracle.nativeui

import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import ro.alintudor.oracle.core.OracleKnowledgeArticle
import ro.alintudor.oracle.core.OracleKnowledgeSync
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** Knowledge UI backed by the WordPress REST sync cache. */
class OracleKnowledgeModule(private val host: OracleNativeModule) {
    fun render(
        items: List<OracleKnowledgeArticle>,
        onOpen: (String) -> Unit,
        onRefresh: () -> Unit = {}
    ) {
        host.content.removeAllViews()
        host.addSectionLabel("KNOWLEDGE • ALINTUDOR.RO")

        val context = host.root.context
        val last = OracleKnowledgeSync.lastSuccess(context)
        val error = OracleKnowledgeSync.lastError(context)
        val status = if (last == 0L) {
            "Nesincronizat"
        } else {
            "Ultimul refresh: ${SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault()).format(Date(last))}"
        }

        host.addCard(
            "SURSA CANONICĂ",
            "${OracleKnowledgeSync.SOURCE_URL}\n$status\nActualizare automată: zilnic"
        )

        val refresh = Button(context).apply {
            text = "REFRESH KNOWLEDGE"
            textSize = 13f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(Color.WHITE)
            isAllCaps = false
            background = GradientDrawable().apply {
                setColor(Color.rgb(12, 54, 82))
                cornerRadius = host.dp(11).toFloat()
                setStroke(host.dp(1), Color.rgb(55, 105, 145))
            }
            setOnClickListener { onRefresh() }
        }
        host.content.addView(refresh, LinearLayout.LayoutParams(-1, host.dp(46)).apply {
            setMargins(0, 0, 0, host.dp(12))
        })

        if (error.isNotBlank()) {
            host.addCard("ULTIMA EROARE DE SINCRONIZARE", error)
        }

        if (items.isEmpty()) {
            host.addCard(
                "KNOWLEDGE GOL",
                "Nu există încă articole în cache. Apasă REFRESH KNOWLEDGE pentru preluarea inițială."
            )
            return
        }

        host.addSectionLabel("ARTICOLE • ${items.size}")
        items.forEach { article ->
            val card = LinearLayout(context).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(host.dp(15), host.dp(13), host.dp(15), host.dp(13))
                background = GradientDrawable().apply {
                    setColor(Color.rgb(7, 12, 23))
                    cornerRadius = host.dp(15).toFloat()
                    setStroke(host.dp(1), Color.rgb(38, 55, 80))
                }
            }
            card.addView(TextView(context).apply {
                text = article.title
                textSize = 18f
                typeface = Typeface.DEFAULT_BOLD
                setTextColor(Color.WHITE)
            })
            if (article.publishedAt > 0L) card.addView(TextView(context).apply {
                text = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault()).format(Date(article.publishedAt))
                textSize = 11f
                setTextColor(host.accent)
                setPadding(0, host.dp(5), 0, 0)
            })
            card.addView(TextView(context).apply {
                text = article.excerpt
                textSize = 13f
                setTextColor(Color.rgb(190, 198, 213))
                setPadding(0, host.dp(8), 0, host.dp(8))
            })
            val open = Button(context).apply {
                text = "DESCHIDE ARTICOLUL"
                textSize = 12f
                typeface = Typeface.DEFAULT_BOLD
                isAllCaps = false
                setTextColor(Color.WHITE)
                background = GradientDrawable().apply {
                    setColor(Color.rgb(12, 54, 82))
                    cornerRadius = host.dp(11).toFloat()
                }
                setOnClickListener { onOpen(article.url) }
            }
            card.addView(open, LinearLayout.LayoutParams(-1, host.dp(44)))
            host.content.addView(card, LinearLayout.LayoutParams(-1, -2).apply {
                setMargins(0, 0, 0, host.dp(10))
            })
        }
    }
}
