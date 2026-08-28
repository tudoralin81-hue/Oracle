package ro.alintudor.oracle

import android.app.Activity
import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.*
import ro.alintudor.oracle.nativeui.*

class MainActivity : Activity() {
    private lateinit var root: FrameLayout
    private var currentModule: String? = null
    private val titles = linkedMapOf("portfolio" to "PORTFOLIO", "alerts" to "ALERTS", "news" to "NEWS", "growth" to "GROWTH", "knowledge" to "KNOWLEDGE", "analysis" to "ANALYSIS", "watchlist" to "WATCHLIST")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = Color.rgb(2,4,10); window.navigationBarColor = Color.rgb(2,4,10)
        root = FrameLayout(this).apply { setBackgroundColor(Color.rgb(2,4,10)) }
        setContentView(root); showHub()
    }

    private fun showHub() {
        currentModule = null; root.removeAllViews()
        val scroll = ScrollView(this)
        val box = LinearLayout(this).apply { orientation=LinearLayout.VERTICAL; gravity=Gravity.CENTER; setPadding(20,28,20,28) }
        val title = TextView(this).apply { text="ORACLE"; textSize=32f; gravity=Gravity.CENTER; setTextColor(Color.WHITE); setPadding(0,12,0,28) }
        box.addView(title)
        titles.forEach { (key,label) ->
            val b=Button(this).apply { text=label; textSize=18f; isAllCaps=false; setOnClickListener { openModule(key) } }
            box.addView(b, LinearLayout.LayoutParams(-1,64).apply { setMargins(0,0,0,12) })
        }
        scroll.addView(box); root.addView(scroll, FrameLayout.LayoutParams(-1,-1));
    }

    private fun openModule(key: String) {
        currentModule = key; root.removeAllViews()
        val host = OracleNativeModule(this, titles[key] ?: key.uppercase())
        root.addView(host.root, FrameLayout.LayoutParams(-1,-1))
        when (key) {
            "portfolio" -> OraclePortfolioModule(host).render()
            "alerts" -> OracleAlertsModule(host).render()
            "news" -> OracleNewsModule(host).render(emptyList())
            else -> OracleSimpleModule(host, titles[key] ?: key.uppercase()).render()
        }
    }

    override fun onBackPressed() { if (currentModule != null) showHub() else super.onBackPressed() }
}
