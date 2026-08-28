package ro.alintudor.oracle

import android.app.Activity
import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import android.widget.*
import ro.alintudor.oracle.core.OracleRepository
import ro.alintudor.oracle.core.snapshot
import ro.alintudor.oracle.nativeui.*

class MainActivity : Activity() {
    private lateinit var root: FrameLayout
    private lateinit var repository: OracleRepository
    private var currentModule: String? = null
    private val titles = linkedMapOf("portfolio" to "PORTFOLIO", "alerts" to "ALERTS", "news" to "NEWS", "growth" to "GROWTH", "knowledge" to "KNOWLEDGE", "analysis" to "ANALYSIS", "watchlist" to "WATCHLIST")

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
            alpha = 0f
        }
        box.addView(title)
        title.animate().alpha(1f).setDuration(500).start()
        titles.forEach { (key, label) ->
            val button = Button(this).apply {
                text = label
                textSize = 18f
                isAllCaps = false
                alpha = 0f
                setOnClickListener { openModule(key) }
            }
            box.addView(button, LinearLayout.LayoutParams(-1, 64).apply { setMargins(0, 0, 0, 12) })
            button.animate().alpha(1f).setDuration(350).setStartDelay((box.childCount * 60).toLong()).start()
        }
        scroll.addView(box)
        root.addView(scroll, FrameLayout.LayoutParams(-1, -1))
    }

    private fun openModule(key: String) {
        currentModule = key
        root.removeAllViews()
        val host = OracleNativeModule(this, titles[key] ?: key.uppercase())
        root.addView(host.root, FrameLayout.LayoutParams(-1, -1))
        val data = repository.snapshot()
        when (key) {
            "portfolio" -> OraclePortfolioModule(host).render(data.positions)
            "alerts" -> OracleAlertsModule(host).render(data.alerts)
            "news" -> OracleNewsModule(host).render(data.news)
            "growth", "analysis", "watchlist", "knowledge" ->
                OracleSimpleModule(host, titles[key] ?: key.uppercase()).render(
                    actions = data.actions,
                    knowledge = data.knowledge,
                    positions = data.positions
                )
        }
    }

    @Suppress("DEPRECATION")
    override fun onBackPressed() {
        if (currentModule != null) showHub() else super.onBackPressed()
    }
}
