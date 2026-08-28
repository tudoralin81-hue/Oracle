package ro.alintudor.oracle

import android.app.Activity
import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import android.widget.*
import ro.alintudor.oracle.core.OracleLocalProcessor
import ro.alintudor.oracle.core.OracleRepository
import ro.alintudor.oracle.core.snapshot
import ro.alintudor.oracle.nativeui.*

class MainActivity : Activity() {
    private lateinit var root: FrameLayout
    private lateinit var repository: OracleRepository
    private var currentModule: String? = null
    private val titles = linkedMapOf("portfolio" to "PORTFOLIO", "alerts" to "ALERTS", "news" to "NEWS", "growth" to "GROWTH", "knowledge" to "KNOWLEDGE", "analysis" to "ANALYSIS", "watchlist" to "WATCHLIST", "journal" to "JURNAL ACTIVITATE")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        repository = OracleRepository(this)
        window.statusBarColor = Color.rgb(2, 4, 10)
        window.navigationBarColor = Color.rgb(2, 4, 10)
        root = FrameLayout(this).apply { setBackgroundColor(Color.rgb(2, 4, 10)) }
        setContentView(root)
        showHub()
    }

    private fun showHub() {
        currentModule = null
        root.removeAllViews()
        val scroll = ScrollView(this)
        val box = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(20, 28, 20, 28)
        }
        val title = TextView(this).apply {
            text = "ORACLE"
            textSize = 32f
            gravity = Gravity.CENTER
            setTextColor(Color.WHITE)
            setPadding(0, 12, 0, 28)
        }
        box.addView(title)
        titles.forEach { (key, label) ->
            val button = Button(this).apply {
                text = label
                textSize = 18f
                isAllCaps = false
                setOnClickListener { openModule(key) }
            }
            box.addView(button, LinearLayout.LayoutParams(-1, 64).apply { setMargins(0, 0, 0, 12) })
        }
        scroll.addView(box)
        root.addView(scroll, FrameLayout.LayoutParams(-1, -1))
    }

    private fun openModule(key: String) {
        currentModule = key
        renderModule(key)
        // Never block the UI with local recalculation. Render persisted data first,
        // then refresh on a worker and replace the module when the new snapshot is ready.
        Thread {
            runCatching { OracleLocalProcessor.refresh(repository) }
                .onSuccess {
                    runOnUiThread {
                        if (currentModule == key && !isFinishing) renderModule(key, refresh = false)
                    }
                }
        }.start()
    }

    private fun renderModule(key: String, refresh: Boolean = false) {
        root.removeAllViews()
        val host = OracleNativeModule(this, titles[key] ?: key.uppercase()) {
            openModule(key)
        }
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

    @Suppress("DEPRECATION")
    override fun onBackPressed() {
        if (currentModule != null) showHub() else super.onBackPressed()
    }
}
