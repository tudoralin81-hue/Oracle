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

// ANALYSIS_RAW_VALUES_V4

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
        // AUTO_WATCHLIST_ANALYZE: a Watchlist navigation opens the actual ticker analysis, not only the input field.
        if (tickerDraft.isNotBlank()) input.postDelayed({ run() }, 220L)
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
        val watchStore = OracleWatchlistStore(host.root.context)
        val watchTicker = r.ticker.trim().uppercase(Locale.US)
        val watchEye = WatchlistEyeView(host.root.context, host.dp(42)).apply {
            tag = "oracle_watchlist_eye_direct"
            isClickable = true
            isFocusable = true
            contentDescription = "Adaugă sau scoate $watchTicker din Watchlist"
            setSelectedState(watchStore.load().any { it.equals(watchTicker, true) })
            setOnClickListener {
                val current = watchStore.load().toMutableList()
                val present = current.any { it.equals(watchTicker, true) }
                if (present) current.removeAll { it.equals(watchTicker, true) } else current.add(watchTicker)
                watchStore.save(current)
                setSelectedState(!present)
                Toast.makeText(host.root.context, if (!present) "$watchTicker adăugat în Watchlist" else "$watchTicker scos din Watchlist", Toast.LENGTH_SHORT).show()
            }
        }
        headline.addView(watchEye, LinearLayout.LayoutParams(host.dp(42), host.dp(42)).apply { setMargins(host.dp(4), 0, host.dp(8), 0) })
        headline.addView(TextView(host.root.context).apply {
            text = money(r.price)
            textSize = 17f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(Color.rgb(45, 232, 92))
            gravity = Gravity.END
        })
        top.addView(headline)
        top.addView(TextView(host.root.context).apply {
            text = companyName(r.ticker)
            textSize = 15f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(Color.rgb(205, 213, 228))
            setPadding(0, host.dp(4), 0, 0)
        })
        top.addView(TextView(host.root.context).apply {
            text = "Sector: ${r.sector ?: "Sector indisponibil"}"
            textSize = 13.5f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(Color.rgb(145, 158, 180))
            setPadding(0, host.dp(2), 0, 0)
        })
        host.content.addView(top, LinearLayout.LayoutParams(-1, -2).apply { setMargins(0, 0, 0, host.dp(10)) })

        // ANALYSIS_PARAMETERS_V8
        // All market-relevant values are presented in one two-column matrix:
        // Oracle factors + supplementary technical indicators + fundamentals.
        host.addSectionLabel("PARAMETRII BURSIERI RELEVANȚI")
        val relevantGrid = LinearLayout(host.root.context).apply { orientation = LinearLayout.VERTICAL }
        val f = r.fundamentals
        val relevantParameters = mutableListOf<Pair<String, String>>()

        // Oracle factors: rawValues[0] is internal News; visible factors start at rawValues[1].
        OracleAnalysisEngine.factorNames.forEachIndexed { i, name ->
            relevantParameters.add(name to (r.rawValues.getOrNull(i + 1) ?: "Valoare indisponibilă"))
        }

        // Supplementary technical indicators.
        relevantParameters.add("RSI (14)" to fmt(r.rsi))
        relevantParameters.add("MACD (12/26)" to metricPair(r.macd, r.macdSignal))
        relevantParameters.add("52W HIGH / LOW" to "${moneyOrDash(r.week52High)} / ${moneyOrDash(r.week52Low)}")
        relevantParameters.add("ATR" to "${money(r.atrValue)}  •  ${fmt(r.atrPct)}%")

        // Fundamentals — kept in the same matrix, not in a separate section.
        relevantParameters.add("Sector" to (f?.sector ?: r.sector ?: "—"))
        relevantParameters.add("Industry" to (f?.industry ?: "—"))
        relevantParameters.add("P/E" to num2(f?.trailingPe))
        relevantParameters.add("Fwd P/E" to num2(f?.forwardPe))
        relevantParameters.add("P/B" to num2(f?.priceToBook))
        relevantParameters.add("Revenue growth (YoY)" to pctFund(f?.revenueGrowth))
        relevantParameters.add("Earnings growth" to pctFund(f?.earningsGrowth))
        relevantParameters.add("Net margin" to pctFund(f?.profitMargin))
        relevantParameters.add("Operating margin" to pctFund(f?.operatingMargin))
        relevantParameters.add("ROE" to pctFund(f?.returnOnEquity))
        relevantParameters.add("D/E" to num2(f?.debtToEquity))
        relevantParameters.add("Current ratio" to num2(f?.currentRatio))
        relevantParameters.add("Quick ratio" to num2(f?.quickRatio))
        relevantParameters.add("Beta" to num2(f?.beta))
        relevantParameters.add("Market cap" to capText(f?.marketCap))

        addMetricGrid(relevantGrid, relevantParameters)
        host.content.addView(relevantGrid, LinearLayout.LayoutParams(-1, -2).apply {
            setMargins(0, 0, 0, host.dp(10))
        })

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

