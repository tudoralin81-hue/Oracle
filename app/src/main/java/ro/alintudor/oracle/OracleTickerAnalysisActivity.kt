package ro.alintudor.oracle

import android.app.Activity
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.view.Gravity
import android.widget.*
import ro.alintudor.oracle.core.OracleWatchlistStore

/** Ticker Analysis destination opened from Watchlist. */
class OracleTickerAnalysisActivity : Activity() {
    private val bg = Color.rgb(1,3,8)
    private val cyan = Color.rgb(25,205,255)
    private val yellow = Color.rgb(255,210,45)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val ticker = intent.getStringExtra("ticker")?.trim()?.uppercase().orEmpty()
        val store = OracleWatchlistStore(this)
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(12), dp(20), dp(24))
            setBackgroundColor(bg)
        }
        val header = LinearLayout(this).apply { gravity = Gravity.CENTER_VERTICAL }
        header.addView(Button(this).apply {
            text = "‹"; textSize = 28f; setTextColor(Color.WHITE)
            setBackgroundColor(Color.TRANSPARENT); setOnClickListener { finish() }
        }, LinearLayout.LayoutParams(dp(52), dp(52)))
        header.addView(TextView(this).apply {
            text = "ORACLE\nANALYSIS"; gravity = Gravity.CENTER; textSize = 18f
            typeface = Typeface.DEFAULT_BOLD; setTextColor(Color.WHITE)
        }, LinearLayout.LayoutParams(0, dp(58), 1f))
        root.addView(header)
        root.addView(android.view.View(this).apply { setBackgroundColor(cyan) }, LinearLayout.LayoutParams(-1, dp(1)))
        root.addView(TextView(this).apply {
            text = "ANALYSIS • SINGLE TICKER"; textSize = 16f; letterSpacing = .12f
            typeface = Typeface.DEFAULT_BOLD; setTextColor(cyan); setPadding(0, dp(24), 0, dp(12))
        })
        root.addView(TextView(this).apply {
            text = if (ticker.isBlank()) "TICKER" else ticker
            textSize = 30f; typeface = Typeface.DEFAULT_BOLD; setTextColor(Color.WHITE)
        })
        root.addView(TextView(this).apply {
            text = "Analiza ticker selectat din Watchlist"
            textSize = 15f; setTextColor(Color.rgb(175,182,198)); setPadding(0, dp(5), 0, dp(18))
        })
        val action = Button(this).apply {
            text = if (store.contains(ticker)) "✓ ÎN WATCHLIST" else "+ ADAUGĂ ÎN WATCHLIST"
            setTextColor(Color.WHITE); textSize = 15f; isAllCaps = false
            setBackgroundColor(Color.rgb(10,70,105))
            setOnClickListener {
                if (store.contains(ticker)) store.remove(ticker) else store.add(ticker)
                text = if (store.contains(ticker)) "✓ ÎN WATCHLIST" else "+ ADAUGĂ ÎN WATCHLIST"
            }
        }
        root.addView(action, LinearLayout.LayoutParams(-1, dp(52)).apply { setMargins(0,0,0,dp(16)) })
        root.addView(TextView(this).apply {
            text = "WATCHLIST"; textSize = 13f; typeface = Typeface.DEFAULT_BOLD; setTextColor(yellow)
        })
        root.addView(TextView(this).apply {
            text = "Acest ticker este gestionat separat de Portfolio și rămâne salvat local pe dispozitiv."
            textSize = 14f; setTextColor(Color.rgb(175,182,198)); setPadding(0,dp(8),0,0)
        })
        setContentView(root)
    }

    private fun dp(v:Int) = (v * resources.displayMetrics.density).toInt()
}
