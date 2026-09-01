package ro.alintudor.oracle

import android.content.Context
import android.graphics.*
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.View
import android.widget.*
import kotlin.math.min

/**
 * B514 START — vector composition inspired by the approved Oracle mystical interface.
 * IMPORTANT: this is a native UI composition, NOT an image asset.
 * Protected modules are opened through the existing callback and are not modified here.
 */
class OracleStartView(context: Context, private val onOpen: (String) -> Unit) : FrameLayout(context) {
    private val d = resources.displayMetrics.density
    private fun dp(v: Float) = (v * d).toInt()
    private val bg = Color.rgb(1, 3, 7)
    private val gold = Color.rgb(255, 196, 48)
    private val gold2 = Color.rgb(255, 224, 108)
    private val white = Color.rgb(245, 242, 232)
    private val muted = Color.rgb(181, 169, 143)
    private val green = Color.rgb(78, 231, 92)

    init {
        setBackgroundColor(bg)
        val scroll = ScrollView(context).apply {
            isFillViewport = true
            setBackgroundColor(Color.TRANSPARENT)
            overScrollMode = View.OVER_SCROLL_NEVER
        }
        val page = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(12f), dp(8f), dp(12f), dp(18f))
        }
        page.addView(topBar(), LinearLayout.LayoutParams(-1, dp(58f)))
        page.addView(OracleMysticCanvas(context), LinearLayout.LayoutParams(-1, dp(485f)))
        page.addView(tagline(), LinearLayout.LayoutParams(-1, dp(52f)))
        page.addView(cards(), LinearLayout.LayoutParams(-1, -2))
        page.addView(status(), LinearLayout.LayoutParams(-1, dp(105f)).apply { setMargins(0, dp(10f), 0, 0) })
        page.addView(footer(), LinearLayout.LayoutParams(-1, dp(62f)))
        scroll.addView(page)
        addView(scroll, LayoutParams(-1, -1))
    }

    private fun topBar() = LinearLayout(context).apply {
        orientation = LinearLayout.HORIZONTAL
        gravity = Gravity.CENTER_VERTICAL
        addView(TextView(context).apply {
            text = "☰"; textSize = 28f; gravity = Gravity.CENTER
            setTextColor(gold2); background = outline(gold, 12)
        }, LinearLayout.LayoutParams(dp(54f), dp(46f)).apply { setMargins(dp(5f), 0, 0, 0) })
        addView(Space(context), LinearLayout.LayoutParams(0, 1, 1f))
        addView(TextView(context).apply {
            text = "⚙"; textSize = 25f; gravity = Gravity.CENTER
            setTextColor(gold2); background = outline(gold, 12)
        }, LinearLayout.LayoutParams(dp(54f), dp(46f)).apply { setMargins(0, 0, dp(5f), 0) })
    }

    private fun tagline() = LinearLayout(context).apply {
        orientation = LinearLayout.VERTICAL; gravity = Gravity.CENTER
        addView(TextView(context).apply {
            text = "S E E   M O R E .   K N O W   F I R S T ."
            textSize = 11f; letterSpacing = .24f; typeface = Typeface.DEFAULT_BOLD; setTextColor(white); gravity = Gravity.CENTER
        })
        addView(View(context).apply { setBackgroundColor(gold) }, LinearLayout.LayoutParams(dp(220f), dp(1f)).apply { setMargins(0, dp(9f), 0, 0) })
    }

    private fun cards() = LinearLayout(context).apply {
        orientation = LinearLayout.VERTICAL
        val data = listOf(
            arrayOf("PORTFOLIO", "OVERVIEW", "portfolio", "purple"),
            arrayOf("WATCHLIST", "TRACK & FOCUS", "watchlist", "gold"),
            arrayOf("ANALYSIS", "CHARTS & TOOLS", "analysis", "cyan"),
            arrayOf("GROWTH", "FUTURE SCAN", "growth", "green"),
            arrayOf("ALERTS", "STAY AHEAD", "alerts", "red"),
            arrayOf("NEWS", "MARKET PULSE", "news", "cyan"),
            arrayOf("KNOWLEDGE", "LEARN & EVOLVE", "knowledge", "gold"),
            arrayOf("STOCK", "INTELLIGENCE", "stock", "purple")
        )
        for (r in 0..1) {
            val row = LinearLayout(context).apply { orientation = LinearLayout.HORIZONTAL }
            for (c in 0..3) {
                val item = data[r * 4 + c]
                row.addView(card(item), LinearLayout.LayoutParams(0, dp(151f), 1f).apply {
                    setMargins(if (c == 0) 0 else dp(4f), 0, if (c == 3) 0 else dp(4f), dp(8f))
                })
            }
            addView(row)
        }
    }

    private fun card(item: Array<String>) = LinearLayout(context).apply {
        orientation = LinearLayout.VERTICAL; gravity = Gravity.CENTER
        setPadding(dp(4f), dp(6f), dp(4f), dp(5f))
        background = clippedPanel()
        isClickable = true; isFocusable = true
        setOnClickListener { onOpen(item[2]) }
        val accent = when (item[3]) {
            "purple" -> Color.rgb(204, 79, 255)
            "cyan" -> Color.rgb(25, 215, 255)
            "green" -> Color.rgb(119, 255, 39)
            "red" -> Color.rgb(255, 69, 48)
            else -> gold
        }
        addView(OracleCardGlyph(context, accent, item[2]), LinearLayout.LayoutParams(-1, dp(83f)))
        addView(TextView(context).apply {
            text = item[0]; textSize = 12.5f; letterSpacing = .08f; typeface = Typeface.DEFAULT_BOLD
            setTextColor(white); gravity = Gravity.CENTER; maxLines = 1
        }, LinearLayout.LayoutParams(-1, dp(24f)))
        addView(TextView(context).apply {
            text = item[1]; textSize = 7.5f; letterSpacing = .10f; setTextColor(muted); gravity = Gravity.CENTER; maxLines = 1
        }, LinearLayout.LayoutParams(-1, dp(18f)))
        addView(View(context).apply { setBackgroundColor(accent) }, LinearLayout.LayoutParams(dp(38f), dp(1f)).apply { setMargins(0, dp(4f), 0, 0) })
    }

    private fun status() = LinearLayout(context).apply {
        orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL
        setPadding(dp(13f), dp(8f), dp(13f), dp(8f)); background = clippedPanel()
        addView(OracleCardGlyph(context, green, "eye"), LinearLayout.LayoutParams(dp(72f), dp(78f)))
        addView(LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL; gravity = Gravity.CENTER_VERTICAL
            addView(TextView(context).apply { text="ORACLE READY"; textSize=12f; letterSpacing=.04f; typeface=Typeface.DEFAULT_BOLD; setTextColor(white) })
            addView(TextView(context).apply { text="Market Intelligence Active"; textSize=9f; setTextColor(green); setPadding(0,dp(5f),0,0) })
        }, LinearLayout.LayoutParams(0,-1,1f))
        addView(View(context).apply { setBackgroundColor(gold) }, LinearLayout.LayoutParams(dp(1f), dp(62f)))
        addView(OracleCardGlyph(context, gold, "signal"), LinearLayout.LayoutParams(dp(70f), dp(78f)))
        addView(LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL; gravity = Gravity.CENTER_VERTICAL
            addView(TextView(context).apply { text="LOCAL INTELLIGENCE"; textSize=11f; letterSpacing=.03f; typeface=Typeface.DEFAULT_BOLD; setTextColor(white) })
            addView(TextView(context).apply { text="Synced & Protected"; textSize=9f; setTextColor(green); setPadding(0,dp(5f),0,0) })
        }, LinearLayout.LayoutParams(0,-1,1f))
        addView(View(context).apply { setBackgroundColor(green) }, LinearLayout.LayoutParams(dp(16f), dp(16f)).apply { setMargins(dp(5f),0,dp(3f),0) })
    }

    private fun footer() = LinearLayout(context).apply {
        orientation = LinearLayout.VERTICAL; gravity = Gravity.CENTER
        addView(TextView(context).apply { text="O R A C L E"; textSize=15f; letterSpacing=.45f; typeface=Typeface.SERIF; setTextColor(gold) })
        addView(TextView(context).apply { text="S E E   M O R E .   K N O W   F I R S T ."; textSize=7f; letterSpacing=.20f; setTextColor(muted); setPadding(0,dp(5f),0,0) })
    }

    private fun clippedPanel() = GradientDrawable().apply {
        setColor(Color.argb(205, 4, 7, 14)); setStroke(dp(1f), gold); cornerRadius = dp(10f).toFloat()
    }
    private fun outline(color:Int, radius:Int) = GradientDrawable().apply { setColor(Color.TRANSPARENT); setStroke(dp(1f), color); cornerRadius=dp(radius.toFloat()).toFloat() }

    private class OracleMysticCanvas(context: Context) : View(context) {
        private val p = Paint(Paint.ANTI_ALIAS_FLAG); private val path = Path()
        private val gold = Color.rgb(255, 196, 48); private val bright = Color.rgb(255, 225, 105)
        private val white = Color.rgb(248, 245, 234)
        override fun onDraw(c: Canvas) {
            val w = width.toFloat(); val h = height.toFloat(); val cx = w/2f; val cy = h*.49f; val r=min(w*.43f,h*.39f)
            p.style=Paint.Style.FILL; p.shader=LinearGradient(0f,0f,w,h,Color.rgb(1,2,5),Color.rgb(8,6,5),Shader.TileMode.CLAMP); c.drawRect(0f,0f,w,h,p); p.shader=null
            p.style=Paint.Style.STROKE; p.strokeWidth=1f*density; p.color=gold; p.alpha=55
            for(i in 1..8) c.drawCircle(cx,cy,r*i/8f,p)
            for(i in 0 until 12){ val a=i*Math.PI/6; c.drawLine(cx+Math.cos(a)*r*.05,cy+Math.sin(a)*r*.05,cx+Math.cos(a)*r,cy+Math.sin(a)*r,p) }
            p.strokeWidth=1.2f*density; p.alpha=145
            c.drawCircle(cx,cy,r*.72f,p); c.drawCircle(cx,cy,r*.61f,p); c.drawCircle(cx,cy,r*.43f,p)
            // stylized eye
            path.reset(); path.moveTo(cx-r*.58f,cy); path.cubicTo(cx-r*.30f,cy-r*.34f,cx+r*.30f,cy-r*.34f,cx+r*.58f,cy); path.cubicTo(cx+r*.30f,cy+r*.34f,cx-r*.30f,cy+r*.34f,cx-r*.58f,cy)
            p.color=bright; p.alpha=235; p.strokeWidth=2.2f*density; c.drawPath(path,p)
            p.color=gold; p.strokeWidth=1.2f*density; p.alpha=255; c.drawCircle(cx,cy,r*.14f,p); p.style=Paint.Style.FILL; c.drawCircle(cx,cy,r*.065f,p)
            // vertical oracle axis and eye above
            p.style=Paint.Style.STROKE; p.strokeWidth=1f*density; p.alpha=210
            c.drawLine(cx, dp(10f).toFloat(), cx, cy-r*.78f,p); c.drawLine(cx,cy+r*.78f,cx,h-dp(12f).toFloat(),p)
            val ty=dp(60f).toFloat(); path.reset(); path.moveTo(cx-dp(38f),ty); path.lineTo(cx,ty-dp(24f)); path.lineTo(cx+dp(38f),ty); path.lineTo(cx,ty+dp(24f)); path.close(); c.drawPath(path,p); c.drawCircle(cx,ty,dp(9f).toFloat(),p)
            p.style=Paint.Style.FILL; c.drawCircle(cx,dp(10f).toFloat(),dp(3f).toFloat(),p)
            // subtle cloud / constellation texture
            p.alpha=55; for(i in 0..28){ val x=(i*83%maxOf(1,width)).toFloat(); val y=(i*47%maxOf(1,height)).toFloat(); c.drawCircle(x,y,1.5f*density,p) }
            // logo typography is native text, not baked into an image
            p.alpha=255; p.color=white; p.textAlign=Paint.Align.CENTER; p.typeface=Typeface.create(Typeface.SERIF,Typeface.BOLD); p.textSize=dp(30f).toFloat(); c.drawText("ORACLE",cx,cy+r*.52f,p)
            p.color=gold; p.typeface=Typeface.DEFAULT_BOLD; p.textSize=dp(8f).toFloat(); c.drawText("STOCK INTELLIGENCE",cx,cy+r*.61f,p)
            p.textAlign=Paint.Align.LEFT
        }
        private val density get() = resources.displayMetrics.density
    }

    private class OracleCardGlyph(context: Context, private val accent: Int, private val kind: String) : View(context) {
        private val p=Paint(Paint.ANTI_ALIAS_FLAG); private val path=Path(); private val d=resources.displayMetrics.density
        override fun onDraw(c:Canvas){
            val w=width.toFloat(); val h=height.toFloat(); val cx=w/2f; val cy=h/2f; val r=min(w,h)*.39f
            p.style=Paint.Style.STROKE; p.strokeWidth=1f*d; p.color=accent; p.alpha=120
            c.drawCircle(cx,cy,r,p); c.drawCircle(cx,cy,r*.78f,p); c.drawCircle(cx,cy,r*.54f,p)
            p.alpha=245; p.strokeWidth=2f*d
            when(kind){
                "eye" -> { path.reset(); path.moveTo(cx-r*.65f,cy); path.cubicTo(cx-r*.3f,cy-r*.55f,cx+r*.3f,cy-r*.55f,cx+r*.65f,cy); path.cubicTo(cx+r*.3f,cy+r*.55f,cx-r*.3f,cy+r*.55f,cx-r*.65f,cy); c.drawPath(path,p); c.drawCircle(cx,cy,r*.18f,p) }
                "signal" -> { for(i in -2..2){val bh=(kotlin.math.abs(i)+1)*r*.45f; c.drawLine(cx+i*r*.28f,cy+bh,cx+i*r*.28f,cy-bh,p)} }
                "portfolio" -> { c.drawRect(cx-r*.28f,cy-r*.30f,cx+r*.28f,cy+r*.28f,p); c.drawCircle(cx+r*.20f,cy+r*.20f,r*.18f,p) }
                "watchlist" -> { path.reset(); path.moveTo(cx-r*.62f,cy); path.cubicTo(cx-r*.3f,cy-r*.55f,cx+r*.3f,cy-r*.55f,cx+r*.62f,cy); path.cubicTo(cx+r*.3f,cy+r*.55f,cx-r*.3f,cy+r*.55f,cx-r*.62f,cy); c.drawPath(path,p); c.drawCircle(cx,cy,r*.16f,p) }
                "analysis" -> { path.reset(); path.moveTo(cx-r*.55f,cy+r*.35f); path.lineTo(cx-r*.18f,cy); path.lineTo(cx+r*.05f,cy+r*.12f); path.lineTo(cx+r*.52f,cy-r*.42f); c.drawPath(path,p) }
                "growth" -> { path.reset(); path.moveTo(cx-r*.5f,cy+r*.3f); path.lineTo(cx-r*.15f,cy); path.lineTo(cx+r*.05f,cy+r*.1f); path.lineTo(cx+r*.5f,cy-r*.4f); c.drawPath(path,p) }
                "alerts" -> { c.drawCircle(cx,cy-r*.08f,r*.38f,p); c.drawLine(cx-r*.52f,cy+r*.42f,cx+r*.52f,cy+r*.42f,p) }
                "news" -> { c.drawRect(cx-r*.45f,cy-r*.42f,cx+r*.45f,cy+r*.42f,p); for(i in 0..2)c.drawLine(cx-r*.28f,cy-r*.18f+i*r*.18f,cx+r*.28f,cy-r*.18f+i*r*.18f,p) }
                "knowledge" -> { c.drawRect(cx-r*.45f,cy-r*.4f,cx,cy+r*.4f,p); c.drawRect(cx,cy-r*.4f,cx+r*.45f,cy+r*.4f,p) }
                "stock" -> { path.reset(); path.moveTo(cx-r*.45f,cy); path.lineTo(cx-r*.12f,cy-r*.3f); path.lineTo(cx+r*.12f,cy+r*.28f); path.lineTo(cx+r*.48f,cy-r*.42f); c.drawPath(path,p) }
            }
            p.style=Paint.Style.FILL; p.alpha=255; c.drawCircle(cx,cy-r*.88f,r*.035f,p)
        }
    }
}
