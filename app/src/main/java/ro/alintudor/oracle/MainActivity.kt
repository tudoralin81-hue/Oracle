package ro.alintudor.oracle

import android.app.Activity
import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.os.Looper
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
    private val mainHandler = Handler(Looper.getMainLooper())
    private val titles = linkedMapOf("portfolio" to "PORTFOLIO", "alerts" to "ALERTS", "news" to "NEWS", "growth" to "GROWTH", "knowledge" to "KNOWLEDGE", "analysis" to "ANALYSIS", "watchlist" to "WATCHLIST", "journal" to "JURNAL ACTIVITATE")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        repository = OracleRepository(this)
        window.statusBarColor = Color.rgb(2, 4, 10)
        window.navigationBarColor = Color.rgb(2, 4, 10)
        root = FrameLayout(this).apply { setBackgroundColor(Color.rgb(2, 4, 10)) }
        setContentView(root)

        // The first frame is always the native hub. No network call, refresh or
        // calculation is allowed to block startup or leave a skeleton screen.
        runCatching { showHub() }
            .onFailure { showFatalError("Pornirea Oracle a eșuat", it) }
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
        // Render persisted local data immediately. A slow/failing calculation
        // must never replace the screen with an endless loading state.
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
                    // Keep the already-rendered cached module visible and expose
                    // the actual refresh failure instead of showing fake loading.
                    Toast.makeText(this, "Refresh local eșuat: ${error.message ?: error.javaClass.simpleName}", Toast.LENGTH_LONG).show()
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
        box.addView(Button(this).apply {
            text = "REÎNCEARCĂ"
            setOnClickListener { openModule(key) }
        })
        box.addView(Button(this).apply {
            text = "ÎNAPOI LA ORACLE"
            setOnClickListener { showHub() }
        })
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
