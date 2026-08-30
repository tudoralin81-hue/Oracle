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
        host.addSectionLabel("KNOWLEDGE")

        val context = host.root.context
        val last = OracleKnowledgeSync.lastSuccess(context)
        val error = OracleKnowledgeSync.lastError(context)
        val status = if (last == 0L) {
            "Nesincronizat încă"
        } else {
            "Ultimul refresh: ${SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault()).format(Date(last))}"
        }

        // Hero card — intentionally mirrors the visual language of the Oracle app:
        // dark surface, gold border, centered identity and one obvious primary action.
        val hero = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            setPadding(host.dp(22), host.dp(24), host.dp(22), host.dp(22))
            background = GradientDrawable().apply {
                setColor(Color.rgb(7, 12, 23))
                cornerRadius = host.dp(18).toFloat()
                setStroke(host.dp(1), host.accent)
            }
        }

        hero.addView(TextView(context).apply {
            text = "▱"
            textSize = 58f
            gravity = Gravity.CENTER
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            setTextColor(host.accent)
        }, LinearLayout.LayoutParams(-1, host.dp(70)))

        hero.addView(TextView(context).apply {
            text = "KNOWLEDGE"
            textSize = 28f
            gravity = Gravity.CENTER
            typeface = Typeface.DEFAULT_BOLD
            letterSpacing = 0.12f
            setTextColor(Color.WHITE)
        }, LinearLayout.LayoutParams(-1, host.dp(48)))

        val divider = TextView(context).apply {
            text = "━━━━━━━━━━━━━━━━"
            textSize = 11f
            gravity = Gravity.CENTER
            setTextColor(host.accent)
            alpha = 0.8f
        }
        hero.addView(divider, LinearLayout.LayoutParams(-1, host.dp(28)))

        hero.addView(TextView(context).apply {
            text = "Accesează articole, analize și idei\npentru investitori inteligenți."
            textSize = 15f
            gravity = Gravity.CENTER
            setLineSpacing(host.dp(2).toFloat(), 1f)
            setTextColor(Color.rgb(220, 225, 235))
        }, LinearLayout.LayoutParams(-1, host.dp(60)))

        val openKnowledge = Button(context).apply {
            text = "↗   DESCHIDE ALINTUDOR.RO/KNOWLEDGE"
            textSize = 13f
            typeface = Typeface.DEFAULT_BOLD
            isAllCaps = false
            setTextColor(Color.rgb(8, 12, 20))
            background = GradientDrawable().apply {
                setColor(host.accent)
                cornerRadius = host.dp(12).toFloat()
            }
            setOnClickListener { onOpen(OracleKnowledgeSync.SOURCE_URL) }
        }
        hero.addView(openKnowledge, LinearLayout.LayoutParams(-1, host.dp(52)).apply {
            setMargins(0, host.dp(12), 0, 0)
        })

        host.content.addView(hero, LinearLayout.LayoutParams(-1, -2).apply {
            setMargins(0, 0, 0, host.dp(12))
        })

        // Small information card below the hero, as in the proposed design.
        val info = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(host.dp(16), host.dp(14), host.dp(16), host.dp(14))
            background = GradientDrawable().apply {
                setColor(Color.rgb(9, 15, 27))
                cornerRadius = host.dp(15).toFloat()
                setStroke(host.dp(1), Color.rgb(38, 55, 80))
            }
        }
        info.addView(TextView(context).apply {
            text = "✓"
            textSize = 25f
            gravity = Gravity.CENTER
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(host.accent)
        }, LinearLayout.LayoutParams(host.dp(42), host.dp(42)))
        info.addView(TextView(context).apply {
            text = "Conținut independent\nSe deschide în browser.\n$status"
            textSize = 13f
            setLineSpacing(host.dp(2).toFloat(), 1f)
            setTextColor(Color.rgb(205, 212, 225))
            setPadding(host.dp(10), 0, 0, 0)
        }, LinearLayout.LayoutParams(0, -2, 1f))
        host.content.addView(info, LinearLayout.LayoutParams(-1, -2).apply {
            setMargins(0, 0, 0, host.dp(14))
        })

        val refresh = Button(context).apply {
            text = "⟳  REFRESH KNOWLEDGE"
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
                "ARTICOLE",
                "Nu există încă articole în cache. Folosește REFRESH KNOWLEDGE pentru preluarea inițială."
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
