package ro.alintudor.oracle.nativeui

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import ro.alintudor.oracle.core.OracleWatchlistStore
import java.util.Locale

/** Analysis-only Watchlist control. It does not alter the Watchlist screen. */
object OracleAnalysisWatchlistEyeOverlay {
    private val tickerPattern = Regex("^[A-Z][A-Z0-9.-]{0,7}$")
    private const val TAG_EYE = 0x0EA71001

    fun install(host: OracleNativeModule) {
        fun scan() {
            for (i in 0 until host.content.childCount) {
                val card = host.content.getChildAt(i) as? LinearLayout ?: continue
                val headline = card.getChildAt(0) as? LinearLayout ?: continue
                if (headline.getTag(TAG_EYE) == true) continue
                val tickerView = headline.getChildAt(0) as? TextView ?: continue
                val ticker = tickerView.text?.toString()?.trim()?.uppercase(Locale.US) ?: continue
                if (!tickerPattern.matches(ticker)) continue

                val store = OracleWatchlistStore(host.root.context)
                val eye = EyeView(host).apply {
                    isClickable = true
                    isFocusable = true
                    contentDescription = "Adaugă $ticker în Watchlist"
                    setOnClickListener {
                        val current = store.load().toMutableList()
                        val present = current.any { it.equals(ticker, true) }
                        if (present) {
                            current.removeAll { it.equals(ticker, true) }
                        } else {
                            current.add(ticker)
                        }
                        store.save(current)
                        refresh(current.any { it.equals(ticker, true) })
                        contentDescription = if (current.any { it.equals(ticker, true) }) {
                            "Scoate $ticker din Watchlist"
                        } else {
                            "Adaugă $ticker în Watchlist"
                        }
                    }
                }
                eye.refresh(store.load().any { it.equals(ticker, true) })
                headline.addView(eye, 1.coerceAtMost(headline.childCount), LinearLayout.LayoutParams(host.dp(46), host.dp(42)))
                headline.setTag(TAG_EYE, true)
            }
        }

        host.content.setOnHierarchyChangeListener(object : android.view.ViewGroup.OnHierarchyChangeListener {
            override fun onChildViewAdded(parent: android.view.View?, child: android.view.View?) { child?.post { scan() } }
            override fun onChildViewRemoved(parent: android.view.View?, child: android.view.View?) = Unit
        })
        host.content.post { scan() }
    }

    private class EyeView(private val host: OracleNativeModule) : View(host.root.context) {
        private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = host.dp(2).toFloat()
            strokeCap = Paint.Cap.ROUND
            strokeJoin = Paint.Join.ROUND
        }
        private var selected = false

        fun refresh(value: Boolean) {
            selected = value
            paint.color = if (selected) Color.rgb(255, 210, 45) else Color.rgb(125, 135, 155)
            invalidate()
        }

        override fun onDraw(canvas: Canvas) {
            super.onDraw(canvas)
            val cx = width / 2f
            val cy = height / 2f
            val rx = host.dp(15).toFloat()
            val ry = host.dp(9).toFloat()
            val iris = host.dp(4).toFloat()
            val left = cx - rx
            val right = cx + rx
            val top = cy - ry
            val bottom = cy + ry
            canvas.drawOval(left, top, right, bottom, paint)
            canvas.drawCircle(cx, cy, iris, paint)
            if (selected) {
                paint.style = Paint.Style.FILL
                canvas.drawCircle(cx, cy, host.dp(2).toFloat(), paint)
                paint.style = Paint.Style.STROKE
            }
        }
    }
}
