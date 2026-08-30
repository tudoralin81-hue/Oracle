package ro.alintudor.oracle.nativeui

import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.text.Editable
import android.text.InputType
import android.text.TextWatcher
import android.view.Gravity
import android.view.inputmethod.InputMethodManager
import android.view.WindowManager
import android.widget.*
import ro.alintudor.oracle.core.*
import java.util.Locale
import kotlin.math.abs

class OracleSimpleModule(private val host: OracleNativeModule, private val moduleTitle: String, private val onWatchlistTickerClick: (String) -> Unit = {}) {
    companion object {
        @Volatile private var tickerDraft: String = ""
        fun setTickerDraft(ticker: String) { tickerDraft = ticker.trim().uppercase(Locale.US) }
    }

    fun render(actions: List<OracleAction> = emptyList(), knowledge: List<OracleKnowledgeItem> = emptyList(), positions: List<OraclePosition> = emptyList(), history: List<OracleHistoryPoint> = emptyList(), watchlist: List<String> = OracleWatchlistStore(host.root.context).load()) {
        host.content.removeAllViews()
        val p = OracleAnalytics.normalize(positions)
        val computed = OracleAnalytics.actions(p, history)
        when (moduleTitle) {
            "GROWTH" -> renderGrowth()
            "ANALYSIS" -> renderAnalysis()
            "WATCHLIST" -> renderWatchlist(watchlist)
            "KNOWLEDGE" -> renderKnowledge(knowledge)
            else -> renderActions(if (computed.isNotEmpty()) computed else actions)
        }
    }

    private fun renderGrowth() {
        val r = OracleRepository(host.root.context)
        OracleGrowthModule(host).render(r.cachedGrowth(), r.cachedNews())
    }

