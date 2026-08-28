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

    inner class OracleView : View(this@MainActivity) {
        private val bg = Color.rgb(2, 4, 10)
        private val panel = Color.rgb(10, 14, 24)
        private val text = Color.rgb(235, 239, 248)
        private val muted = Color.rgb(127, 141, 170)
        private val accent = Color.rgb(139, 124, 255)
        private val green = Color.rgb(54, 211, 153)
        private val red = Color.rgb(248, 113, 113)
        private val p = Paint(Paint.ANTI_ALIAS_FLAG)
        private var page = "PIN"
        private var pin = ""
        private var msg = ""
        private val positions = listOf("NVDA" to 14.8f, "VRT" to 12.4f, "RZLV" to 8.7f, "PLTR" to 7.9f, "SOUN" to 5.8f)
        private val alerts = listOf("VRT — trailing stop approaching", "RZLV — momentum weakening", "NVDA — profit target reached")
        private val journal = listOf("BUY NVDA · 10 shares", "BUY VRT · 20 shares", "SELL CRSP · position closed")
        private val watch = listOf("NVDA", "VRT", "RZLV", "PLTR", "SOUN", "AMD")

        init { setBackgroundColor(bg) }

        private fun txt(c: Canvas, s: String, x: Float, y: Float, size: Float, color: Int = text, bold: Boolean = false) {
            p.color = color; p.textSize = size
            p.typeface = Typeface.create("sans", if (bold) Typeface.BOLD else Typeface.NORMAL)
            c.drawText(s, x, y, p)
        }
        private fun center(c: Canvas, s: String, x: Float, y: Float, size: Float, color: Int = text, bold: Boolean = true) {
            p.color = color; p.textSize = size
            p.typeface = Typeface.create("sans", if (bold) Typeface.BOLD else Typeface.NORMAL)
            c.drawText(s, x - p.measureText(s) / 2.0f, y, p)
        }
        private fun box(c: Canvas, l: Float, t: Float, r: Float, b: Float, color: Int = panel, rad: Float = 24.0f) {
            p.color = color; p.style = Paint.Style.FILL
            c.drawRoundRect(l, t, r, b, rad, rad, p)
        }

        override fun onDraw(c: Canvas) {
            c.drawColor(bg)
            if (page == "PIN") drawPin(c) else drawPage(c)
        }

        private fun drawPin(c: Canvas) {
            val cx = width.toFloat() / 2.0f
            center(c, "ORACLE", cx, 145.0f, 42.0f, accent, true)
            center(c, "AI STOCK ORACLE", cx, 180.0f, 14.0f, muted, false)
            box(c, cx - 155.0f, 240.0f, cx + 155.0f, 500.0f, Color.rgb(8, 12, 21), 30.0f)
            center(c, "Introdu PIN", cx, 285.0f, 20.0f, text, true)
            center(c, if (pin.isEmpty()) "••••" else "•".repeat(pin.length), cx, 350.0f, 34.0f, text, true)
            center(c, msg, cx, 390.0f, 13.0f, red, false)
            val keys = arrayOf("1","2","3","4","5","6","7","8","9","⌫","0","OK")
            for (i in keys.indices) {
                val col = i % 3; val row = i / 3
                val x = cx - 95.0f + col.toFloat() * 95.0f
                val y = 440.0f + row.toFloat() * 62.0f
                box(c, x - 36.0f, y - 28.0f, x + 36.0f, y + 28.0f, Color.rgb(18,24,38), 18.0f)
                center(c, keys[i], x, y + 8.0f, 18.0f, text, true)
            }
        }

        private fun drawHeader(c: Canvas, title: String) {
            txt(c, "ORACLE", 28.0f, 46.0f, 22.0f, accent, true)
            txt(c, title, 28.0f, 80.0f, 28.0f, text, true)
            txt(c, "Standalone · no WordPress", 28.0f, 102.0f, 12.0f, muted, false)
        }
        private fun drawPage(c: Canvas) {
            drawHeader(c, pageTitle())
            when (page) {
                "HUB" -> drawHub(c); "PORTFOLIO" -> drawPortfolio(c); "ALERTS" -> drawAlerts(c)
                "JOURNAL" -> drawJournal(c); "NEWS" -> drawNews(c); "GROWTH" -> drawGrowth(c)
                "ANALYSIS" -> drawAnalysis(c); "WATCHLIST" -> drawWatch(c)
            }
            drawNav(c)
        }
        private fun pageTitle() = when(page) {
            "HUB" -> "AI Stock Oracle"; "PORTFOLIO" -> "Portfolio"; "ALERTS" -> "SELL Alerts"
            "JOURNAL" -> "Jurnal activitate"; "NEWS" -> "News"; "GROWTH" -> "Growth"
            "ANALYSIS" -> "Analysis"; "WATCHLIST" -> "Watchlist"; else -> "Oracle"
        }

        private fun drawHub(c: Canvas) {
            val cx = width.toFloat()/2.0f; val cy = height.toFloat()/2.0f - 50.0f
            val radius = min(width, height).toFloat()*0.30f
            p.color = Color.rgb(37,46,67); p.strokeWidth = 2.0f; p.style = Paint.Style.STROKE
            c.drawCircle(cx, cy, radius, p); p.style = Paint.Style.FILL
            node(c,cx,cy-radius,"Portfolio",accent); node(c,cx-radius*0.9f,cy-radius*0.38f,"Alerts",red)
            node(c,cx+radius*0.9f,cy-radius*0.38f,"News",green); node(c,cx-radius*0.8f,cy+radius*0.55f,"Growth",accent)
            node(c,cx+radius*0.8f,cy+radius*0.55f,"Watchlist",accent); node(c,cx-radius*0.32f,cy+radius*0.92f,"Analysis",green)
            node(c,cx+radius*0.32f,cy+radius*0.92f,"Journal",accent)
            center(c,"ORACLE",cx,cy+8.0f,30.0f,text,true); center(c,"AI STOCK",cx,cy+32.0f,13.0f,muted,false)
        }
        private fun node(c: Canvas,x:Float,y:Float,label:String,color:Int) { p.color=color; c.drawCircle(x,y,48.0f,p); center(c,label,x,y+5.0f,12.0f,Color.WHITE,true) }

        private fun drawPortfolio(c: Canvas) {
            var y=145.0f; box(c,20.0f,y-35.0f,width.toFloat()-20.0f,y+52.0f)
            txt(c,"TOTAL PORTFOLIO",38.0f,y,12.0f,muted,true); txt(c,"$128,420",38.0f,y+31.0f,28.0f,text,true)
            txt(c,"+18.6%",width.toFloat()-110.0f,y+25.0f,18.0f,green,true); y+=90.0f
            positions.forEach { (ticker,weight) ->
                box(c,20.0f,y-25.0f,width.toFloat()-20.0f,y+55.0f); txt(c,ticker,38.0f,y+8.0f,19.0f,text,true)
                txt(c,"Weight ${"%.1f".format(weight)}%",38.0f,y+34.0f,12.0f,muted,false)
                txt(c,if(weight>10.0f)"HOLD" else "WATCH",width.toFloat()-110.0f,y+18.0f,12.0f,if(weight>10.0f)green else accent,true); y+=82.0f
            }
        }
        private fun drawAlerts(c: Canvas) { var y=145.0f; alerts.forEachIndexed { i,item -> box(c,20.0f,y-32.0f,width.toFloat()-20.0f,y+58.0f); p.color=if(i==2)green else red; c.drawCircle(45.0f,y+8.0f,8.0f,p); txt(c,item,65.0f,y+4.0f,15.0f,text,true); txt(c,"Signal engine · 08:25",65.0f,y+30.0f,11.0f,muted,false); y+=100.0f }; button(c,"RUN ALERT SCAN",20.0f,y,width.toFloat()-20.0f,y+58.0f) }
        private fun drawJournal(c: Canvas) { var y=145.0f; journal.forEachIndexed { i,item -> box(c,20.0f,y-30.0f,width.toFloat()-20.0f,y+52.0f); txt(c,"08:2$i",38.0f,y-3.0f,11.0f,muted,false); txt(c,item,105.0f,y+4.0f,14.0f,text,true); y+=88.0f }; button(c,"EXPORT JOURNAL",20.0f,y,width.toFloat()-20.0f,y+58.0f) }
        private fun drawNews(c: Canvas) { val titles=arrayOf("US yields and equities in focus","AI infrastructure spending accelerates","Semiconductors lead risk appetite","Markets await inflation signals"); var y=145.0f; titles.forEach { title -> box(c,20.0f,y-30.0f,width.toFloat()-20.0f,y+72.0f); txt(c,"ECONOMY",38.0f,y,10.0f,accent,true); txt(c,title,38.0f,y+28.0f,16.0f,text,true); txt(c,"Updated today · breaking monitor",38.0f,y+52.0f,11.0f,muted,false); y+=112.0f } }
        private fun drawGrowth(c: Canvas) { box(c,20.0f,140.0f,width.toFloat()-20.0f,285.0f); txt(c,"12-MONTH FORECAST",38.0f,175.0f,12.0f,muted,true); txt(c,"$161,900",38.0f,218.0f,34.0f,text,true); txt(c,"+26.0% projected",38.0f,250.0f,14.0f,green,true); txt(c,"Scenario",width.toFloat()-130.0f,175.0f,11.0f,muted,false); txt(c,"Base",width.toFloat()-130.0f,202.0f,14.0f,text,true); txt(c,"Bull",width.toFloat()-130.0f,230.0f,14.0f,green,true); txt(c,"Bear",width.toFloat()-130.0f,258.0f,14.0f,red,true); for(i in 0..5){val x=40.0f+i.toFloat()*58.0f; val top=400.0f-i.toFloat()*28.0f; p.color=Color.rgb(35,45,65); c.drawRect(x,top,x+42.0f,top+20.0f,p)} }
        private fun drawAnalysis(c: Canvas) { box(c,20.0f,140.0f,width.toFloat()-20.0f,205.0f); txt(c,"Ticker",38.0f,178.0f,12.0f,muted,false); txt(c,"NVDA",105.0f,178.0f,17.0f,text,true); txt(c,"BULLISH",width.toFloat()-120.0f,178.0f,13.0f,green,true); val rows=arrayOf("Trend" to "Above EMA 20/50","Momentum" to "Positive","Support" to "$172.00","Resistance" to "$184.50","Risk" to "Medium"); var y=245.0f; rows.forEach{(a,b)->txt(c,a,38.0f,y,13.0f,muted,false);txt(c,b,width.toFloat()/2.0f,y,14.0f,text,true);y+=42.0f} }
        private fun drawWatch(c: Canvas) { var y=145.0f; watch.forEach{ticker->box(c,20.0f,y-30.0f,width.toFloat()-20.0f,y+48.0f);txt(c,ticker,38.0f,y+3.0f,18.0f,text,true);txt(c,"Live watch · signal ready",110.0f,y+3.0f,12.0f,muted,false);y+=82.0f};button(c,"+ ADD TICKER",20.0f,y,width.toFloat()-20.0f,y+58.0f) }
        private fun button(c:Canvas,label:String,l:Float,t:Float,r:Float,b:Float){box(c,l,t,r,b,Color.rgb(30,36,55),18.0f);center(c,label,(l+r)/2.0f,t+37.0f,13.0f,accent,true)}
        private fun drawNav(c:Canvas){val y=height.toFloat()-64.0f;p.color=Color.rgb(7,10,18);c.drawRect(0.0f,y,width.toFloat(),height.toFloat(),p);val items=arrayOf("HUB","PORTFOLIO","ALERTS","JOURNAL","WATCHLIST");items.forEachIndexed{i,item->center(c,item,width.toFloat()*(i.toFloat()+0.5f)/5.0f,y+38.0f,10.0f,if(page==item)accent else muted,true)}}

        override fun onTouchEvent(e: MotionEvent): Boolean {
            if(e.action!=MotionEvent.ACTION_UP)return true
            val x=e.x;val y=e.y
            if(page=="PIN"){handlePin(x,y);invalidate();return true}
            if(y>height.toFloat()-90.0f){val index=(x/(width.toFloat()/5.0f)).toInt().coerceIn(0,4);page=arrayOf("HUB","PORTFOLIO","ALERTS","JOURNAL","WATCHLIST")[index];invalidate();return true}
            if(page=="HUB"){
                val cx=width.toFloat()/2.0f;val cy=height.toFloat()/2.0f-50.0f;val radius=min(width,height).toFloat()*0.30f;val dx=x-cx;val dy=y-cy
                page=when{dy < -radius*0.65f->"PORTFOLIO";dx < -radius*0.60f&&dy<0.0f->"ALERTS";dx>radius*0.60f&&dy<0.0f->"NEWS";dx<-radius*0.45f&&dy>0.0f->"GROWTH";dx>radius*0.45f&&dy>0.0f->"WATCHLIST";dy>radius*0.55f->"ANALYSIS";else->page}
            }
            invalidate();return true
        }
        private fun handlePin(x:Float,y:Float){if(y<400.0f)return;val cx=width.toFloat()/2.0f;val col=((x-cx+95.0f)/95.0f).toInt();val row=((y-440.0f)/62.0f).toInt();if(col !in 0..2||row !in 0..3)return;val keys=arrayOf("1","2","3","4","5","6","7","8","9","⌫","0","OK");when(val key=keys[row*3+col]){"⌫"->if(pin.isNotEmpty())pin=pin.dropLast(1);"OK"->if(pin=="1234"){page="HUB";msg=""}else{msg="PIN invalid";pin=""};else->if(pin.length<6)pin+=key}}
    }
}
