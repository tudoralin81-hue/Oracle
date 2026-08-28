package ro.alintudor.oracle

import android.app.Activity
import android.os.Bundle
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.view.MotionEvent
import android.view.View
import kotlin.math.min

class MainActivity : Activity() {
    private lateinit var oracleView: OracleView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        oracleView = OracleView()
        setContentView(oracleView)
    }

    override fun onBackPressed() {
        if (oracleView.goBack()) return
        super.onBackPressed()
    }

    inner class OracleView : View(this@MainActivity) {
        private val bg = Color.rgb(2, 4, 10)
        private val panel = Color.rgb(8, 13, 23)
        private val panel2 = Color.rgb(11, 17, 29)
        private val text = Color.rgb(238, 242, 250)
        private val muted = Color.rgb(137, 151, 177)
        private val accent = Color.rgb(139, 124, 255)
        private val green = Color.rgb(54, 211, 153)
        private val red = Color.rgb(248, 113, 113)
        private val cyan = Color.rgb(64, 200, 255)
        private val gold = Color.rgb(220, 177, 76)
        private val p = Paint(Paint.ANTI_ALIAS_FLAG)
        private var page = "PIN"
        private var pin = ""
        private var msg = ""
        private val history = ArrayDeque<String>()

        // Initial data copied from the information currently exposed by the WordPress Oracle pages.
        // Dynamic plugin records will be connected through the Oracle data layer in the next backend step.
        private val positions = listOf(
            "NVDA" to 14.8f, "VRT" to 12.4f, "RZLV" to 8.7f, "PLTR" to 7.9f, "SOUN" to 5.8f
        )
        private val alerts = listOf(
            "VRT — trailing stop approaching", "RZLV — momentum weakening", "NVDA — profit target reached"
        )
        private val journal = listOf(
            "BUY NVDA · 10 shares", "BUY VRT · 20 shares", "SELL CRSP · position closed"
        )
        private val watch = listOf("NVDA", "VRT", "RZLV", "PLTR", "SOUN", "AMD")
        private val news = listOf(
            "US yields and equities in focus",
            "AI infrastructure spending accelerates",
            "Semiconductors lead risk appetite",
            "Markets await inflation signals"
        )
        private val articles = listOf(
            "Trendul și structura pieței — Aplicarea profesionistă",
            "Trendul și structura pieței — Citirea pe grafic",
            "Trendul și structura pieței — Fundamentul",
            "Cum funcționează piața în spatele graficului",
            "Bazele pieței și limbajul graficului — Aplicare",
            "Bazele pieței și limbajul graficului — Citirea pe grafic",
            "Bazele pieței și limbajul graficului — FUNDAMENTUL"
        )

        init { setBackgroundColor(bg) }

        private fun sx(v: Float) = v * width / 1080f
        private fun sy(v: Float) = v * height / 1920f
        private fun fs(v: Float) = v * min(width / 1080f, height / 1920f)

        private fun txt(c: Canvas, s: String, x: Float, y: Float, size: Float, color: Int = text, bold: Boolean = false) {
            p.color = color
            p.textSize = fs(size)
            p.typeface = Typeface.create("sans", if (bold) Typeface.BOLD else Typeface.NORMAL)
            c.drawText(s, sx(x), sy(y), p)
        }
        private fun center(c: Canvas, s: String, x: Float, y: Float, size: Float, color: Int = text, bold: Boolean = true) {
            p.color = color
            p.textSize = fs(size)
            p.typeface = Typeface.create("sans", if (bold) Typeface.BOLD else Typeface.NORMAL)
            c.drawText(s, sx(x) - p.measureText(s) / 2f, sy(y), p)
        }
        private fun box(c: Canvas, l: Float, t: Float, r: Float, b: Float, color: Int = panel, rad: Float = 26f) {
            p.color = color; p.style = Paint.Style.FILL
            c.drawRoundRect(sx(l), sy(t), sx(r), sy(b), sx(rad), sx(rad), p)
        }
        private fun line(c: Canvas, x1: Float, y1: Float, x2: Float, y2: Float, color: Int, widthPx: Float = 2f) {
            p.color = color; p.strokeWidth = sx(widthPx); p.style = Paint.Style.STROKE
            c.drawLine(sx(x1), sy(y1), sx(x2), sy(y2), p)
            p.style = Paint.Style.FILL
        }

        override fun onDraw(c: Canvas) {
            c.drawColor(bg)
            if (page == "PIN") drawPin(c) else drawPage(c)
        }

        private fun drawPin(c: Canvas) {
            val cx = 540f
            center(c, "ORACLE", cx, 230f, 58f, accent, true)
            center(c, "AI STOCK ORACLE", cx, 270f, 21f, muted, false)
            box(c, 100f, 350f, 980f, 1160f, panel, 34f)
            center(c, "Introdu PIN", cx, 440f, 30f, text, true)
            center(c, if (pin.isEmpty()) "••••" else "•".repeat(pin.length), cx, 540f, 50f, text, true)
            center(c, msg, cx, 600f, 18f, red, false)
            val keys = arrayOf("1","2","3","4","5","6","7","8","9","⌫","0","OK")
            for (i in keys.indices) {
                val col = i % 3; val row = i / 3
                val x = 300f + col * 240f
                val y = 700f + row * 115f
                box(c, x - 85f, y - 48f, x + 85f, y + 48f, panel2, 24f)
                center(c, keys[i], x, y + 17f, 28f, text, true)
            }
        }

        private fun drawHeader(c: Canvas, title: String) {
            box(c, 22f, 20f, 1058f, 125f, panel, 26f)
            if (page != "HUB") center(c, "‹", 70f, 88f, 54f, text, false)
            txt(c, "ORACLE", if (page == "HUB") 48f else 110f, 58f, 24f, accent, true)
            txt(c, title, if (page == "HUB") 48f else 110f, 101f, 31f, text, true)
            if (page != "HUB") txt(c, "Înapoi", 150f, 58f, 13f, muted, false)
        }

        private fun drawPage(c: Canvas) {
            drawHeader(c, pageTitle())
            when (page) {
                "HUB" -> drawHub(c)
                "PORTFOLIO" -> drawPortfolio(c)
                "ALERTS" -> drawAlerts(c)
                "JOURNAL" -> drawJournal(c)
                "NEWS" -> drawNews(c)
                "GROWTH" -> drawGrowth(c)
                "ANALYSIS" -> drawAnalysis(c)
                "WATCHLIST" -> drawWatch(c)
                "KNOWLEDGE" -> drawKnowledge(c)
            }
        }

        private fun pageTitle() = when(page) {
            "HUB" -> "AI Stock Oracle"; "PORTFOLIO" -> "Portfolio"; "ALERTS" -> "SELL Alerts"
            "JOURNAL" -> "Jurnal activitate"; "NEWS" -> "News"; "GROWTH" -> "Growth"
            "ANALYSIS" -> "Analysis"; "WATCHLIST" -> "Watchlist"; "KNOWLEDGE" -> "Knowledge"
            else -> "Oracle"
        }

        private fun drawHub(c: Canvas) {
            val cx = 540f; val cy = 1030f; val radius = 350f
            p.color = Color.rgb(37,46,67); p.strokeWidth = sx(2f); p.style = Paint.Style.STROKE
            c.drawCircle(sx(cx), sy(cy), sx(radius), p); p.style = Paint.Style.FILL
            hubNode(c,cx,cy-radius,"Portfolio",accent); hubNode(c,cx-radius*.9f,cy-radius*.38f,"Alerts",red)
            hubNode(c,cx+radius*.9f,cy-radius*.38f,"News",green); hubNode(c,cx-radius*.8f,cy+radius*.55f,"Growth",accent)
            hubNode(c,cx+radius*.8f,cy+radius*.55f,"Watchlist",accent); hubNode(c,cx-radius*.32f,cy+radius*.92f,"Analysis",green)
            hubNode(c,cx+radius*.32f,cy+radius*.92f,"Journal",accent); hubNode(c,cx,cy+radius*1.18f,"Knowledge",gold)
            center(c,"ORACLE",cx,cy+8f,36f,text,true); center(c,"AI STOCK",cx,cy+39f,16f,muted,false)
            box(c,40f,1660f,1040f,1795f,panel,28f)
            txt(c,"12-MONTH FORECAST",68f,1710f,15f,muted,true)
            txt(c,"\$161,900",68f,1755f,38f,text,true)
            txt(c,"+26.0% projected",68f,1782f,16f,green,true)
        }

        private fun hubNode(c: Canvas,x: Float,y: Float,label: String,color: Int) {
            p.color=color; c.drawCircle(sx(x),sy(y),sx(58f),p)
            center(c,label,x,y+6f,14f,Color.WHITE,true)
        }

        private fun drawPortfolio(c: Canvas) {
            box(c,40f,165f,1040f,330f,panel,28f)
            txt(c,"TOTAL PORTFOLIO",70f,215f,16f,muted,true)
            txt(c,"\$128,420",70f,275f,42f,text,true)
            txt(c,"+18.6%",850f,260f,22f,green,true)
            var y=385f
            positions.forEach { (ticker,weight) ->
                box(c,40f,y-34f,1040f,y+74f,panel2,24f)
                txt(c,ticker,70f,y+10f,25f,text,true)
                txt(c,"Weight ${"%.1f".format(weight)}%",70f,y+48f,16f,muted,false)
                txt(c,if(weight>10f)"HOLD" else "WATCH",850f,y+20f,17f,if(weight>10f)green else accent,true)
                y += 130f
            }
        }

        private fun drawAlerts(c: Canvas) {
            var y=180f
            alerts.forEachIndexed { i,item ->
                box(c,40f,y-38f,1040f,y+90f,panel2,24f)
                p.color=if(i==2)green else red; c.drawCircle(sx(75f),sy(y+22f),sx(10f),p)
                txt(c,item,110f,y+18f,19f,text,true)
                txt(c,"Signal engine · 08:25",110f,y+55f,14f,muted,false)
                y+=150f
            }
            actionButton(c,"RUN ALERT SCAN",40f,y+20f,1040f,y+95f)
        }

        private fun drawJournal(c: Canvas) {
            var y=180f
            journal.forEachIndexed { i,item ->
                box(c,40f,y-38f,1040f,y+80f,panel2,24f)
                txt(c,"08:2$i",70f,y+8f,15f,muted,false)
                txt(c,item,150f,y+12f,18f,text,true)
                y+=145f
            }
            actionButton(c,"EXPORT JOURNAL",40f,y+20f,1040f,y+95f)
            txt(c,"Istoric complet: sincronizarea cu datele istorice Oracle urmează în data layer.",40f,y+145f,14f,muted,false)
        }

        private fun drawNews(c: Canvas) {
            var y=170f
            news.forEach { title ->
                box(c,40f,y-34f,1040f,y+100f,panel2,24f)
                txt(c,"ECONOMY",70f,y,14f,accent,true)
                txt(c,title,70f,y+40f,20f,text,true)
                txt(c,"Updated today · breaking monitor",70f,y+76f,14f,muted,false)
                y+=155f
            }
            txt(c,"12 surse economice în versiunea WordPress · feedurile rămân parte din specificația Oracle.",40f,900f,14f,muted,false)
        }

        private fun drawGrowth(c: Canvas) {
            box(c,40f,165f,1040f,420f,panel,28f)
            txt(c,"12-MONTH FORECAST",70f,220f,16f,muted,true)
            txt(c,"\$161,900",70f,285f,48f,text,true)
            txt(c,"+26.0% projected",70f,325f,19f,green,true)
            txt(c,"Scenario",830f,220f,14f,muted,false)
            txt(c,"Base",830f,260f,18f,text,true)
            txt(c,"Bull",830f,300f,18f,green,true)
            txt(c,"Bear",830f,340f,18f,red,true)
            for (i in 0..8) {
                val x=70f+i*95f; val h=80f+i*22f
                p.color=Color.rgb(38,51,76); c.drawRect(sx(x),sy(720f-h),sx(x+60f),sy(720f),p)
            }
        }

        private fun drawAnalysis(c: Canvas) {
            box(c,40f,165f,1040f,300f,panel,28f)
            txt(c,"Ticker",70f,220f,15f,muted,false); txt(c,"NVDA",190f,220f,24f,text,true)
            txt(c,"BULLISH",850f,220f,17f,green,true)
            val rows=arrayOf("Trend" to "Above EMA 20/50","Momentum" to "Positive","Support" to "\$172.00","Resistance" to "\$184.50","Risk" to "Medium")
            var y=360f
            rows.forEach{(a,b)->
                line(c,70f,y-25f,1010f,y-25f,Color.rgb(30,38,55),1f)
                txt(c,a,70f,y,17f,muted,false); txt(c,b,430f,y,19f,text,true); y+=82f
            }
        }

        private fun drawWatch(c: Canvas) {
            var y=175f
            watch.forEach{ticker->
                box(c,40f,y-36f,1040f,y+70f,panel2,24f)
                txt(c,ticker,70f,y+20f,25f,text,true)
                txt(c,"Live watch · signal ready",250f,y+20f,16f,muted,false)
                y+=125f
            }
            actionButton(c,"+ ADD TICKER",40f,y+15f,1040f,y+90f)
        }

        private fun drawKnowledge(c: Canvas) {
            var y=170f
            articles.forEachIndexed { i, title ->
                box(c,40f,y-34f,1040f,y+95f,panel2,24f)
                txt(c,"${i+1}",70f,y+38f,22f,gold,true)
                txt(c,title,130f,y+18f,18f,text,true)
                txt(c,"Pastile pentru knowledge",130f,y+55f,14f,muted,false)
                y+=145f
            }
        }

        private fun actionButton(c: Canvas,label:String,l:Float,t:Float,r:Float,b:Float){
            box(c,l,t,r,b,Color.rgb(25,34,54),20f)
            center(c,label,(l+r)/2f,t+49f,17f,accent,true)
        }

        private fun navigate(next: String) {
            if (page == next) return
            if (page != "PIN") history.addLast(page)
            page = next
            invalidate()
        }

        fun goBack(): Boolean {
            if (page == "PIN") return false
            page = if (history.isNotEmpty()) history.removeLast() else "HUB"
            invalidate()
            return true
        }

        override fun onTouchEvent(e: MotionEvent): Boolean {
            if (e.action != MotionEvent.ACTION_UP) return true
            val x = e.x * 1080f / width
            val y = e.y * 1920f / height

            if (page == "PIN") { handlePin(x,y); invalidate(); return true }
            if (page != "HUB" && y < 135f && x < 130f) { goBack(); return true }

            if (page == "HUB") {
                val cx=540f; val cy=1030f; val radius=350f
                val dx=x-cx; val dy=y-cy
                when {
                    dy < -radius*.65f -> navigate("PORTFOLIO")
                    dx < -radius*.60f && dy<0 -> navigate("ALERTS")
                    dx > radius*.60f && dy<0 -> navigate("NEWS")
                    dx < -radius*.45f && dy>0 -> navigate("GROWTH")
                    dx > radius*.45f && dy>0 -> navigate("WATCHLIST")
                    dy > radius*.85f && dx>0 -> navigate("JOURNAL")
                    dy > radius*.85f && dx<0 -> navigate("KNOWLEDGE")
                    dy > radius*.45f -> navigate("ANALYSIS")
                }
            }
            invalidate(); return true
        }

        private fun handlePin(x:Float,y:Float){
            if(y<650f)return
            val cx=540f
            val col=((x-cx+240f)/240f).toInt()
            val row=((y-700f)/115f).toInt()
            if(col !in 0..2 || row !in 0..3)return
            val keys=arrayOf("1","2","3","4","5","6","7","8","9","⌫","0","OK")
            when(val key=keys[row*3+col]){
                "⌫"->if(pin.isNotEmpty())pin=pin.dropLast(1)
                "OK"->if(pin=="1234"){page="HUB";msg=""}else{msg="PIN invalid";pin=""}
                else->if(pin.length<6)pin+=key
            }
        }
    }
}
