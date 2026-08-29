package ro.alintudor.oracle.nativeui

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.view.Gravity
import android.widget.LinearLayout
import android.widget.TextView
import ro.alintudor.oracle.core.OracleNews
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

/** Native financial-news presentation. Oracle remains responsible for sourcing and analysis. */
class OracleNewsModule(private val host: OracleNativeModule) {
    private val time = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.US).apply { timeZone = TimeZone.getTimeZone("Europe/Bucharest") }

    fun render(news: List<OracleNews>) {
        host.content.removeAllViews()
        host.addCard("NEWS", "Știri economice, catalizatori și breaking news")
        if (news.isEmpty()) {
            host.addCard("AȘTEPT ȘTIRI", "Nu există încă știri în cache. Apasă REFRESH când sursa Oracle este disponibilă.")
            return
        }
        val sorted = news.sortedWith(compareByDescending<OracleNews> { it.breaking }.thenByDescending { it.publishedAt })
        addSummary(sorted)
        sorted.take(100).forEachIndexed { i, item -> addNews(i + 1, item) }
    }

    private fun addSummary(news: List<OracleNews>) {
        val breaking = news.count { it.breaking }
        val relevant = news.count { it.relevanceScore >= 70.0 }
        val text = "${news.size} articole  •  $breaking BREAKING  •  $relevant relevante"
        host.addCard("MARKET FEED", text)
    }

    private fun addNews(rank: Int, n: OracleNews) {
        val accent = if (n.breaking) Color.rgb(255, 75, 60) else Color.rgb(25, 205, 255)
        val card = LinearLayout(host.root.context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(host.dp(15), host.dp(13), host.dp(14), host.dp(13))
            background = GradientDrawable().apply {
                setColor(Color.rgb(6, 10, 20)); cornerRadius = host.dp(14).toFloat(); setStroke(host.dp(1), accent)
            }
            isClickable = n.url.isNotBlank(); isFocusable = isClickable
            if (isClickable) setOnClickListener { runCatching { host.root.context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(n.url))) } }
        }
        val top = LinearLayout(host.root.context).apply { gravity = Gravity.CENTER_VERTICAL }
        top.addView(TextView(host.root.context).apply {
            text = "%02d".format(rank); textSize = 10f; typeface = Typeface.DEFAULT_BOLD; setTextColor(accent); gravity = Gravity.CENTER
        }, LinearLayout.LayoutParams(host.dp(32), host.dp(25)))
        top.addView(TextView(host.root.context).apply {
            text = n.ticker.ifBlank { "MARKET" }; textSize = 15f; typeface = Typeface.DEFAULT_BOLD; setTextColor(Color.WHITE)
        }, LinearLayout.LayoutParams(0, -2, 1f))
        top.addView(TextView(host.root.context).apply {
            text = if (n.breaking) "BREAKING" else n.sourceType.ifBlank { "NEWS" }; textSize = 9f; typeface = Typeface.DEFAULT_BOLD; setTextColor(accent); gravity = Gravity.CENTER
        }, LinearLayout.LayoutParams(host.dp(78), host.dp(25)))
        card.addView(top)
        card.addView(TextView(host.root.context).apply {
            text = n.title; textSize = 15f; typeface = Typeface.DEFAULT_BOLD; setTextColor(Color.WHITE); setPadding(host.dp(32), host.dp(5), 0, 0)
        })
        val publisher = n.publisher.ifBlank { n.source }
        val meta = buildList {
            if (publisher.isNotBlank()) add(publisher)
            if (n.publishedAt > 0) add(time.format(Date(n.publishedAt)))
            if (n.relevanceScore > 0) add("Rel %.0f".format(n.relevanceScore))
            n.sentimentScore?.let { add("Sent %+.2f".format(it)) }
        }.joinToString("  •  ")
        if (meta.isNotBlank()) card.addView(TextView(host.root.context).apply {
            text = meta; textSize = 11f; setTextColor(Color.rgb(145, 155, 176)); setPadding(host.dp(32), host.dp(5), 0, 0)
        })
        if (n.url.isNotBlank()) card.addView(TextView(host.root.context).apply {
            text = "DESCHIDE ARTICOLUL  ›"; textSize = 10f; typeface = Typeface.DEFAULT_BOLD; setTextColor(accent); setPadding(host.dp(32), host.dp(7), 0, 0)
        })
        host.content.addView(card, LinearLayout.LayoutParams(-1, -2).apply { setMargins(0, 0, 0, host.dp(9)) })
    }
}
