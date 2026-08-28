package ro.alintudor.oracle

import android.app.Activity
import android.os.Bundle
import android.graphics.*
import android.graphics.drawable.ColorDrawable
import android.view.*
import android.widget.*
import kotlin.math.min

class MainActivity : Activity() {
    private lateinit var view: OracleView
    override fun onCreate(savedInstanceState: Bundle?) { super.onCreate(savedInstanceState); view = OracleView(); setContentView(view) }

    inner class OracleView : View(this@MainActivity) {
        private val bg = Color.rgb(2,4,10); private val panel = Color.rgb(10,14,24); private val text = Color.rgb(235,239,248)
        private val muted = Color.rgb(127,141,170); private val accent = Color.rgb(139,124,255); private val green = Color.rgb(54,211,153); private val red = Color.rgb(248,113,113)
        private val p = Paint(Paint.ANTI_ALIAS_FLAG); private var page = "PIN"; private var pin = ""; private var msg = ""
        private val positions = mutableListOf("NVDA" to 14.8, "VRT" to 12.4, "RZLV" to 8.7, "PLTR" to 7.9, "SOUN" to 5.8)
        private val alerts = mutableListOf("VRT — trailing stop approaching", "RZLV — momentum weakening", "NVDA — profit target reached")
        private val journal = mutableListOf("BUY NVDA · 10 shares", "BUY VRT · 20 shares", "SELL CRSP · position closed")
        private val watch = mutableListOf("NVDA","VRT","RZLV","PLTR","SOUN","AMD")

        init { p.typeface = Typeface.create("sans", Typeface.NORMAL); setBackgroundColor(bg) }
        private fun txt(c:Canvas,s:String,x:Float,y:Float,size:Float,color:Int=text,bold:Boolean=false){ p.color=color;p.textSize=size;p.typeface=Typeface.create("sans",if(bold) Typeface.BOLD else Typeface.NORMAL);c.drawText(s,x,y,p) }
        private fun box(c:Canvas,l:Float,t:Float,r:Float,b:Float,color:Int=panel,rad:Float=24f){p.color=color;c.drawRoundRect(l,t,r,b,rad,rad,p)}
        private fun center(c:Canvas,s:String,x:Float,y:Float,size:Float,color:Int=text,bold:Boolean=true){p.textSize=size;p.typeface=Typeface.create("sans",if(bold) Typeface.BOLD else Typeface.NORMAL);p.color=color;c.drawText(s,x-p.measureText(s)/2,y,p)}

        override fun onDraw(c:Canvas){ super.onDraw(c); c.drawColor(bg); if(page=="PIN") drawPin(c) else drawPage(c) }
        private fun drawPin(c:Canvas){
            center(c,"ORACLE",width/2f,145f,42f,accent,true); center(c,"AI STOCK ORACLE",width/2f,180f,14f,muted,false)
            box(c,width/2f-155,240f,width/2f+155,500f,Color.rgb(8,12,21),30f); center(c,"Introdu PIN",width/2f,285f,20f,text,true)
            center(c,if(pin.isEmpty()) "••••" else "•".repeat(pin.length),width/2f,350f,34f,text,true); center(c,msg,width/2f,390f,13f,red,false)
            val keys=arrayOf("1","2","3","4","5","6","7","8","9","⌫","0","OK"); for(i in keys.indices){ val col=i%3;val row=i/3;val x=width/2f-95+col*95;val y=440+row*62;box(c,x-36,y-28,x+36,y+28,Color.rgb(18,24,38),18f);center(c,keys[i],x,y+8,18f,text,true)}
        }
        private fun drawHeader(c:Canvas,title:String){ txt(c,"ORACLE",28f,46f,22f,accent,true); txt(c,title,28f,80f,28f,text,true); txt(c,"Standalone · no WordPress",28f,102f,12f,muted,false) }
        private fun drawPage(c:Canvas){
            drawHeader(c,pageTitle()); if(page=="HUB") drawHub(c) else if(page=="PORTFOLIO") drawPortfolio(c) else if(page=="ALERTS") drawAlerts(c) else if(page=="JOURNAL") drawJournal(c) else if(page=="NEWS") drawNews(c) else if(page=="GROWTH") drawGrowth(c) else if(page=="ANALYSIS") drawAnalysis(c) else if(page=="WATCHLIST") drawWatch(c)
            drawNav(c)
        }
        private fun pageTitle()=when(page){"HUB"->"AI Stock Oracle";"PORTFOLIO"->"Portfolio";"ALERTS"->"SELL Alerts";"JOURNAL"->"Jurnal activitate";"NEWS"->"News";"GROWTH"->"Growth";"ANALYSIS"->"Analysis";"WATCHLIST"->"Watchlist";else->"Oracle"}
        private fun drawHub(c:Canvas){
            val cx=width/2f; val cy=height/2f-50; val radius=min(width,height)*.30f; p.strokeWidth=2f;p.style=Paint.Style.STROKE;p.color=Color.rgb(37,46,67);c.drawCircle(cx,cy,radius,p);p.style=Paint.Style.FILL
            node(c,cx,cy-radius,"Portfolio","PORTFOLIO",accent);node(c,cx-radius*.9f,cy-radius*.38f,"Alerts","ALERTS",red);node(c,cx+radius*.9f,cy-radius*.38f,"News","NEWS",green);node(c,cx-radius*.8f,cy+radius*.55f,"Growth","GROWTH",accent);node(c,cx+radius*.8f,cy+radius*.55f,"Watchlist","WATCHLIST",accent);node(c,cx-radius*.32f,cy+radius*.92f,"Analysis","ANALYSIS",green);node(c,cx+radius*.32f,cy+radius*.92f,"Journal","JOURNAL",accent); center(c,"ORACLE",cx,cy+8,30f,text,true);center(c,"AI STOCK",cx,cy+32,13f,muted,false)
        }
        private fun node(c:Canvas,x:Float,y:Float,label:String,target:String,color:Int){p.color=color;c.drawCircle(x,y,48f,p);center(c,label,x,y+5,12f,Color.WHITE,true)}
        private fun drawPortfolio(c:Canvas){ var y=145f; box(c,20f,y-35,width-20,y+52);txt(c,"TOTAL PORTFOLIO",38f,y,12f,muted,true);txt(c,"$128,420",38f,y+31,28f,text,true);txt(c,"+18.6%",width-110,y+25,18f,green,true); y+=90; positions.forEach{(t,v)->box(c,20f,y-25,width-20,y+55);txt(c,t,38f,y+8,19f,text,true);txt(c,"Weight ${"%.1f".format(v)}%",38f,y+34,12f,muted,false);txt(c,if(v>10)"HOLD" else "WATCH",width-110,y+18,12f,if(v>10)green else accent,true);y+=82} }
        private fun drawAlerts(c:Canvas){ var y=145f; alerts.forEachIndexed{idx,s->box(c,20f,y-32,width-20,y+58);p.color=if(idx==2)green else red;c.drawCircle(45f,y+8,8f,p);txt(c,s,65f,y+4,15f,text,true);txt(c,"Signal engine · 08:25",65f,y+30,11f,muted,false);y+=100}; button(c,"RUN ALERT SCAN",20f,y,width-20,y+58) }
        private fun drawJournal(c:Canvas){ var y=145f; journal.forEachIndexed{idx,s->box(c,20f,y-30,width-20,y+52);txt(c,"08:2${idx}",38f,y-3,11f,muted,false);txt(c,s,105f,y+4,14f,text,true);y+=88}; button(c,"EXPORT JOURNAL",20f,y,width-20,y+58) }
        private fun drawNews(c:Canvas){ val titles=arrayOf("US yields and equities in focus","AI infrastructure spending accelerates","Semiconductors lead risk appetite","Markets await inflation signals");var y=145f;titles.forEach{t->box(c,20f,y-30,width-20,y+72);txt(c,"ECONOMY",38f,y,10f,accent,true);txt(c,t,38f,y+28,16f,text,true);txt(c,"Updated today · breaking monitor",38f,y+52,11f,muted,false);y+=112} }
        private fun drawGrowth(c:Canvas){ box(c,20f,140f,width-20,285f);txt(c,"12-MONTH FORECAST",38f,175f,12f,muted,true);txt(c,"$161,900",38f,218f,34f,text,true);txt(c,"+26.0% projected",38f,250f,14f,green,true);txt(c,"Scenario",width-130,175f,11f,muted,false);txt(c,"Base",width-130,202f,14f,text,true);txt(c,"Bull",width-130,230f,14f,green,true);txt(c,"Bear",width-130,258f,14f,red,true); for(i in 0..5){p.color=Color.rgb(35,45,65);c.drawRect(40f+i*58,400f-i*28,82f+i*58,420f,p)} }
        private fun drawAnalysis(c:Canvas){box(c,20f,140f,width-20,205f);txt(c,"Ticker",38f,178f,12f,muted,false);txt(c,"NVDA",105f,178f,17f,text,true);txt(c,"BULLISH",width-120,178f,13f,green,true);val rows=arrayOf("Trend" to "Above EMA 20/50","Momentum" to "Positive","Support" to "$172.00","Resistance" to "$184.50","Risk" to "Medium");var y=245f;rows.forEach{(a,b)->txt(c,a,38f,y,13f,muted,false);txt(c,b,width/2f,y,14f,text,true);y+=42} }
        private fun drawWatch(c:Canvas){var y=145f;watch.forEach{t->box(c,20f,y-30,width-20,y+48);txt(c,t,38f,y+3,18f,text,true);txt(c,"Live watch · signal ready",110f,y+3,12f,muted,false);y+=82};button(c,"+ ADD TICKER",20f,y,width-20,y+58)}
        private fun button(c:Canvas,s:String,l:Float,t:Float,r:Float,b:Float){box(c,l,t,r,b,Color.rgb(30,36,55),18f);center(c,s,(l+r)/2,t+37,13f,accent,true)}
        private fun drawNav(c:Canvas){val y=height-64f; p.color=Color.rgb(7,10,18);c.drawRect(0f,y,width,height,p);val items=arrayOf("HUB","PORTFOLIO","ALERTS","JOURNAL","WATCHLIST");items.forEachIndexed{i,s->center(c,s,width*(i+.5f)/5f,y+38,10f,if(page==s)accent else muted,true)} }
        override fun onTouchEvent(e:MotionEvent):Boolean{if(e.action!=MotionEvent.ACTION_UP)return true;val x=e.x;val y=e.y
            if(page=="PIN"){ handlePin(x,y);invalidate();return true }
            if(y>height-90){val i=(x/(width/5)).toInt();page=arrayOf("HUB","PORTFOLIO","ALERTS","JOURNAL","WATCHLIST")[i.coerceIn(0,4)];invalidate();return true}
            if(page=="HUB"){val cx=width/2f;val cy=height/2f-50;val radius=min(width,height)*.30f; val dx=x-cx;val dy=y-cy; if(dy < -radius*.65){page="PORTFOLIO"} else if(dx < -radius*.6 && dy < 0){page="ALERTS"} else if(dx > radius*.6 && dy < 0){page="NEWS"} else if(dx < -radius*.45 && dy > 0){page="GROWTH"} else if(dx > radius*.45 && dy > 0){page="WATCHLIST"} else if(dy>radius*.55){page="ANALYSIS"} }
            invalidate();return true }
        private fun handlePin(x:Float,y:Float){if(y<400)return;val col=((x-width/2f+95)/95).toInt();val row=((y-440)/62).toInt();if(col !in 0..2||row !in 0..3)return;val k=arrayOf("1","2","3","4","5","6","7","8","9","⌫","0","OK")[row*3+col];when(k){"⌫"->if(pin.isNotEmpty())pin=pin.dropLast(1);"OK"->if(pin=="1234"){page="HUB";msg=""}else{msg="PIN invalid";pin=""};else->if(pin.length<6)pin+=k}}
    }
}
