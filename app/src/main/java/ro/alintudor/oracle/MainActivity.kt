package ro.alintudor.oracle

import android.app.Activity
import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.*
import ro.alintudor.oracle.core.OracleBootstrap
import ro.alintudor.oracle.core.OracleGrowthLiveData
import ro.alintudor.oracle.core.OracleLocalProcessor
import ro.alintudor.oracle.core.OracleRepository
import ro.alintudor.oracle.core.OracleWatchlistStore
import ro.alintudor.oracle.core.OracleKnowledgeSync
import ro.alintudor.oracle.core.snapshot
import ro.alintudor.oracle.nativeui.*

class MainActivity : Activity() {
    private lateinit var root: FrameLayout
    private lateinit var repository: OracleRepository
    private var currentModule: String? = null
    private val mainHandler = Handler(Looper.getMainLooper())
    private val titles = linkedMapOf("portfolio" to "PORTFOLIO", "alerts" to "ALERTS", "news" to "NEWS", "growth" to "GROWTH", "knowledge" to "KNOWLEDGE", "analysis" to "ANALYSIS", "watchlist" to "WATCHLIST", "journal" to "JURNAL ACTIVITATE")
    private val subtitles = mapOf("portfolio" to "Poziții, P/L și alocare", "alerts" to "Semnale și alerte active", "news" to "Știri și evenimente relevante", "growth" to "Randament, trend local și contribuție", "knowledge" to "Idei, explicații și documentație", "analysis" to "Analiză și decizii Oracle", "watchlist" to "Acțiuni urmărite și oportunități", "journal" to "Istoric complet al activității")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        repository = OracleRepository(this)
        window.statusBarColor = Color.rgb(1,3,8); window.navigationBarColor = Color.rgb(1,3,8)
        root = FrameLayout(this).apply { setBackgroundColor(Color.rgb(1,3,8)) }
        setContentView(root)
        OracleKnowledgeSync.scheduleDaily(this)
        runCatching { OracleBootstrap.ensure(repository); showHub() }.onFailure { showFatalError("Pornirea Oracle a eșuat",it) }
    }

    private fun showHub() {
        currentModule=null
        root.removeAllViews()
        root.addView(PremiumStartView(this){ openModule(it) }, FrameLayout.LayoutParams(-1,-1))
    }

    private fun dp(v:Int)= (v*resources.displayMetrics.density).toInt()

    private fun openModule(key:String){
        currentModule=key
        runCatching{renderModule(key,false)}.onFailure{showModuleError(key,it)}
        if (key == "analysis") return
        if (key == "knowledge") {
            if (OracleKnowledgeSync.isStale(this)) {
                OracleKnowledgeSync.refreshAsync(this) { ok, error ->
                    if (currentModule != "knowledge" || isFinishing) return@refreshAsync
                    if (ok) runCatching { renderModule("knowledge", false) }.onFailure { showModuleError("knowledge", it) }
                    else if (error != null) Toast.makeText(this, "Knowledge refresh eșuat: $error", Toast.LENGTH_LONG).show()
                }
            }
            return
        }
    }

    private fun showFatalError(title:String, error:Throwable) {
        root.removeAllViews()
        val text=TextView(this).apply { setTextColor(Color.WHITE); textSize=16f; text="$title\n\n${error.message ?: error.javaClass.simpleName}"; setPadding(dp(24),dp(40),dp(24),dp(24)) }
        root.addView(ScrollView(this).apply { addView(text) })
    }

    private fun showModuleError(key:String, error:Throwable) {
        Toast.makeText(this, "${titles[key] ?: key}: ${error.message ?: "eroare"}", Toast.LENGTH_LONG).show()
    }
}