    private fun renderAnalysis() {
        host.addSectionLabel("ANALYSIS • SINGLE TICKER")
        val input = EditText(host.root.context).apply {
            hint = "Introdu tickerul (ex. NVDA)"
            setSingleLine(true)
            textSize = 18f
            setTextColor(Color.WHITE)
            setHintTextColor(Color.rgb(130, 145, 170))
            setPadding(host.dp(16), 0, host.dp(16), 0)
            background = GradientDrawable().apply {
                setColor(Color.rgb(8, 14, 28))
                cornerRadius = host.dp(14).toFloat()
                setStroke(host.dp(1), host.accent)
            }
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_CAP_CHARACTERS
            imeOptions = android.view.inputmethod.EditorInfo.IME_ACTION_DONE
            isFocusable = true
            isFocusableInTouchMode = true
            showSoftInputOnFocus = true
            if (tickerDraft.isNotBlank()) setText(tickerDraft)
        }
        input.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                tickerDraft = s?.toString() ?: ""
                if (tickerDraft.isNotBlank()) {
                    input.post {
                        if (input.windowToken != null) {
                            input.requestFocus()
                            (host.root.context.getSystemService(InputMethodManager::class.java))?.showSoftInput(input, InputMethodManager.SHOW_IMPLICIT)
                        }
                    }
                }
            }
            override fun afterTextChanged(s: Editable?) = Unit
        })
        host.fixedToolbar.addView(input, LinearLayout.LayoutParams(-1, host.dp(52)).apply { setMargins(0, host.dp(3), 0, host.dp(6)) })
        if (tickerDraft.isNotBlank()) {
            input.setSelection(input.text.length)
            input.post {
                input.requestFocus()
                (host.root.context.getSystemService(InputMethodManager::class.java))?.showSoftInput(input, InputMethodManager.SHOW_IMPLICIT)
            }
        }

        input.postDelayed({
            input.requestFocus()
            val imm = host.root.context.getSystemService(InputMethodManager::class.java)
            imm?.showSoftInput(input, InputMethodManager.SHOW_IMPLICIT)
            (host.root.context as? android.app.Activity)?.window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE)
        }, 180L)
        val button = Button(host.root.context).apply {
            text = "ANALIZEAZĂ TICKER"
            textSize = 13f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(Color.WHITE)
            background = GradientDrawable().apply {
                setColor(Color.rgb(15, 75, 110))
                cornerRadius = host.dp(13).toFloat()
            }
        }
        host.fixedToolbar.addView(button, LinearLayout.LayoutParams(-1, host.dp(48)).apply { setMargins(0, 0, 0, host.dp(8)) })

        fun run() {
            val t = input.text.toString().trim().uppercase(Locale.US)
            if (t.isBlank()) {
                input.error = "Introdu un ticker"
                return
            }
            tickerDraft = t
            button.isEnabled = false
            button.text = "SE ANALIZEAZĂ…"
            Thread {
                val x = runCatching { OracleAnalysisEngine.analyze(t) }
                host.root.post {
                    button.isEnabled = true
                    button.text = "ANALIZEAZĂ TICKER"
                    x.onSuccess { renderResult(it) }
                        .onFailure { Toast.makeText(host.root.context, "Analiza a eșuat: ${it.message ?: it.javaClass.simpleName}", Toast.LENGTH_LONG).show() }
                }
            }.start()
        }
        button.setOnClickListener { run() }
        input.setOnEditorActionListener { _, actionId, _ -> if (actionId == android.view.inputmethod.EditorInfo.IME_ACTION_DONE) { run(); true } else false }
    }

    private fun renderResult(r: OracleAnalysisEngine.Result?) {
        if (r == null) {
            Toast.makeText(host.root.context, "Tickerul nu a putut fi găsit / analizat.", Toast.LENGTH_LONG).show()
            return
        }
        if (host.content.childCount > 1) host.content.removeViews(1, host.content.childCount - 1)

        val top = LinearLayout(host.root.context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(host.dp(16), host.dp(14), host.dp(16), host.dp(14))
            background = GradientDrawable().apply {
                setColor(Color.rgb(5, 10, 19))
                cornerRadius = host.dp(16).toFloat()
                setStroke(host.dp(1), host.accent)
            }
        }
        val headline = LinearLayout(host.root.context).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL }
        headline.addView(TextView(host.root.context).apply {
            text = r.ticker
            textSize = 30f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(Color.WHITE)
        }, LinearLayout.LayoutParams(0, -2, 1f))
        headline.addView(TextView(host.root.context).apply {
            text = money(r.price)
            textSize = 17f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(Color.rgb(45, 232, 92))
            gravity = Gravity.END
        })
        top.addView(headline)
        top.addView(TextView(host.root.context).apply {
            text = "${companyName(r.ticker)}   •   Sector: ${sector(r.ticker)}"
            textSize = 15f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(Color.rgb(205, 213, 228))
            setPadding(0, host.dp(4), 0, 0)
        })
        host.content.addView(top, LinearLayout.LayoutParams(-1, -2).apply { setMargins(0, 0, 0, host.dp(10)) })

        host.addSectionLabel("PARAMETRII ORACLE • VALORI")
        val grid = LinearLayout(host.root.context).apply { orientation = LinearLayout.VERTICAL }
        OracleAnalysisEngine.factorNames.forEachIndexed { i, n ->
            if (n.equals("News", true)) return@forEachIndexed
            val row = LinearLayout(host.root.context).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                setPadding(host.dp(14), host.dp(10), host.dp(12), host.dp(10))
                background = GradientDrawable().apply {
                    setColor(if (i % 2 == 0) Color.rgb(7, 12, 23) else Color.rgb(10, 16, 29))
                    cornerRadius = host.dp(9).toFloat()
                }
            }
            row.addView(TextView(host.root.context).apply {
                text = n
                textSize = 14f
                setTextColor(Color.rgb(215, 222, 235))
            }, LinearLayout.LayoutParams(0, -2, 1f))
            row.addView(TextView(host.root.context).apply {
                text = fmt(r.factors[i])
                textSize = 15f
                typeface = Typeface.DEFAULT_BOLD
                setTextColor(Color.WHITE)
                gravity = Gravity.END
            }, LinearLayout.LayoutParams(host.dp(72), -2))
            grid.addView(row, LinearLayout.LayoutParams(-1, -2).apply { setMargins(0, 0, 0, host.dp(3)) })
        }
        host.content.addView(grid, LinearLayout.LayoutParams(-1, -2).apply { setMargins(0, 0, 0, host.dp(10)) })

        host.addSectionLabel("ANALIZĂ ORACLE")
        val card = LinearLayout(host.root.context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(host.dp(15), host.dp(13), host.dp(15), host.dp(13))
            background = GradientDrawable().apply {
                setColor(Color.rgb(7, 12, 23))
                cornerRadius = host.dp(15).toFloat()
                setStroke(host.dp(1), Color.rgb(34, 55, 82))
            }
        }
        analysisLines(r).forEach { line ->
            card.addView(TextView(host.root.context).apply {
                text = "— $line"
                textSize = 13f
                setTextColor(Color.rgb(205, 213, 228))
                setPadding(0, host.dp(4), 0, host.dp(4))
            })
        }
        host.content.addView(card, LinearLayout.LayoutParams(-1, -2).apply { setMargins(0, 0, 0, host.dp(12)) })

        addTechnicalChart(r.ticker)

        val store = OracleWatchlistStore(host.root.context)
        val list = store.load().toMutableList()
        val inWatch = list.any { it.equals(r.ticker, true) }
        val w = Button(host.root.context).apply {
            text = if (inWatch) "✓  ESTE ÎN WATCHLIST" else "＋  ADAUGĂ ÎN WATCHLIST"
            textSize = 13f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(Color.WHITE)
            background = GradientDrawable().apply {
                setColor(if (inWatch) Color.rgb(25, 75, 45) else Color.rgb(95, 55, 10))
                cornerRadius = host.dp(13).toFloat()
            }
            isEnabled = !inWatch
        }
        w.setOnClickListener {
            if (list.none { it.equals(r.ticker, true) }) {
                list.add(r.ticker)
                store.save(list)
                w.text = "✓  ADAUGAT ÎN WATCHLIST"
                w.isEnabled = false
                Toast.makeText(host.root.context, "${r.ticker} adăugat în Watchlist", Toast.LENGTH_SHORT).show()
            }
        }
        host.content.addView(w, LinearLayout.LayoutParams(-1, host.dp(50)).apply { setMargins(0, 0, 0, host.dp(16)) })
    }

    private fun addTechnicalChart(ticker: String) {
        val chartTitle = TextView(host.root.context).apply {
            text = "GRAFIC TEHNIC • DATE REALE"
            textSize = 22f
            typeface = Typeface.DEFAULT_BOLD
            letterSpacing = .10f
            setTextColor(host.accent)
            setPadding(host.dp(5), host.dp(10), host.dp(5), host.dp(10))
        }
        host.content.addView(chartTitle, LinearLayout.LayoutParams(-1, -2))
        val box = LinearLayout(host.root.context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(host.dp(8), host.dp(8), host.dp(8), host.dp(8))
            background = GradientDrawable().apply {
                setColor(Color.rgb(3, 7, 14))
                cornerRadius = host.dp(15).toFloat()
                setStroke(host.dp(1), Color.rgb(34, 55, 82))
            }
        }
        val chart = OracleAnalysisChartView(host.root.context, ticker)
        box.addView(chart, LinearLayout.LayoutParams(-1, host.dp(660)))

        val ranges = LinearLayout(host.root.context).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL }
        val activeRange = Color.rgb(20, 70, 105)
        val inactiveRange = Color.rgb(12, 20, 34)
        fun styleRange(b: Button, active: Boolean) {
            b.alpha = 1f
            b.background = GradientDrawable().apply {
                setColor(if (active) activeRange else inactiveRange)
                cornerRadius = host.dp(9).toFloat()
                setStroke(host.dp(1), Color.rgb(45, 65, 90))
            }
        }
        listOf("5M", "30M", "1H", "1D", "5D", "1M", "3M", "1Y").forEachIndexed { i, label ->
            val b = Button(host.root.context).apply {
                text = label
                textSize = 10f
                typeface = Typeface.DEFAULT_BOLD
                setTextColor(Color.WHITE)
                setPadding(0, 0, 0, 0)
            }
            styleRange(b, i == 0)
            b.setOnClickListener {
                chart.setMode(label)
                for (j in 0 until ranges.childCount) {
                    val other = ranges.getChildAt(j) as Button
                    styleRange(other, other === b)
                }
            }
            ranges.addView(b, LinearLayout.LayoutParams(0, host.dp(44), 1f).apply { setMargins(host.dp(2), host.dp(6), host.dp(2), 0) })
        }
        box.addView(ranges)

        val indicators = LinearLayout(host.root.context).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL }
        listOf("BB", "MA/EMA", "MA Cross", "ICHI", "RSI", "ADX").forEach { label ->
            val b = Button(host.root.context).apply {
                text = label
                textSize = if (label == "MA Cross") 8.5f else 9f
                typeface = Typeface.DEFAULT_BOLD
                setTextColor(Color.rgb(205, 213, 228))
                setPadding(0, 0, 0, 0)
            }
            val initiallyActive = label != "ICHI"
            fun styleIndicator(active: Boolean) {
                b.alpha = 1f
                b.background = GradientDrawable().apply {
                    setColor(if (active) activeRange else Color.rgb(8, 14, 25))
                    cornerRadius = host.dp(8).toFloat()
                    setStroke(host.dp(1), Color.rgb(40, 55, 78))
                }
            }
            styleIndicator(initiallyActive)
            var active = initiallyActive
            b.setOnClickListener {
                chart.toggleIndicator(label)
                active = !active
                styleIndicator(active)
            }
            indicators.addView(b, LinearLayout.LayoutParams(0, host.dp(38), 1f).apply { setMargins(host.dp(2), host.dp(5), host.dp(2), 0) })
        }
        box.addView(indicators)

        val legend = LinearLayout(host.root.context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(host.dp(12), host.dp(10), host.dp(12), host.dp(7))
            background = GradientDrawable().apply {
                setColor(Color.rgb(7, 12, 23))
                cornerRadius = host.dp(10).toFloat()
                setStroke(host.dp(1), Color.rgb(34, 55, 82))
            }
        }
        listOf(
            "— Linie verde/roșie: direcția trendului calculată pe ultimele lumânări.",
            "— Liniile paralele: canalul de variație al trendului.",
            "— Linie albastră: suport tehnic recent.",
            "— Linie aurie: rezistență tehnică recentă.",
            "— Săgeata: scenariul de trend proiectat pe baza structurii actuale.",
            "— Punctele verzi/roșii: semnale MA Cross bullish/bearish.",
            "— BB / MA-EMA / MA Cross / Ichimoku / RSI / ADX: indicatorii activabili de mai sus."
        ).forEach { t ->
            legend.addView(TextView(host.root.context).apply {
                text = t
                textSize = 12.5f
                setTextColor(Color.rgb(195, 205, 220))
                setPadding(0, host.dp(3), 0, host.dp(3))
            })
        }
        box.addView(legend)
        host.content.addView(box, LinearLayout.LayoutParams(-1, -2).apply { setMargins(0, 0, 0, host.dp(14)) })
    }

    private fun analysisLines(r: OracleAnalysisEngine.Result): List<String> {
        val f = r.factors
        return listOf(
            when { f[2] >= 75 -> "Trendul este puternic pozitiv, cu prețul peste mediile relevante."; f[2] >= 50 -> "Trendul este constructiv, dar fără confirmare puternică."; else -> "Trendul este fragil și cere confirmare înaintea unei intrări." },
            when { f[3] >= 70 -> "Momentum-ul susține continuarea mișcării."; f[3] >= 50 -> "Momentum-ul este mixt și nu oferă avantaj clar."; else -> "Momentum-ul este slab și reduce calitatea mișcării." },
            when { f[1] >= 90 -> "Breakout-ul este confirmat de volum."; f[1] >= 60 -> "Prețul testează breakout-ul, dar confirmarea este incompletă."; else -> "Nu există breakout tehnic convingător." },
            if (r.volumeRatio >= 1.25) "Volumul peste media 20D validează mai bine mișcarea." else "Volumul nu validează decisiv mișcarea curentă.",
            when { f[5] >= 70 -> "Poziționarea față de suport și rezistență este favorabilă."; f[5] >= 45 -> "Poziționarea față de suport și rezistență este intermediară."; else -> "Poziționarea în intervalul tehnic recent este nefavorabilă." },
            if (f[7] >= 65) "Bollinger indică o poziționare tehnică favorabilă." else "Bollinger nu confirmă o extensie bullish clară.",
            if (f[8] >= 80) "Ichimoku confirmă structura bullish." else "Ichimoku nu confirmă o structură bullish completă.",
            when { (r.adx ?: 0.0) >= 25 -> "ADX indică o tendință suficient de puternică."; (r.adx ?: 0.0) >= 20 -> "ADX indică o tendință moderată."; else -> "ADX indică o tendință slabă sau neconfirmată." },
            if (r.rsi > 70) "RSI este ridicat; riscul unei supracumpărări trebuie urmărit." else if (r.rsi < 35) "RSI este scăzut; presiunea de vânzare rămâne relevantă." else "RSI este într-o zonă tehnică relativ echilibrată.",
            "Volatilitatea este reflectată de ATR ${fmt(r.atrPct)}%; gestionarea riscului este esențială.",
            if (r.sma50 != null && r.sma200 != null && r.sma50 > r.sma200) "SMA50 peste SMA200 susține structura de trend pe termen mai lung." else "Structura mediilor mobile nu oferă o confirmare bullish completă.",
            if (f[4] >= 65) "Fluxul de volum susține interesul pentru mișcarea actuală." else "Fluxul de volum nu oferă o confirmare decisivă.",
            "Contextul tehnic trebuie urmărit împreună cu suportul apropiat și volatilitatea.",
            if (f[10] >= 65) "Raportul tehnic risc/recompensă este relativ favorabil." else "Raportul risc/recompensă cere prudență înaintea intrării.",
            "Concluzie: configurația actuală trebuie evaluată dinamic la următoarea sesiune."
        )
    }

    private fun companyName(t: String) = when (t) {
        "AAOI" -> "Applied Optoelectronics, Inc."
        "NVDA" -> "NVIDIA Corporation"; "AAPL" -> "Apple Inc."; "MSFT" -> "Microsoft Corporation"; "AMZN" -> "Amazon.com, Inc."; "GOOGL" -> "Alphabet Inc."; "META" -> "Meta Platforms, Inc."; "TSLA" -> "Tesla, Inc."; "AMD" -> "Advanced Micro Devices, Inc."; "AVGO" -> "Broadcom Inc."; "NFLX" -> "Netflix, Inc."; else -> t
    }
    private fun sector(t: String) = when (t) {
        "AAOI" -> "Technology / Optical Networking"
        "NVDA", "AMD", "AVGO" -> "Semiconductors"; "AAPL", "MSFT", "GOOGL", "META", "AMZN", "NFLX" -> "Technology / Internet"; "TSLA" -> "Automotive / Energy"; else -> "Sector indisponibil"
    }

    private fun renderWatchlist(items: List<String>) {
        host.addSectionLabel("WATCHLIST • TICKERE SALVATE")
        if (items.isEmpty()) { host.addCard("WATCHLIST GOALĂ", "Adaugă un ticker din Analysis. Lista este separată de Portofoliu."); return }
        val store = OracleWatchlistStore(host.root.context)
        items.distinct().forEach { t ->
            val row = LinearLayout(host.root.context).apply {
                orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL
                setPadding(host.dp(15), host.dp(12), host.dp(10), host.dp(12))
                background = GradientDrawable().apply { setColor(Color.rgb(7, 12, 23)); cornerRadius = host.dp(14).toFloat(); setStroke(host.dp(1), Color.rgb(45, 70, 105)) }
                isClickable = true
                isFocusable = true
                contentDescription = "$t — deschide în Analysis"
                setOnClickListener { onWatchlistTickerClick(t) }
            }
            row.addView(TextView(host.root.context).apply { text = t; textSize = 19f; typeface = Typeface.DEFAULT_BOLD; setTextColor(Color.WHITE) }, LinearLayout.LayoutParams(0, -2, 1f))
            row.addView(TextView(host.root.context).apply {
                text = "›"; textSize = 25f; typeface = Typeface.DEFAULT_BOLD; setTextColor(host.accent); gravity = Gravity.CENTER
            }, LinearLayout.LayoutParams(host.dp(30), host.dp(38)))
            val delete = TextView(host.root.context).apply {
                text = "ȘTERGE"; textSize = 10f; typeface = Typeface.DEFAULT_BOLD; setTextColor(Color.rgb(255, 90, 90)); setPadding(host.dp(8), host.dp(8), host.dp(8), host.dp(8))
                isClickable = true
                isFocusable = true
                setOnClickListener { val next = store.load().filterNot { it.equals(t, true) }; store.save(next); renderWatchlist(next) }
            }
            row.addView(delete, LinearLayout.LayoutParams(-2, -2))
            host.content.addView(row, LinearLayout.LayoutParams(-1, -2).apply { setMargins(0, 0, 0, host.dp(8)) })
        }
    }

    private fun renderKnowledge(items: List<OracleKnowledgeItem>) {
        host.addCard("KNOWLEDGE", "Biblioteca Oracle — conținut local")
        if (items.isEmpty()) return
        items.sortedByDescending { it.publishedAt }.forEach { addItem(it.title, "${it.category}\n${it.content}") }
    }

    private fun renderActions(actions: List<OracleAction>) {
        host.addCard("ACTIONS", "Motor local de semnale — prioritizare după scor")
        if (actions.isEmpty()) return
        val buys = actions.count { it.action.equals("BUY", true) }
        val sells = actions.count { it.action.equals("SELL", true) }
        host.addCard("SIGNAL SUMMARY", "BUY $buys • HOLD ${actions.size - buys - sells} • SELL $sells\nTotal semnale ${actions.size}")
        actions.sortedByDescending { abs(it.score) }.take(50).forEachIndexed { i, a -> addItem("${i + 1}. ${a.action} • ${a.ticker}", "Scor ${fmt(a.score)}\n${a.reason}") }
    }

    private fun addItem(title: String, body: String) {
        val c = LinearLayout(host.root.context).apply {
            orientation = LinearLayout.VERTICAL; setPadding(host.dp(16), host.dp(13), host.dp(16), host.dp(13))
            background = GradientDrawable().apply { setColor(Color.rgb(6, 10, 20)); cornerRadius = host.dp(14).toFloat(); setStroke(host.dp(1), Color.rgb(34, 43, 65)) }
        }
        val row = LinearLayout(host.root.context).apply { gravity = Gravity.CENTER_VERTICAL }
        row.addView(TextView(host.root.context).apply { text = "◆"; textSize = 9f; setTextColor(host.accent) }, LinearLayout.LayoutParams(host.dp(22), host.dp(22)))
        row.addView(TextView(host.root.context).apply { text = title.uppercase(); textSize = 15f; typeface = Typeface.DEFAULT_BOLD; setTextColor(Color.WHITE) }, LinearLayout.LayoutParams(0, -2, 1f))
        row.addView(TextView(host.root.context).apply { text = "›"; textSize = 24f; setTextColor(host.accent) }, LinearLayout.LayoutParams(host.dp(24), host.dp(30)))
        c.addView(row)
        c.addView(TextView(host.root.context).apply { text = body; textSize = 14f; setTextColor(Color.rgb(175, 183, 201)); setPadding(host.dp(22), host.dp(5), 0, 0) })
        host.content.addView(c, LinearLayout.LayoutParams(-1, -2).apply { setMargins(0, 0, 0, host.dp(9)) })
    }

    private fun fmt(v: Double) = "%.1f".format(Locale.US, v)
    private fun money(v: Double) = "%.2f USD".format(Locale.US, v)
    private fun moneyOrDash(v: Double?) = v?.let { money(it) } ?: "—"
    private fun signed(v: Double) = if (v >= 0) "+${fmt(v)}" else fmt(v)
    private fun factorColor(v: Double) = when { v >= 75 -> Color.rgb(105, 245, 35); v >= 55 -> Color.rgb(255, 210, 55); else -> Color.rgb(255, 90, 90) }
}
