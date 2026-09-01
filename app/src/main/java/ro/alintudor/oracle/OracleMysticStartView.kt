package ro.alintudor.oracle

import android.content.Context
import android.graphics.Color
import android.view.Gravity
import android.view.View
import android.widget.*

/** B514 START: generated Oracle mystical artwork used as a single scalable home surface. */
class OracleMysticStartView(context: Context, private val onOpen: (String) -> Unit) : FrameLayout(context) {
    private val d = resources.displayMetrics.density
    private fun dp(v:Int)=(v*d).toInt()
    init {
        setBackgroundColor(Color.BLACK)
        val scroll=ScrollView(context).apply{isFillViewport=true;setBackgroundColor(Color.BLACK)}
        val root=FrameLayout(context)
        scroll.addView(root,FrameLayout.LayoutParams(-1,-1))
        val image=ImageView(context).apply{
            setImageResource(R.drawable.oracle_start_b514)
            scaleType=ImageView.ScaleType.FIT_CENTER
            adjustViewBounds=true
            setBackgroundColor(Color.BLACK)
            contentDescription="Oracle Stock Intelligence"
        }
        root.addView(image,FrameLayout.LayoutParams(-1,-2).apply{gravity=Gravity.TOP})
        val overlay=FrameLayout(context)
        root.addView(overlay,FrameLayout.LayoutParams(-1,-1).apply{gravity=Gravity.TOP})
        // Reference artwork: 180x389. Touch targets follow the artwork's relative positions.
        val w=resources.displayMetrics.widthPixels.toFloat(); val scale=w/180f
        val zones=listOf(
            Triple("portfolio",.16f,.47f),Triple("watchlist",.50f,.47f),
            Triple("analysis",.30f,.74f),Triple("growth",.80f,.47f),
            Triple("alerts",.16f,.72f),Triple("news",.50f,.72f),
            Triple("knowledge",.80f,.72f),Triple("analysis",.30f,.47f)
        )
        zones.forEach{(key,x,y)->
            val b=View(context).apply{setBackgroundColor(Color.TRANSPARENT);isClickable=true;setOnClickListener{onOpen(key)}}
            val size=dp(62); val lp=FrameLayout.LayoutParams(size,size)
            lp.leftMargin=(w*x-size/2).toInt().coerceAtLeast(0)
            lp.topMargin=(389f*y*scale-size/2).toInt().coerceAtLeast(0)
            overlay.addView(b,lp)
        }
        addView(scroll,LayoutParams(-1,-1))
    }
}
