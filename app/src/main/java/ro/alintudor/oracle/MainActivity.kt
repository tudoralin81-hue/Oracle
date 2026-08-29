package ro.alintudor.oracle

import android.app.Activity
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.View
import android.widget.*
import ro.alintudor.oracle.core.OracleBootstrap
import ro.alintudor.oracle.core.OracleLocalProcessor
import ro.alintudor.oracle.core.OracleRepository
import ro.alintudor.oracle.core.snapshot
import ro.alintudor.oracle.nativeui.*

class MainActivity : Activity() {
    private lateinit var root: FrameLayout
    private lateinit var repository: OracleRepository
    private var currentModule: String? = null
    private val mainHandler = Handler(Looper.getMainLooper())

    private val titles = linkedMapOf(
        "portfolio" to "PORTFOLIO",
        "alerts" to "ALERTS",
        "news" to "NEWS",
        "growth" to "GROWTH",
        "knowledge" to "KNOWLEDGE",
        "analysis" to "ANALYSIS",
        "watchlist" to "WATCHLIST",
        "journal" to "JURNAL ACTIVITATE"
    )

    private val subtitles = mapOf(
        "portfolio" to "Poziții, P/L și alocare",
        "alerts" to "Semnale și alerte active",
        "news" to "Știri și evenimente relevante",
        "growth" to "Randament, trend local și contribuție",
        "knowledge" to "Idei, explicații și documentație",
        "analysis" to "Analiză și decizii Oracle",
        "watchlist" to "Acțiuni urmărite și oportunități",
        "journal" to "Istoric complet al activității"
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        repository = OracleRepository(this)
        window.statusBarColor = Color.rgb(2, 4, 10)
        window.navigationBarColor = Color.rgb(2, 4, 10)
        root = FrameLayout(this).apply { setBackgroundColor(Color.rgb(2, 4, 10)) }
        setContentView(root)

        runCatching { OracleBootstrap.ensure(repository); showHub() }
            .onFailure { showFatalError("Pornirea Oracle a eșuat", it) }
    }