// BUILD_VERSION_BOTTOM_V1
host.content.addView(TextView(host.root.context).apply {
    text = "ORACLE • V6g-FINAL-B513"
    textSize = 10f
    typeface = Typeface.DEFAULT_BOLD
    letterSpacing = .08f
    gravity = Gravity.CENTER
    setTextColor(Color.rgb(110, 120, 140))
    setPadding(0, host.dp(6), 0, host.dp(18))
}, LinearLayout.LayoutParams(-1, -2))
    }

    private fun addMetricGrid(container: LinearLayout, items: List<Pair<String, String>>) {
        var row: LinearLayout? = null
        items.forEachIndexed { index, item ->
            if (index % 2 == 0) {
                row = LinearLayout(host.root.context).apply {
                    orientation = LinearLayout.HORIZONTAL
                    gravity = Gravity.FILL_VERTICAL
                    setMeasureWithLargestChildEnabled(true)
                }
                container.addView(row, LinearLayout.LayoutParams(-1, -2).apply {
                    setMargins(0, 0, 0, host.dp(6))
                })
            }

            val card = LinearLayout(host.root.context).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(host.dp(11), host.dp(8), host.dp(11), host.dp(8))
                background = GradientDrawable().apply {
                    setColor(Color.rgb(6, 12, 24))
                    cornerRadius = host.dp(12).toFloat()
                    setStroke(host.dp(1), Color.rgb(35, 65, 98))
                }
            }
            card.addView(TextView(host.root.context).apply {
                text = item.first.uppercase(Locale.US)
                textSize = 10f
                typeface = Typeface.DEFAULT_BOLD
                letterSpacing = .07f
                setTextColor(Color.rgb(85, 190, 235))
                includeFontPadding = true
            })
            card.addView(TextView(host.root.context).apply {
                text = item.second
                textSize = 12.5f
                typeface = Typeface.DEFAULT_BOLD
                setTextColor(metricValueColor(item.first, item.second))
                setPadding(0, host.dp(2), 0, 0)
                includeFontPadding = true
                setHorizontallyScrolling(false)
                maxLines = Int.MAX_VALUE
                ellipsize = null
            })
            row?.addView(card, LinearLayout.LayoutParams(0, -2, 1f).apply {
                if (index % 2 == 1) setMargins(host.dp(4), 0, 0, 0)
                else setMargins(0, 0, host.dp(4), 0)
            })
        }

        // Equalize each row after Android has measured multiline content.
        container.post {
            for (i in 0 until container.childCount) {
                val rv = container.getChildAt(i) as? LinearLayout ?: continue
                var maxHeight = 0
                for (j in 0 until rv.childCount) {
                    maxHeight = maxOf(maxHeight, rv.getChildAt(j).measuredHeight)
                }
                if (maxHeight > 0) {
                    for (j in 0 until rv.childCount) {
                        val child = rv.getChildAt(j)
                        val lp = child.layoutParams
                        if (lp.height != maxHeight) {
                            lp.height = maxHeight
                            child.layoutParams = lp
                        }
                    }
                }
            }
            container.requestLayout()
        }
    }

    private fun metricValueColor(label: String, value: String): Int {
        val l = label.uppercase(Locale.US)
        val v = value.uppercase(Locale.US)
        if (value == "—" || value.contains("INDISPONIBILĂ") || value.contains("INDISPONIBILĂ")) return Color.rgb(205, 165, 38)

        fun numberAfter(token: String): Double? {
            val m = Regex(Regex.escape(token) + "\\s*(-?\\d+(?:[.,]\\d+)?)", RegexOption.IGNORE_CASE).find(value) ?: return null
            return m.groupValues[1].replace(',', '.').toDoubleOrNull()
        }
        fun firstNumber(): Double? = Regex("-?\\d+(?:[.,]\\d+)?").find(value)?.value?.replace(',', '.')?.toDoubleOrNull()
        fun pctNumber(): Double? = firstNumber()

        return when {
            l == "SECTOR" || l == "INDUSTRY" -> Color.rgb(50, 220, 135)
            l == "BREAKOUT" -> if (v.contains("BREAKOUT: DA")) Color.rgb(50, 220, 135) else Color.rgb(205, 165, 38)
            l == "TREND" -> {
                val p = numberAfter("Preț"); val s50 = numberAfter("SMA50"); val s200 = numberAfter("SMA200")
                when {
                    p != null && s50 != null && s200 != null && p >= s50 && p >= s200 -> Color.rgb(50, 220, 135)
                    p != null && s50 != null && s200 != null && p < s50 && p < s200 -> Color.rgb(244, 67, 54)
                    else -> Color.rgb(205, 165, 38)
                }
            }
            l == "MOMENTUM" -> {
                val nums = Regex("-?\\d+(?:[.,]\\d+)?").findAll(value).mapNotNull { it.value.replace(',', '.').toDoubleOrNull() }.toList()
                when {
                    nums.size >= 2 && nums[0] > 0 && nums[1] > 0 -> Color.rgb(50, 220, 135)
                    nums.size >= 2 && nums[0] < 0 && nums[1] < 0 -> Color.rgb(244, 67, 54)
                    else -> Color.rgb(205, 165, 38)
                }
            }
            l == "VOLUME" -> {
                val n = firstNumber()
                when { n == null -> Color.rgb(205, 165, 38); n in 0.8..1.8 -> Color.rgb(50, 220, 135); n < 0.8 -> Color.rgb(205, 165, 38); else -> Color.rgb(205, 165, 38) }
            }
            l == "SUPPORT / RESISTANCE" -> Color.rgb(205, 165, 38)
            l == "BOLLINGER" -> {
                val pos = numberAfter("Poziție")
                when { pos == null -> Color.rgb(205, 165, 38); pos in -20.0..20.0 -> Color.rgb(50, 220, 135); pos < -20.0 -> Color.rgb(244, 67, 54); else -> Color.rgb(205, 165, 38) }
            }
            l == "ICHIMOKU" -> if (v.contains("BULLISH")) Color.rgb(50, 220, 135) else Color.rgb(244, 67, 54)
            l == "MARKET / SECTOR" -> Color.rgb(50, 220, 135)
            l == "RISK / REWARD" -> {
                val n = firstNumber()
                when { n == null -> Color.rgb(205, 165, 38); n <= 5.0 -> Color.rgb(50, 220, 135); n <= 8.0 -> Color.rgb(205, 165, 38); else -> Color.rgb(244, 67, 54) }
            }
            l == "ADX" -> {
                val n = firstNumber()
                when { n == null -> Color.rgb(205, 165, 38); n >= 20.0 -> Color.rgb(50, 220, 135); else -> Color.rgb(205, 165, 38) }
            }
            l == "RSI (14)" -> {
                val n = firstNumber()
                when { n == null -> Color.rgb(205, 165, 38); n in 30.0..70.0 -> Color.rgb(50, 220, 135); n < 30.0 -> Color.rgb(205, 165, 38); else -> Color.rgb(244, 67, 54) }
            }
            l == "MACD (12/26)" -> {
                val nums = Regex("-?\\d+(?:[.,]\\d+)?").findAll(value).mapNotNull { it.value.replace(',', '.').toDoubleOrNull() }.toList()
                when { nums.size >= 2 && nums[0] > nums[1] -> Color.rgb(50, 220, 135); nums.size >= 2 && nums[0] < nums[1] -> Color.rgb(244, 67, 54); else -> Color.rgb(205, 165, 38) }
            }
            l == "ATR" -> {
                val n = Regex("(-?\\d+(?:[.,]\\d+)?)%", RegexOption.IGNORE_CASE).find(value)?.groupValues?.get(1)?.replace(',', '.')?.toDoubleOrNull()
                when { n == null -> Color.rgb(205, 165, 38); n in 2.0..6.0 -> Color.rgb(50, 220, 135); n > 6.0 -> Color.rgb(244, 67, 54); else -> Color.rgb(205, 165, 38) }
            }
            l == "52W HIGH / LOW" -> Color.rgb(205, 165, 38)
            l == "P/E" || l == "FWD P/E" -> {
                val n = firstNumber()
                when { n == null -> Color.rgb(205, 165, 38); n in 10.0..30.0 -> Color.rgb(50, 220, 135); n < 10.0 -> Color.rgb(205, 165, 38); else -> Color.rgb(244, 67, 54) }
            }
            l == "P/B" -> {
                val n = firstNumber()
                when { n == null -> Color.rgb(205, 165, 38); n in 1.0..5.0 -> Color.rgb(50, 220, 135); n < 1.0 -> Color.rgb(205, 165, 38); else -> Color.rgb(244, 67, 54) }
            }
            l.startsWith("REVENUE GROWTH") -> {
                val n = pctNumber()
                when { n == null -> Color.rgb(205, 165, 38); n >= 10.0 -> Color.rgb(50, 220, 135); n >= 0.0 -> Color.rgb(205, 165, 38); else -> Color.rgb(244, 67, 54) }
            }
            l == "EARNINGS GROWTH" -> {
                val n = pctNumber()
                when { n == null -> Color.rgb(205, 165, 38); n >= 10.0 -> Color.rgb(50, 220, 135); n >= 0.0 -> Color.rgb(205, 165, 38); else -> Color.rgb(244, 67, 54) }
            }
            l == "NET MARGIN" || l == "OPERATING MARGIN" -> {
                val n = pctNumber()
                when { n == null -> Color.rgb(205, 165, 38); n >= 10.0 -> Color.rgb(50, 220, 135); n >= 0.0 -> Color.rgb(205, 165, 38); else -> Color.rgb(244, 67, 54) }
            }
            l == "ROE" -> {
                val n = pctNumber()
                when { n == null -> Color.rgb(205, 165, 38); n >= 15.0 -> Color.rgb(50, 220, 135); n >= 0.0 -> Color.rgb(205, 165, 38); else -> Color.rgb(244, 67, 54) }
            }
            l == "D/E" -> {
                val n = firstNumber()
                when { n == null -> Color.rgb(205, 165, 38); n <= 1.0 -> Color.rgb(50, 220, 135); n <= 2.0 -> Color.rgb(205, 165, 38); else -> Color.rgb(244, 67, 54) }
            }
            l == "CURRENT RATIO" -> {
                val n = firstNumber()
                when { n == null -> Color.rgb(205, 165, 38); n in 1.5..3.0 -> Color.rgb(50, 220, 135); n >= 1.0 -> Color.rgb(205, 165, 38); else -> Color.rgb(244, 67, 54) }
            }
            l == "QUICK RATIO" -> {
                val n = firstNumber()
                when { n == null -> Color.rgb(205, 165, 38); n in 1.0..2.0 -> Color.rgb(50, 220, 135); n >= 0.7 -> Color.rgb(205, 165, 38); else -> Color.rgb(244, 67, 54) }
            }
            l == "BETA" -> {
                val n = firstNumber()
                when { n == null -> Color.rgb(205, 165, 38); n in 0.8..1.5 -> Color.rgb(50, 220, 135); n > 1.5 -> Color.rgb(244, 67, 54); else -> Color.rgb(205, 165, 38) }
            }
            l == "MARKET CAP" -> Color.rgb(50, 220, 135)
            else -> Color.rgb(205, 165, 38)
        }
    }
    private fun metricPair(value: Double?, signal: Double?): String = "${num2(value)}  •  SIG ${num2(signal)}"
    private fun num2(value: Double?): String = value?.let { "%.2f".format(Locale.US, it) } ?: "—"
    private fun pctFund(value: Double?): String = value?.let { "%.2f%%".format(Locale.US, it * 100.0) } ?: "—"
    private fun capText(value: Double?): String = when { value == null -> "—"; value >= 1e12 -> "%.2fT".format(Locale.US, value / 1e12); value >= 1e9 -> "%.2fB".format(Locale.US, value / 1e9); value >= 1e6 -> "%.2fM".format(Locale.US, value / 1e6); else -> "%.0f".format(Locale.US, value) }

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
        "APLD" -> "Applied Digital Corporation"
        "NVDA" -> "NVIDIA Corporation"; "AAPL" -> "Apple Inc."; "MSFT" -> "Microsoft Corporation"; "AMZN" -> "Amazon.com, Inc."; "GOOGL" -> "Alphabet Inc."; "META" -> "Meta Platforms, Inc."; "TSLA" -> "Tesla, Inc."; "AMD" -> "Advanced Micro Devices, Inc."; "AVGO" -> "Broadcom Inc."; "NFLX" -> "Netflix, Inc."; else -> t
    }

    private fun renderWatchlist(items: List<String>) {
        host.content.removeAllViews()
        host.addSectionLabel("WATCHLIST • TICKERE SALVATE")
        if (items.isEmpty()) {
            host.addCard("WATCHLIST GOALĂ", "Adaugă un ticker din Analysis. Lista este separată de Portofoliu.")
            return
        }

        val store = OracleWatchlistStore(host.root.context)
        items.map { it.trim().uppercase(Locale.US) }
            .filter { it.isNotBlank() }
            .distinct()
            .forEach { ticker ->
                val row = LinearLayout(host.root.context).apply {
                    orientation = LinearLayout.HORIZONTAL
                    gravity = Gravity.CENTER_VERTICAL
                    setPadding(host.dp(8), host.dp(8), host.dp(8), host.dp(8))
                    background = GradientDrawable().apply {
                        setColor(Color.rgb(7, 12, 23))
                        cornerRadius = host.dp(14).toFloat()
                        setStroke(host.dp(1), Color.rgb(45, 70, 105))
                    }
                }

                // Real Android Button: this is deliberately the primary navigation control.
                // Every saved ticker gets its own independent clickable control.
                val tickerButton = Button(host.root.context).apply {
                    text = ticker
                    textSize = 20f
                    typeface = Typeface.DEFAULT_BOLD
                    setTextColor(Color.WHITE)
                    gravity = Gravity.CENTER_VERTICAL or Gravity.START
                    setPadding(host.dp(10), 0, host.dp(4), 0)
                    background = android.graphics.drawable.ColorDrawable(Color.TRANSPARENT)
                    isAllCaps = false
                    minHeight = 0
                    minimumHeight = 0
                    contentDescription = "Deschide $ticker în Analysis"
                    setOnClickListener {
                        onWatchlistTickerClick(ticker)
                    }
                }
                row.addView(tickerButton, LinearLayout.LayoutParams(0, host.dp(84), 1f))

                val openButton = Button(host.root.context).apply {
                    text = "›"
                    textSize = 30f
                    typeface = Typeface.DEFAULT_BOLD
                    setTextColor(host.accent)
                    gravity = Gravity.CENTER
                    setPadding(0, 0, 0, 0)
                    background = android.graphics.drawable.ColorDrawable(Color.TRANSPARENT)
                    minHeight = 0
                    minimumHeight = 0
                    contentDescription = "Deschide $ticker în Analysis"
                    setOnClickListener {
                        onWatchlistTickerClick(ticker)
                    }
                }
                row.addView(openButton, LinearLayout.LayoutParams(host.dp(54), host.dp(84)))

                val deleteButton = Button(host.root.context).apply {
                    text = "ȘTERGE"
                    textSize = 14f
                    typeface = Typeface.DEFAULT_BOLD
                    setTextColor(Color.rgb(255, 105, 105))
                    gravity = Gravity.CENTER
                    setPadding(0, 0, 0, 0)
                    background = android.graphics.drawable.ColorDrawable(Color.TRANSPARENT)
                    minHeight = 0
                    minimumHeight = 0
                    contentDescription = "Șterge $ticker din Watchlist"
                    setOnClickListener {
                        val current = store.load().toMutableList()
                        current.removeAll { it.equals(ticker, true) }
                        store.save(current)
                        renderWatchlist(store.load())
                    }
                }
                row.addView(deleteButton, LinearLayout.LayoutParams(host.dp(112), host.dp(84)))

                host.content.addView(row, LinearLayout.LayoutParams(-1, host.dp(100)).apply {
                    setMargins(0, 0, 0, host.dp(12))
                })
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
    private class WatchlistEyeView(context: android.content.Context, private val sizePx: Int) : android.view.View(context) {
        private val paint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
            style = android.graphics.Paint.Style.STROKE
            strokeWidth = (sizePx * 0.055f).coerceAtLeast(2f)
            strokeCap = android.graphics.Paint.Cap.ROUND
            strokeJoin = android.graphics.Paint.Join.ROUND
        }
        private var selected = false

        fun setSelectedState(value: Boolean) {
            selected = value
            paint.color = if (selected) Color.rgb(255, 210, 45) else Color.rgb(125, 135, 155)
            invalidate()
        }

        override fun onDraw(canvas: android.graphics.Canvas) {
            super.onDraw(canvas)
            val cx = width / 2f
            val cy = height / 2f
            val rx = width * 0.32f
            val ry = height * 0.22f
            canvas.drawOval(cx - rx, cy - ry, cx + rx, cy + ry, paint)
            canvas.drawCircle(cx, cy, width * 0.105f, paint)
            if (selected) {
                paint.style = android.graphics.Paint.Style.FILL
                canvas.drawCircle(cx, cy, width * 0.052f, paint)
                paint.style = android.graphics.Paint.Style.STROKE
            }
        }
    }

}
