package ro.alintudor.oracle

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Shader
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.View
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.Space
import android.widget.TextView

/** B514 START — complete visual redesign. Modules are opened unchanged. */
class OracleStartView(context: Context, private val onOpen: (String) -> Unit) : FrameLayout(context) {
    private val d = resources.displayMetrics.density
    private fun dp(v: Int) = (v * d).toInt()
    private val bg = Color.rgb(2, 5, 12)
    private val panel = Color.rgb(8, 13, 25)
    private val panel2 = Color.rgb(11, 18, 33)
    private val cyan = Color.rgb(42, 202, 255)
    private val cyanSoft = Color.rgb(105, 225, 255)
    private val gold = Color.rgb(255, 205, 55)
    private val white = Color.rgb(244, 247, 252)
    private val muted = Color.rgb(143, 158, 181)
    private val green = Color.rgb(57, 222, 145)

    init {
        setBackgroundColor(bg)
        addView(StartBackground(context), LayoutParams(-1, -1))
        val scroll = ScrollView(context).apply { isFillViewport = true; setBackgroundColor(Color.TRANSPARENT) }
        val page = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(18), dp(14), dp(18), dp(30))
        }
        page.addView(topBar())
        page.addView(hero(), LinearLayout.LayoutParams(-1, -2).apply { setMargins(0, dp(18), 0, 0) })
        page.addView(moduleGrid(), LinearLayout.LayoutParams(-1, -2).apply { setMargins(0, dp(18), 0, 0) })
        page.addView(footer(), LinearLayout.LayoutParams(-1, -2).apply { setMargins(0, dp(10), 0, 0) })
        scroll.addView(page)
        addView(scroll, LayoutParams(-1, -1))
    }

    private fun topBar() = LinearLayout(context).apply {
        orientation = LinearLayout.HORIZONTAL
        gravity = Gravity.CENTER_VERTICAL
        addView(TextView(context).apply {
            text = "ORACLE"
            textSize = 30f
            typeface = Typeface.create(Typeface.SERIF, Typeface.BOLD)
            letterSpacing = .07f
            setTextColor(white)
        }, LinearLayout.LayoutParams(0, dp(46), 1f))
        addView(TextView(context).apply {
            text = "B514"
            textSize = 12f
            typeface = Typeface.DEFAULT_BOLD
            letterSpacing = .18f
            gravity = Gravity.CENTER
            setTextColor(gold)
            background = rounded(Color.TRANSPARENT, gold, 18)
            setPadding(dp(14), 0, dp(14), 0)
        }, LinearLayout.LayoutParams(dp(72), dp(34)))
    }

    private fun hero() = LinearLayout(context).apply {
        orientation = LinearLayout.VERTICAL
        setPadding(dp(20), dp(20), dp(20), dp(20))
        background = rounded(panel, Color.rgb(33, 70, 105), 24)
        elevation = dp(8).toFloat()
        addView(TextView(context).apply {
            text = "INTELLIGENCE COMMAND CENTER"
            textSize = 11f; typeface = Typeface.DEFAULT_BOLD; letterSpacing = .18f; setTextColor(cyan)
        })
        addView(TextView(context).apply {
            text = "DECIDE BEFORE\nTHE MARKET MOVES."
            textSize = 29f; typeface = Typeface.DEFAULT_BOLD; setTextColor(white)
            setPadding(0, dp(8), 0, dp(4))
        })
        addView(TextView(context).apply {
            text = "Un singur punct de intrare pentru întregul Oracle."
            textSize = 14f; setTextColor(muted)
        })
        addView(LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(12), dp(10), dp(12), dp(10))
            background = rounded(Color.rgb(6, 19, 24), Color.rgb(27, 77, 76), 14)
            addView(View(context).apply { setBackgroundColor(green) }, LinearLayout.LayoutParams(dp(8), dp(8)).apply { setMargins(0,0,dp(10),0) })
            addView(TextView(context).apply {
                text = "ORACLE CORE ONLINE"; textSize = 11f; typeface = Typeface.DEFAULT_BOLD; letterSpacing = .10f; setTextColor(white)
            }, LinearLayout.LayoutParams(0, -2, 1f))
            addView(TextView(context).apply {
                text = "V6g-FINAL-B514"; textSize = 9f; typeface = Typeface.DEFAULT_BOLD; setTextColor(cyanSoft)
            })
        }, LinearLayout.LayoutParams(-1, -2).apply { setMargins(0, dp(18), 0, 0) })
        addView(LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL
            addView(TextView(context).apply { text="LOCAL MARKET INTELLIGENCE"; textSize=9f; letterSpacing=.12f; typeface=Typeface.DEFAULT_BOLD; setTextColor(muted) }, LinearLayout.LayoutParams(0,dp(30),1f))
            addView(TextView(context).apply { text="●  READY"; textSize=10f; typeface=Typeface.DEFAULT_BOLD; setTextColor(green) })
        }, LinearLayout.LayoutParams(-1,-2).apply { setMargins(0,dp(8),0,0) })
    }

    private fun moduleGrid() = LinearLayout(context).apply {
        orientation = LinearLayout.VERTICAL
        addView(TextView(context).apply { text="ORACLE MODULES"; textSize=11f; typeface=Typeface.DEFAULT_BOLD; letterSpacing=.20f; setTextColor(gold) })
        addView(TextView(context).apply { text="Choose your next move"; textSize=20f; typeface=Typeface.DEFAULT_BOLD; setTextColor(white); setPadding(0,dp(4),0,dp(12)) })
        val modules = listOf(
            arrayOf("01","PORTFOLIO","Poziții • P/L • alocare","portfolio"), arrayOf("02","GROWTH","Oportunități • scor • forecast","growth"),
            arrayOf("03","ANALYSIS","Tehnic • fundamentals • scenarii","analysis"), arrayOf("04","WATCHLIST","Ticker-e urmărite","watchlist"),
            arrayOf("05","ALERTS","Semnale și alerte active","alerts"), arrayOf("06","NEWS","Catalizatori și evenimente","news"),
            arrayOf("07","KNOWLEDGE","Baza de cunoaștere Oracle","knowledge"), arrayOf("08","JURNAL","Istoric și activitate","journal")
        )
        for (i in modules.indices step 2) {
            val row = LinearLayout(context).apply { orientation = LinearLayout.HORIZONTAL }
            row.addView(card(modules[i]), LinearLayout.LayoutParams(0,dp(112),1f).apply { setMargins(0,0,dp(7),dp(8)) })
            row.addView(card(modules[i+1]), LinearLayout.LayoutParams(0,dp(112),1f).apply { setMargins(dp(7),0,0,dp(8)) })
            addView(row)
        }
    }

    private fun card(m: Array<String>) = LinearLayout(context).apply {
        val accent = when (m[0]) { "01","04" -> cyan; "02","05" -> gold; "03","07" -> cyanSoft; else -> Color.rgb(173,135,255) }
        orientation = LinearLayout.VERTICAL; gravity = Gravity.CENTER_VERTICAL
        setPadding(dp(14),dp(10),dp(12),dp(10)); background = rounded(panel2,Color.rgb(36,53,77),18)
        isClickable = true; isFocusable = true; elevation = dp(3).toFloat(); setOnClickListener { onOpen(m[3]) }
        addView(LinearLayout(context).apply {
            orientation=LinearLayout.HORIZONTAL; gravity=Gravity.CENTER_VERTICAL
            addView(TextView(context).apply { text=m[0]; textSize=9f; typeface=Typeface.DEFAULT_BOLD; setTextColor(accent); gravity=Gravity.CENTER; background=rounded(Color.TRANSPARENT,accent,10); setPadding(dp(8),0,dp(8),0) },LinearLayout.LayoutParams(dp(32),dp(22)))
            addView(Space(context),LinearLayout.LayoutParams(0,1,1f))
            addView(TextView(context).apply { text="›"; textSize=24f; setTextColor(muted); gravity=Gravity.CENTER },LinearLayout.LayoutParams(dp(20),dp(28)))
        },LinearLayout.LayoutParams(-1,dp(26)))
        addView(TextView(context).apply { text=m[1]; textSize=14f; typeface=Typeface.DEFAULT_BOLD; setTextColor(white); setPadding(0,dp(7),0,dp(2)) })
        addView(TextView(context).apply { text=m[2]; textSize=9.5f; setTextColor(muted); maxLines=2 })
    }

    private fun footer() = LinearLayout(context).apply {
        orientation=LinearLayout.VERTICAL
        addView(View(context).apply { setBackgroundColor(Color.rgb(31,49,72)) },LinearLayout.LayoutParams(-1,dp(1)))
        addView(TextView(context).apply { text="ORACLE  •  V6g-FINAL-B514  •  START"; textSize=9f; typeface=Typeface.DEFAULT_BOLD; letterSpacing=.14f; gravity=Gravity.CENTER; setTextColor(Color.rgb(108,124,150)); setPadding(0,dp(12),0,0) })
    }

    private fun rounded(fill:Int, stroke:Int, radius:Int) = GradientDrawable().apply { setColor(fill); setCornerRadius(dp(radius).toFloat()); setStroke(dp(1),stroke) }

    private class StartBackground(context: Context) : View(context) {
        private val p=Paint(Paint.ANTI_ALIAS_FLAG); private val d=resources.displayMetrics.density
        override fun onDraw(c:Canvas) {
            val w=width.toFloat(); val h=height.toFloat()
            p.shader=LinearGradient(0f,0f,w,h,Color.rgb(2,5,12),Color.rgb(5,11,22),Shader.TileMode.CLAMP); c.drawRect(0f,0f,w,h,p); p.shader=null
            p.style=Paint.Style.STROKE; p.strokeWidth=d; p.color=Color.rgb(42,202,255); p.alpha=28
            val step=54f*d; var x=-h
            while(x<w+h){c.drawLine(x,0f,x+h,h,p);x+=step}; var y=0f
            while(y<h){c.drawLine(0f,y,w,y,p);y+=step}
            p.style=Paint.Style.FILL; p.alpha=18; c.drawCircle(w*.86f,h*.16f,110f*d,p); p.alpha=12; c.drawCircle(w*.14f,h*.72f,150f*d,p); p.alpha=255
        }
    }
}