    private fun showHub() {
        currentModule = null
        root.removeAllViews()

        val scroll = ScrollView(this).apply {
            isFillViewport = true
            setBackgroundColor(Color.rgb(2, 4, 10))
        }
        val page = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(18), dp(18), dp(18), dp(28))
        }

        val header = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(6), dp(8), dp(6), dp(18))
        }
        header.addView(TextView(this).apply {
            text = "ORACLE"
            textSize = 34f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(Color.WHITE)
            letterSpacing = 0.08f
        })
        header.addView(TextView(this).apply {
            text = "PORTFOLIO INTELLIGENCE"
            textSize = 13f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(Color.rgb(145, 155, 175))
            letterSpacing = 0.12f
            setPadding(0, dp(5), 0, 0)
        })
        page.addView(header)

        val status = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(14), dp(12), dp(14), dp(12))
            setBackgroundColor(Color.rgb(9, 13, 26))
        }
        status.addView(View(this).apply {
            setBackgroundColor(Color.rgb(50, 210, 130))
        }, LinearLayout.LayoutParams(dp(8), dp(8)))
        status.addView(TextView(this).apply {
            text = "  ORACLE READY"
            textSize = 13f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(Color.WHITE)
        }, LinearLayout.LayoutParams(0, -2, 1f))
        status.addView(TextView(this).apply {
            text = "LOCAL DATA"
            textSize = 11f
            setTextColor(Color.rgb(145, 155, 175))
        })
        val statusLp = LinearLayout.LayoutParams(-1, -2)
        statusLp.setMargins(0, 0, 0, dp(18))
        page.addView(status, statusLp)

        val grid = GridLayout(this).apply {
            columnCount = if (resources.configuration.screenWidthDp >= 600) 2 else 1
            useDefaultMargins = false
            alignmentMode = GridLayout.ALIGN_BOUNDS
        }

        titles.entries.forEachIndexed { index, (key, label) ->
            val card = makeHomeCard(index + 1, label, subtitles[key] ?: "", key)
            val columns = grid.columnCount
            val row = index / columns
            val col = index % columns
            val lp = GridLayout.LayoutParams(
                GridLayout.spec(row),
                GridLayout.spec(col, 1f)
            )
            lp.width = 0
            lp.height = dp(108)
            if (columns == 1) {
                lp.columnSpec = GridLayout.spec(col, 1f)
            }
            lp.setMargins(if (col == 0) 0 else dp(6), dp(6), if (col == columns - 1) 0 else dp(6), dp(6))
            grid.addView(card, lp)
        }

        page.addView(grid)
        page.addView(TextView(this).apply {
            text = "Atinge orice modul pentru a-l deschide"
            textSize = 12f
            gravity = Gravity.CENTER
            setTextColor(Color.rgb(105, 115, 135))
            setPadding(0, dp(18), 0, 0)
        })

        scroll.addView(page)
        root.addView(scroll, FrameLayout.LayoutParams(-1, -1))
    }

    private fun makeHomeCard(number: Int, label: String, description: String, key: String): LinearLayout {
        val card = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), dp(14), dp(16), dp(12))
            setBackgroundColor(Color.rgb(9, 13, 26))
            isClickable = true
            isFocusable = true
            elevation = dp(2).toFloat()
            setOnClickListener { openModule(key) }
        }

        val top = LinearLayout(this).apply { gravity = Gravity.CENTER_VERTICAL }
        top.addView(TextView(this).apply {
            text = "%02d".format(number)
            textSize = 11f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(Color.rgb(100, 150, 230))
        }, LinearLayout.LayoutParams(0, -2, 1f))
        top.addView(TextView(this).apply {
            text = "›"
            textSize = 25f
            setTextColor(Color.rgb(125, 135, 155))
            gravity = Gravity.CENTER
        }, LinearLayout.LayoutParams(dp(22), dp(28)))
        card.addView(top)

        card.addView(TextView(this).apply {
            text = label
            textSize = 19f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(Color.WHITE)
            setPadding(0, dp(4), 0, 0)
        })
        card.addView(TextView(this).apply {
            text = description
            textSize = 13f
            setTextColor(Color.rgb(175, 180, 195))
            setPadding(0, dp(3), 0, 0)
        })
        return card
    }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()

    private fun openModule(key: String) {
        currentModule = key
        runCatching { renderModule(key, refresh = false) }
            .onFailure { showModuleError(key, it) }

        Thread {
            val result = runCatching { OracleLocalProcessor.refresh(repository) }
            mainHandler.post {
                if (currentModule != key || isFinishing) return@post
                result.onSuccess {
                    runCatching { renderModule(key, refresh = false) }
                        .onFailure { showModuleError(key, it) }
                }.onFailure { error ->
                    Toast.makeText(this, "Refresh local eșuat: ${error.message ?: error.javaClass.simpleName}", Toast.LENGTH_LONG).show()
                }
            }
        }.start()
    }

    private fun renderModule(key: String, refresh: Boolean = false) {
        root.removeAllViews()
        val host = OracleNativeModule(this, titles[key] ?: key.uppercase()) { openModule(key) }
        root.addView(host.root, FrameLayout.LayoutParams(-1, -1))
        val data = if (refresh) OracleLocalProcessor.refresh(repository) else repository.snapshot()
        when (key) {
            "portfolio" -> OraclePortfolioModule(host).render(data.positions)
            "alerts" -> OracleAlertsModule(host).render(data.alerts)
            "news" -> OracleNewsModule(host).render(data.news)
            "journal" -> OracleJournalModule(host).render(data.journal, data.history, data.alerts)
            "growth", "analysis", "watchlist", "knowledge" ->
                OracleSimpleModule(host, titles[key] ?: key.uppercase()).render(
                    actions = data.actions,
                    knowledge = data.knowledge,
                    positions = data.positions,
                    history = data.history
                )
        }
    }

    private fun showModuleError(key: String, error: Throwable) {
        root.removeAllViews()
        val box = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(32, 32, 32, 32)
            setBackgroundColor(Color.rgb(2, 4, 10))
        }
        box.addView(TextView(this).apply {
            text = "ORACLE  •  ${titles[key] ?: key.uppercase()}"
            textSize = 22f
            gravity = Gravity.CENTER
            setTextColor(Color.WHITE)
        })
        box.addView(TextView(this).apply {
            text = "Modulul nu s-a putut încărca.\n\n${error.message ?: error.javaClass.simpleName}"
            textSize = 16f
            gravity = Gravity.CENTER
            setTextColor(Color.LTGRAY)
            setPadding(0, 24, 0, 24)
        })
        box.addView(Button(this).apply { text = "REÎNCEARCĂ"; setOnClickListener { openModule(key) } })
        box.addView(Button(this).apply { text = "ÎNAPOI LA ORACLE"; setOnClickListener { showHub() } })
        root.addView(box, FrameLayout.LayoutParams(-1, -1))
    }

    private fun showFatalError(title: String, error: Throwable) {
        root.removeAllViews()
        val text = TextView(this).apply {
            text = "$title\n\n${error.message ?: error.javaClass.simpleName}\n\nAplicația nu va rămâne blocată pe loading."
            textSize = 17f
            gravity = Gravity.CENTER
            setTextColor(Color.WHITE)
            setPadding(32, 32, 32, 32)
        }
        root.addView(text, FrameLayout.LayoutParams(-1, -1))
    }

    @Suppress("DEPRECATION")
    override fun onBackPressed() {
        if (currentModule != null) showHub() else super.onBackPressed()
    }
}
