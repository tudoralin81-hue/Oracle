package ro.alintudor.oracle

import android.app.Activity
import android.os.Bundle
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.view.MotionEvent
import android.view.View
import kotlin.math.min

class MainActivity : Activity() {
    private lateinit var oracleView: OracleView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        oracleView = OracleView()
        setContentView(oracleView)
    }

    override fun onBackPressed() {
        if (oracleView.goBack()) return
        super.onBackPressed()
    }

    inner class OracleView : View(this@MainActivity) {
        private val bg = Color.rgb(2, 4, 10)
        private val panel = Color.rgb(8, 13, 23)
        private val panel2 = Color.rgb(11, 17, 29)
        private val text = Color.rgb(238, 242, 250)
        private val muted = Color.rgb(137, 151, 177)
        private val accent = Color.rgb(139, 124, 255)
        private val green = Color.rgb(54, 211, 153)
        private val red = Color.rgb(248, 113, 113)
        private val gold = Color.rgb(220, 177, 76)
        private val p = Paint(Paint.ANTI_ALIAS_FLAG)
        private var page = "HUB"
        private val history = ArrayDeque<String>()

        private val positions = listOf("NVDA" to 14.8f, "VRT" to 12.4f, "RZLV" to 8.7f, "PLTR" to 7.9f, "SOUN" to 5.8f)
        private val alerts = listOf("VRT — trailing stop approaching", "RZLV — momentum weakening", "NVDA — profit target reached")
        private val journal = listOf("BUY NVDA · 10 shares", "BUY VRT · 20 shares", "SELL CRSP · position closed")
        private val watch = listOf("NVDA", "VRT", "RZLV", "PLTR", "SOUN", "AMD")
        private val news = listOf("US yields and equities in focus", "AI infrastructure spending accelerates", "Semiconductors lead risk appetite", "Markets await inflation signals")
        private val articles = listOf(
            "Trendul și structura pieței — Aplicarea profesionistă",
            "Trendul și structura pieței — Citirea pe grafic",
            "Trendul și structura pieței — Fundamentul",
            "Cum funcționează piața în spatele graficului",
            "Bazele pieței și limbajul graficului — Aplicare",
            "Bazele pieței și limbajul graficului — Citirea pe grafic",
            "Bazele pieței și limbajul graficului — FUNDAMENTUL"
        )

        init { setBackgroundColor(bg) }

        private fun sx(v: Float) = v * width / 1080f
        private fun sy(v: Float) = v * height / 1920f
        private fun fs(v: Float) = v * min(width / 1080f, height / 1920f)

        private fun txt(c: Canvas, s: String, x: Float, y: Float, size: Float, color: Int = text, bold: Boolean = false) {
            p.color = color; p.textSize = fs(size)
            p.typeface = Typeface.create("sans", if (bold) Typeface.BOLD else Typeface.NORMAL)
            c.drawText(s, sx(x), sy(y), p)
        }
        private fun center(c: Canvas, s: String, x: Float, y: Float, size: Float, color: Int = text, bold: Boolean = true) {
            p.color = color; p.textSize = fs(size)
            p.typeface = Typeface.create("sans", if (bold) Typeface.BOLD else Typeface.NORMAL)
            c.drawText(s, sx(x) - p.measureText(s) / 2f, sy(y), p)
        }
        private fun box(c: Canvas, l: Float, t: Float, r: Float, b: Float, color: Int = panel, rad: Float = 26f) {
            p.color = color; p.style = Paint.Style.FILL
            c.drawRoundRect(sx(l), sy(t), sx(r), sy(b), sx(rad), sx(rad), p)
        }
        private fun divider(c: Canvas, y: Float) {
            p.color = Color.rgb(30, 38, 55); p.strokeWidth = sx(1f)
            c.drawLine(sx(50f), sy(y), sx(1030f), sy(y), p)
        }

        override fun onDraw(c: Canvas) {
            c.drawColor(bg)
            drawHeader(c, pageTitle())
            when (page) {
                "HUB" -> drawHub(c)
                "PORTFOLIO" -> drawPortfolio(c)
                "ALERTS" -> drawAlerts(c)
                "JOURNAL" -> drawJournal(c)
                "NEWS" -> drawNews(c)
                "GROWTH" -> drawGrowth(c)
                "ANALYSIS" -> drawAnalysis(c)
                "WATCHLIST" -> drawWatch(c)
                "KNOWLEDGE" -> drawKnowledge(c)
            }
        }

        private fun drawHeader(c: Canvas, title: String) {
            box(c, 22f, 20f, 1058f, 145f, panel, 26f)
            if (page != "HUB") {
                center(c, "‹", 70f, 94f, 58f, text, false)
                txt(c, "Înapoi", 110f, 58f, 13f, muted)
            }
            txt(c, "ORACLE", if (page == "HUB") 50f else 155f, 62f, 25f, accent, true)
            txt(c, title, if (page == "HUB") 50f else 155f, 108f, 31f, text, true)
        }

        private fun pageTitle() = when(page) {
            "HUB" -> "AI Stock Oracle"; "PORTFOLIO" -> "Portfolio"; "ALERTS" -> "SELL Alerts"
            "JOURNAL" -> "Jurnal activitate"; "NEWS" -> "News"; "GROWTH" -> "Growth"
            "ANALYSIS" -> "Analysis"; "WATCHLIST" -> "Watchlist"; "KNOWLEDGE" -> "Knowledge"
            else -> "Oracle"
        }

        private fun drawHub(c: Canvas) {
            val cx = 540f; val cy = 930f; val radius = 355f
            p.color = Color.rgb(37, 46, 67); p.style = Paint.Style.STROKE; p.strokeWidth = sx(2f)
            c.drawCircle(sx(cx), sy(cy), sx(radius), p); p.style = Paint.Style.FILL
            hubNode(c, cx, cy-radius, "Portfolio", accent)
            hubNode(c, cx-radius*.9f, cy-radius*.38f, "Alerts", red)
            hubNode(c, cx+radius*.9f, cy-radius*.38f, "News", green)
            hubNode(c, cx-radius*.8f, cy+radius*.55f, "Growth", accent)
            hubNode(c, cx+radius*.8f, cy+radius*.55f, "Watchlist", accent)
            hubNode(c, cx-radius*.32f, cy+radius*.92f, "Analysis", green)
            hubNode(c, cx+radius*.32f, cy+radius*.92f, "Journal", accent)
            hubNode(c, cx, cy+radius*1.18f, "Knowledge", gold)
            center(c, "ORACLE", cx, cy+8f, 38f, text, true)
            center(c, "AI STOCK", cx, cy+40f, 16f, muted, false)
            box(c, 40f, 1580f, 1040f, 1795f, panel, 28f)
            txt(c, "12-MONTH FORECAST", 68f, 1630f, 16f, muted, true)
            txt(c, "$161,900", 68f, 1690f, 42f, text, true)
            txt(c, "+26.0% projected", 68f, 1725f, 18f, green, true)
            txt(c, "BASE", 830f, 1635f, 13f, muted, true)
            txt(c, "BULL", 830f, 1680f, 16f, green, true)
            txt(c, "BEAR", 830f, 1725f, 16f, red, true)
        }

        private fun hubNode(c: Canvas, x: Float, y: Float, label: String, color: Int) {
            p.color = color; p.style = Paint.Style.FILL
            c.drawCircle(sx(x), sy(y), sx(58f), p)
            center(c, label, x, y+6f, 14f, Color.WHITE, true)
        }

        private fun drawPortfolio(c: Canvas) {
            box(c, 40f, 175f, 1040f, 350f, panel, 28f)
            txt(c, "TOTAL PORTFOLIO", 70f, 225f, 16f, muted, true)
            txt(c, "$128,420", 70f, 290f, 46f, text, true)
            txt(c, "+18.6%", 850f, 270f, 23f, green, true)
            var y = 420f
            positions.forEach { (ticker, weight) ->
                box(c, 40f, y-40f, 1040f, y+82f, panel2, 24f)
                txt(c, ticker, 70f, y+5f, 27f, text, true)
                txt(c, "Weight ${"%.1f".format(weight)}%", 70f, y+45f, 17f, muted)
                txt(c, if (weight > 10f) "HOLD" else "WATCH", 850f, y+20f, 18f, if (weight > 10f) green else accent, true)
                y += 140f
            }
        }

        private fun drawAlerts(c: Canvas) {
            var y = 205f
            alerts.forEachIndexed { i, item ->
                box(c, 40f, y-42f, 1040f, y+88f, panel2, 24f)
                p.color = if (i == 2) green else red; c.drawCircle(sx(78f), sy(y+22f), sx(11f), p)
                txt(c, item, 112f, y+18f, 20f, text, true)
                txt(c, "Signal engine · 08:25", 112f, y+58f, 15f, muted)
                y += 155f
            }
            actionButton(c, "RUN ALERT SCAN", 40f, y+15f, 1040f, y+95f)
        }

        private fun drawJournal(c: Canvas) {
            var y = 205f
            journal.forEachIndexed { i, item ->
                box(c, 40f, y-42f, 1040f, y+82f, panel2, 24f)
                txt(c, "08:2$i", 70f, y+8f, 16f, muted)
                txt(c, item, 150f, y+14f, 20f, text, true)
                y += 150f
            }
            actionButton(c, "EXPORT JOURNAL", 40f, y+20f, 1040f, y+100f)
            txt(c, "Istoric complet și mișcările pozițiilor vor fi sincronizate cu data layer Oracle.", 40f, y+155f, 14f, muted)
        }

        private fun drawNews(c: Canvas) {
            var y = 195f
            news.forEach { title ->
                box(c, 40f, y-38f, 1040f, y+105f, panel2, 24f)
                txt(c, "ECONOMY", 70f, y, 14f, accent, true)
                txt(c, title, 70f, y+42f, 21f, text, true)
                txt(c, "Updated today · breaking monitor", 70f, y+80f, 15f, muted)
                y += 160f
            }
            txt(c, "12 surse economice · feedurile Oracle rămân parte din specificație.", 40f, 900f, 15f, muted)
        }

        private fun drawGrowth(c: Canvas) {
            box(c, 40f, 175f, 1040f, 430f, panel, 28f)
            txt(c, "12-MONTH FORECAST", 70f, 230f, 16f, muted, true)
            txt(c, "$161,900", 70f, 300f, 50f, text, true)
            txt(c, "+26.0% projected", 70f, 342f, 20f, green, true)
            txt(c, "Scenario", 830f, 230f, 14f, muted)
            txt(c, "Base", 830f, 272f, 19f, text, true)
            txt(c, "Bull", 830f, 312f, 19f, green, true)
            txt(c, "Bear", 830f, 352f, 19f, red, true)
            for (i in 0..8) {
                val x = 70f + i*105f; val h = 80f + i*25f
                p.color = Color.rgb(38, 51, 76); c.drawRect(sx(x), sy(800f-h), sx(x+64f), sy(800f), p)
            }
        }

        private fun drawAnalysis(c: Canvas) {
            box(c, 40f, 175f, 1040f, 315f, panel, 28f)
            txt(c, "Ticker", 70f, 230f, 16f, muted)
            txt(c, "NVDA", 190f, 230f, 26f, text, true)
            txt(c, "BULLISH", 850f, 230f, 18f, green, true)
            val rows = arrayOf("Trend" to "Above EMA 20/50", "Momentum" to "Positive", "Support" to "$172.00", "Resistance" to "$184.50", "Risk" to "Medium")
            var y = 380f
            rows.forEach { (a,b) ->
                divider(c, y-30f)
                txt(c, a, 70f, y, 18f, muted)
                txt(c, b, 430f, y, 20f, text, true)
                y += 86f
            }
        }

        private fun drawWatch(c: Canvas) {
            var y = 195f
            watch.forEach { ticker ->
                box(c, 40f, y-38f, 1040f, y+72f, panel2, 24f)
                txt(c, ticker, 70f, y+22f, 27f, text, true)
                txt(c, "Live watch · signal ready", 250f, y+22f, 17f, muted)
                y += 130f
            }
            actionButton(c, "+ ADD TICKER", 40f, y+15f, 1040f, y+95f)
        }

        private fun drawKnowledge(c: Canvas) {
            var y = 195f
            articles.forEachIndexed { i, title ->
                box(c, 40f, y-38f, 1040f, y+100f, panel2, 24f)
                txt(c, "${i+1}", 70f, y+42f, 23f, gold, true)
                txt(c, title, 130f, y+20f, 19f, text, true)
                txt(c, "Pastile pentru knowledge", 130f, y+60f, 15f, muted)
                y += 150f
            }
        }

        private fun actionButton(c: Canvas, label: String, l: Float, t: Float, r: Float, b: Float) {
            box(c, l, t, r, b, Color.rgb(25,34,54), 20f)
            center(c, label, (l+r)/2f, t+51f, 18f, accent, true)
        }

        private fun navigate(next: String) {
            if (page == next) return
            history.addLast(page)
            page = next
            invalidate()
        }

        fun goBack(): Boolean {
            if (history.isEmpty()) {
                if (page == "HUB") return false
                page = "HUB"
            } else {
                page = history.removeLast()
            }
            invalidate()
            return true
        }

        override fun onTouchEvent(e: MotionEvent): Boolean {
            if (e.action != MotionEvent.ACTION_UP) return true
            val x = e.x * 1080f / width
            val y = e.y * 1920f / height

            if (page != "HUB" && y < 160f && x < 130f) { goBack(); return true }

            if (page == "HUB") {
                val cx=540f; val cy=930f; val radius=355f
                val dx=x-cx; val dy=y-cy
                when {
                    dy < -radius*.65f -> navigate("PORTFOLIO")
                    dx < -radius*.60f && dy < 0 -> navigate("ALERTS")
                    dx > radius*.60f && dy < 0 -> navigate("NEWS")
                    dx < -radius*.45f && dy > 0 -> navigate("GROWTH")
                    dx > radius*.45f && dy > 0 -> navigate("WATCHLIST")
                    dy > radius*.85f && dx > 0 -> navigate("JOURNAL")
                    dy > radius*.85f && dx < 0 -> navigate("KNOWLEDGE")
                    dy > radius*.45f -> navigate("ANALYSIS")
                }
            }
            invalidate(); return true
        }
    }
}
