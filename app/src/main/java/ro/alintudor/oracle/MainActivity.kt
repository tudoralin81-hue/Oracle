package ro.alintudor.oracle

import android.app.Activity
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RadialGradient
import android.graphics.Shader
import android.graphics.Typeface
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.widget.*
import ro.alintudor.oracle.core.OracleBootstrap
import ro.alintudor.oracle.core.OracleLocalProcessor
import ro.alintudor.oracle.core.OracleRepository
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
        runCatching { OracleBootstrap.ensure(repository); showHub() }.onFailure { showFatalError("Pornirea Oracle a eșuat",it) }
    }

    private fun showHub() {
        currentModule=null; root.removeAllViews()
        val scroll=ScrollView(this).apply { isFillViewport=true; setBackgroundColor(Color.rgb(1,3,8)); clipToPadding=false }
        val tablet=resources.configuration.smallestScreenWidthDp>=600
        val page=LinearLayout(this).apply { orientation=LinearLayout.VERTICAL; setPadding(if(tablet) dp(18) else dp(10),dp(4),if(tablet) dp(18) else dp(10),dp(26)) }
        val hero=OracleHeroView(this){ openModule(it) }
        val heroRatio=if(tablet) .70f else .67f
        val heroHeightPx=(resources.displayMetrics.heightPixels*heroRatio).toInt().coerceAtLeast(if(tablet) dp(660) else dp(560))
        page.addView(hero,LinearLayout.LayoutParams(-1,heroHeightPx))
        val status=LinearLayout(this).apply { orientation=LinearLayout.HORIZONTAL; gravity=Gravity.CENTER_VERTICAL; setPadding(dp(15),dp(11),dp(15),dp(11)); setBackgroundColor(Color.rgb(7,11,22)) }
        status.addView(View(this).apply{setBackgroundColor(Color.rgb(50,235,125))},LinearLayout.LayoutParams(dp(8),dp(8)))
        status.addView(TextView(this).apply{text="  ORACLE READY";textSize=13f;typeface=Typeface.DEFAULT_BOLD;setTextColor(Color.WHITE)},LinearLayout.LayoutParams(0,-2,1f))
        status.addView(TextView(this).apply{text="LOCAL INTELLIGENCE";textSize=10f;typeface=Typeface.DEFAULT_BOLD;setTextColor(Color.rgb(125,140,165))})
        page.addView(status,LinearLayout.LayoutParams(-1,-2).apply{setMargins(0,dp(8),0,dp(8))})
        val metrics=LinearLayout(this).apply{orientation=LinearLayout.HORIZONTAL;gravity=Gravity.CENTER}
        metrics.addView(makeMetric("07","MODULE",Color.rgb(255,205,55)),LinearLayout.LayoutParams(0,dp(66),1f).apply{setMargins(0,0,dp(5),0)})
        metrics.addView(makeMetric("AI","LOCAL ENGINE",Color.rgb(55,205,255)),LinearLayout.LayoutParams(0,dp(66),1f).apply{setMargins(dp(2),0,dp(2),0)})
        metrics.addView(makeMetric("ON","SYSTEM",Color.rgb(145,245,45)),LinearLayout.LayoutParams(0,dp(66),1f).apply{setMargins(dp(5),0,0,0)})
        page.addView(metrics,LinearLayout.LayoutParams(-1,dp(66)).apply{setMargins(0,0,0,dp(8))})
        page.addView(makeHomeCard(8,"JURNAL ACTIVITATE",subtitles["journal"]!!,"journal"),LinearLayout.LayoutParams(-1,dp(82)).apply{setMargins(dp(2),0,dp(2),0)})
        page.addView(TextView(this).apply{text="ATINGE UN MODUL DIN HARTĂ PENTRU A-L DESCHIDE";textSize=10f;typeface=Typeface.DEFAULT_BOLD;letterSpacing=.08f;gravity=Gravity.CENTER;setTextColor(Color.rgb(82,95,118));setPadding(0,dp(12),0,0)})
        scroll.addView(page); root.addView(scroll,FrameLayout.LayoutParams(-1,-1))
    }

    private fun makeMetric(value:String,label:String,accent:Int)=LinearLayout(this).apply{
        orientation=LinearLayout.VERTICAL;gravity=Gravity.CENTER;setPadding(dp(6),dp(6),dp(6),dp(6));setBackgroundColor(Color.rgb(7,11,22));elevation=dp(1).toFloat()
        addView(TextView(this@MainActivity).apply{text=value;textSize=17f;typeface=Typeface.DEFAULT_BOLD;gravity=Gravity.CENTER;setTextColor(accent)})
        addView(TextView(this@MainActivity).apply{text=label;textSize=8f;typeface=Typeface.DEFAULT_BOLD;letterSpacing=.08f;gravity=Gravity.CENTER;setTextColor(Color.rgb(135,145,165))})
    }

    private fun makeHomeCard(number:Int,label:String,description:String,key:String)=LinearLayout(this).apply{
        orientation=LinearLayout.HORIZONTAL;gravity=Gravity.CENTER_VERTICAL;setPadding(dp(16),dp(10),dp(14),dp(10));setBackgroundColor(Color.rgb(8,12,24));isClickable=true;isFocusable=true;elevation=dp(2).toFloat();setOnClickListener{openModule(key)}
        addView(TextView(this@MainActivity).apply{text="%02d".format(number);textSize=11f;typeface=Typeface.DEFAULT_BOLD;setTextColor(Color.rgb(100,155,235))},LinearLayout.LayoutParams(dp(34),-2))
        addView(LinearLayout(this@MainActivity).apply{orientation=LinearLayout.VERTICAL;addView(TextView(this@MainActivity).apply{text=label;textSize=17f;typeface=Typeface.DEFAULT_BOLD;setTextColor(Color.WHITE)});addView(TextView(this@MainActivity).apply{text=description;textSize=12f;setTextColor(Color.rgb(165,172,190))})},LinearLayout.LayoutParams(0,-2,1f))
        addView(TextView(this@MainActivity).apply{text="›";textSize=26f;gravity=Gravity.CENTER;setTextColor(Color.rgb(125,140,165))},LinearLayout.LayoutParams(dp(28),dp(40)))
    }
    private fun dp(v:Int)= (v*resources.displayMetrics.density).toInt()

    private fun openModule(key:String){
        currentModule=key;runCatching{renderModule(key,false)}.onFailure{showModuleError(key,it)}
        Thread{val result=runCatching{OracleLocalProcessor.refresh(repository)};mainHandler.post{if(currentModule!=key||isFinishing)return@post;result.onSuccess{runCatching{renderModule(key,false)}.onFailure{showModuleError(key,it)}}.onFailure{e->Toast.makeText(this,"Refresh local eșuat: ${e.message?:e.javaClass.simpleName}",Toast.LENGTH_LONG).show()}}}.start()
    }
    private fun renderModule(key:String,refresh:Boolean=false){
        root.removeAllViews();val host=OracleNativeModule(this,titles[key]?:key.uppercase()){showHub()};root.addView(host.root,FrameLayout.LayoutParams(-1,-1));val data=if(refresh)OracleLocalProcessor.refresh(repository)else repository.snapshot()
        when(key){"portfolio"->OraclePortfolioModule(host).render(data.positions);"alerts"->OracleAlertsModule(host).render(data.alerts);"news"->OracleNewsModule(host).render(data.news);"journal"->OracleJournalModule(host).render(data.journal,data.history,data.alerts);"growth","analysis","watchlist","knowledge"->OracleSimpleModule(host,titles[key]?:key.uppercase()).render(actions=data.actions,knowledge=data.knowledge,positions=data.positions,history=data.history)}
    }
    private fun showModuleError(key:String,error:Throwable){root.removeAllViews();val box=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL;gravity=Gravity.CENTER;setPadding(dp(32),dp(32),dp(32),dp(32));setBackgroundColor(Color.rgb(2,4,10))};box.addView(TextView(this).apply{text="ORACLE  •  ${titles[key]?:key.uppercase()}";textSize=22f;gravity=Gravity.CENTER;setTextColor(Color.WHITE)});box.addView(TextView(this).apply{text="Modulul nu s-a putut încărca.\n\n${error.message?:error.javaClass.simpleName}";textSize=16f;gravity=Gravity.CENTER;setTextColor(Color.LTGRAY);setPadding(0,dp(24),0,dp(24))});box.addView(Button(this).apply{text="REÎNCEARCĂ";setOnClickListener{openModule(key)}});box.addView(Button(this).apply{text="ÎNAPOI LA ORACLE";setOnClickListener{showHub()}});root.addView(box,FrameLayout.LayoutParams(-1,-1))}
    private fun showFatalError(title:String,error:Throwable){root.removeAllViews();root.addView(TextView(this).apply{text="$title\n\n${error.message?:error.javaClass.simpleName}\n\nAplicația nu va rămâne blocată pe loading.";textSize=17f;gravity=Gravity.CENTER;setTextColor(Color.WHITE);setPadding(dp(32),dp(32),dp(32),dp(32))},FrameLayout.LayoutParams(-1,-1))}
    @Suppress("DEPRECATION") override fun onBackPressed(){if(currentModule!=null)showHub()else super.onBackPressed()}
}

private class OracleHeroView(context:android.content.Context,private val onModule:(String)->Unit):View(context){
    private val p=Paint(Paint.ANTI_ALIAS_FLAG)
    private var phase=0f
    private val nodes=listOf(
        Node("portfolio","PORTFOLIO",Color.rgb(190,60,255),.50f,.15f),Node("alerts","ALERTS",Color.rgb(255,70,35),.18f,.29f),Node("news","NEWS",Color.rgb(0,215,255),.82f,.29f),Node("growth","GROWTH",Color.rgb(145,245,35),.12f,.53f),Node("knowledge","KNOWLEDGE",Color.rgb(255,210,40),.88f,.53f),Node("analysis","ANALYSIS",Color.rgb(30,205,255),.28f,.76f),Node("watchlist","WATCHLIST",Color.rgb(255,220,35),.72f,.76f))
    private data class Node(val key:String,val label:String,val color:Int,val x:Float,val y:Float)

    init { setLayerType(View.LAYER_TYPE_SOFTWARE,null); isClickable=true }

    override fun onDraw(c:Canvas){
        val w=width.toFloat();val h=height.toFloat();val d=resources.displayMetrics.density;val base=minOf(w,h);val cx=w*.5f;val cy=h*.49f;val r=base*.215f;val nr=base*.105f
        c.drawColor(Color.rgb(1,2,6))
        p.shader=RadialGradient(cx,cy,base*.72f,intArrayOf(Color.rgb(17,20,38),Color.rgb(4,7,16),Color.rgb(1,2,6)),floatArrayOf(0f,.48f,1f),Shader.TileMode.CLAMP);c.drawRect(0f,0f,w,h,p);p.shader=null
        p.style=Paint.Style.FILL
        for(i in 0 until 82){val x=((i*83+37)%1000)/1000f*w;val y=((i*149+91)%1000)/1000f*h*.94f;val twinkle=.55f+.45f*((kotlin.math.sin(phase*.035f+i*.7f)+1f)*.5f);p.color=Color.argb((35+((i%6)*14)*twinkle).toInt(),255,205,80);c.drawCircle(x,y,(.55f+(i%3)*.45f)*d,p)}
        p.style=Paint.Style.STROKE
        for(i in 1..7){p.strokeWidth=if(i==1)2.0f*d else .75f*d;p.color=Color.argb(112-i*11,255,190,35);c.drawCircle(cx,cy,r*(1f+i*.40f),p)}
        p.strokeWidth=.8f*d
        for(n in nodes){
            val x=w*n.x;val y=h*n.y
            p.color=Color.argb(120,255,205,55);c.drawLine(cx,cy,x,y,p)
            val t=((phase*.018f+(n.x+n.y))*1.7f)%1f;val dx=cx+(x-cx)*t;val dy=cy+(y-cy)*t
            p.style=Paint.Style.FILL;p.color=n.color;c.drawCircle(dx,dy,2.0f*d,p)
            drawNode(c,x,y,nr,n,d)
            p.color=Color.rgb(255,205,55);c.drawCircle(x,y-nr*1.02f,2.4f*d,p)
            p.style=Paint.Style.STROKE;p.strokeWidth=.7f*d;p.color=Color.argb(80,Color.red(n.color),Color.green(n.color),Color.blue(n.color));c.drawCircle(x,y,nr*1.13f,p)
        }

        val pulse=1f+.018f*((kotlin.math.sin(phase*.055f)+1f)*.5f)
        p.style=Paint.Style.FILL;p.shadowLayer=base*.055f,0f,0f,Color.argb(115,255,205,45);p.shader=LinearGradient(cx-r,cy-r,cx+r,cy+r,Color.rgb(255,230,100),Color.rgb(224,140,8),Shader.TileMode.CLAMP);c.drawCircle(cx,cy,r*1.10f*pulse,p);p.clearShadowLayer();p.shader=null;p.color=Color.rgb(4,6,12);c.drawCircle(cx,cy,r*.98f,p)
        p.style=Paint.Style.STROKE;p.strokeWidth=1.3f*d;p.color=Color.argb(205,255,210,60);c.drawCircle(cx,cy,r*.90f,p);p.strokeWidth=.9f*d;p.color=Color.argb(115,255,190,35);c.drawCircle(cx,cy,r*1.28f,p)
        p.strokeWidth=1.0f*d;p.color=Color.argb(100,255,210,60);c.drawArc(cx-r*1.48f,cy-r*1.48f,cx+r*1.48f,cy+r*1.48f,phase*.35f,68f,false,p)
        val chart=Path();chart.moveTo(cx-r*.66f,cy+r*.57f);val pts=arrayOf(.00f to .08f,.10f to .02f,.20f to .16f,.30f to .05f,.40f to .20f,.50f to .12f,.60f to .34f,.70f to .22f,.80f to .45f,.90f to .39f,1f to .64f);for((x,y)in pts)chart.lineTo(cx-r*.66f+r*1.32f*x,cy+r*.57f-r*.40f*y);p.color=Color.rgb(255,195,35);p.strokeWidth=1.6f*d;c.drawPath(chart,p)

        p.textAlign=Paint.Align.CENTER;p.typeface=Typeface.create(Typeface.SERIF,Typeface.BOLD);p.color=Color.WHITE;p.textSize=r*.29f;c.drawText("ORACLE",cx,cy+r*.10f,p);p.textSize=r*.105f;p.color=Color.rgb(255,205,65);c.drawText("STOCK INTELLIGENCE",cx,cy+r*.32f,p);p.typeface=Typeface.DEFAULT_BOLD;p.textSize=r*.38f;p.color=Color.rgb(255,205,35);c.drawText("↗",cx,cy-r*.17f,p)

        p.style=Paint.Style.STROKE;p.strokeWidth=1.4f*d;p.color=Color.rgb(125,100,35);c.drawRoundRect(4*d,10*d,48*d,54*d,10*d,10*d,p);c.drawRoundRect(w-48*d,10*d,w-4*d,54*d,10*d,10*d,p)
        p.strokeWidth=2.2f*d;for(i in 0..2){val yy=(25+i*7)*d;c.drawLine(15*d,yy,37*d,yy,p)};c.drawArc(w-37*d,18*d,w-15*d,40*d,-55f,285f,false,p);c.drawLine(w-15*d,18*d,w-15*d,26*d,p)
        p.style=Paint.Style.FILL;p.textSize=base*.034f;p.color=Color.WHITE;c.drawText("ORACLE",cx,base*.055f,p);p.textSize=base*.018f;p.color=Color.rgb(170,150,90);c.drawText("STOCK INTELLIGENCE",cx,base*.082f,p)
        phase+=1f
        postInvalidateDelayed(55)
    }

    private fun drawNode(c:Canvas,x:Float,y:Float,rad:Float,n:Node,d:Float){
        p.style=Paint.Style.FILL;p.shadowLayer=rad*.24f,0f,0f,Color.argb(105,Color.red(n.color),Color.green(n.color),Color.blue(n.color));p.color=Color.argb(245,3,6,13);c.drawCircle(x,y,rad,p);p.clearShadowLayer();p.style=Paint.Style.STROKE;p.strokeWidth=2.0f*d;p.color=n.color;c.drawCircle(x,y,rad,p);p.style=Paint.Style.FILL;p.color=n.color;c.drawCircle(x,y-rad*.72f,rad*.038f,p)
        drawIcon(c,x,y-rad*.25f,rad*.29f,n.key,n.color,d)
        p.textAlign=Paint.Align.CENTER;p.typeface=Typeface.DEFAULT_BOLD;p.textSize=rad*.27f;p.color=n.color;c.drawText(n.label,x,y+rad*.17f,p);p.textSize=rad*.105f;p.color=Color.WHITE
        val desc=when(n.key){"portfolio"->"Performanță și poziții";"alerts"->"Semnale și evenimente";"news"->"Știri financiare";"growth"->"Acțiuni cu potențial";"knowledge"->"Idei și documentație";"analysis"->"Analiză detaliată";else->"Acțiuni favorite"};c.drawText(desc,x,y+rad*.42f,p);p.textSize=rad*.28f;c.drawText("›",x,y+rad*.73f,p)
    }
    private fun drawIcon(c:Canvas,x:Float,y:Float,s:Float,key:String,color:Int,d:Float){
        p.style=Paint.Style.STROKE;p.strokeWidth=1.8f*d;p.strokeCap=Paint.Cap.ROUND;p.strokeJoin=Paint.Join.ROUND;p.color=color
        when(key){
            "portfolio"->{c.drawCircle(x,y,s*.62f,p);c.drawLine(x,y,x,y-s*.62f,p);c.drawLine(x,y,x+s*.48f,y+s*.28f,p)}
            "alerts"->{c.drawArc(x-s*.48f,y-s*.35f,x+s*.48f,y+s*.42f,205f,130f,false,p);c.drawLine(x-s*.58f,y+s*.42f,x+s*.58f,y+s*.42f,p);c.drawCircle(x,y+s*.62f,s*.07f,p)}
            "news"->{c.drawRect(x-s*.58f,y-s*.55f,x+s*.58f,y+s*.55f,p);c.drawLine(x-s*.35f,y-s*.20f,x+s*.35f,y-s*.20f,p);c.drawLine(x-s*.35f,y,x+s*.35f,y,p);c.drawLine(x-s*.35f,y+s*.20f,x+s*.18f,y+s*.20f,p)}
            "growth"->{val q=Path();q.moveTo(x-s*.58f,y+s*.18f);q.lineTo(x-s*.10f,y-s*.28f);q.lineTo(x+s*.08f,y-s*.05f);q.lineTo(x+s*.58f,y-s*.52f);c.drawPath(q,p);c.drawLine(x+s*.30f,y-s*.52f,x+s*.58f,y-s*.52f,p);c.drawLine(x+s*.58f,y-s*.52f,x+s*.58f,y-s*.25f,p)}
            "knowledge"->{c.drawRect(x-s*.58f,y-s*.52f,x-.03f,y+s*.52f,p);c.drawRect(x+.03f,y-s*.52f,x+s*.58f,y+s*.52f,p);c.drawLine(x,y-s*.52f,x,y+s*.52f,p)}
            "analysis"->{c.drawLine(x-s*.58f,y+s*.45f,x-s*.58f,y-s*.48f,p);c.drawLine(x-s*.58f,y+s*.45f,x+s*.58f,y+s*.45f,p);val q=Path();q.moveTo(x-s*.48f,y+s*.20f);q.lineTo(x-s*.15f,y-s*.10f);q.lineTo(x+s*.08f,y+s*.04f);q.lineTo(x+s*.48f,y-s*.40f);c.drawPath(q,p)}
            "watchlist"->{c.drawOval(x-s*.65f,y-s*.35f,x+s*.65f,y+s*.35f,p);c.drawCircle(x,y,s*.16f,p)}
        }
        p.strokeCap=Paint.Cap.BUTT
    }
    override fun onTouchEvent(e:MotionEvent):Boolean{if(e.action!=MotionEvent.ACTION_UP)return true;val w=width.toFloat();val h=height.toFloat();val hit=minOf(w,h)*.125f;for(n in nodes){val dx=e.x-w*n.x;val dy=e.y-h*n.y;if(dx*dx+dy*dy<=hit*hit){onModule(n.key);performClick();return true}};return true}
    override fun performClick():Boolean{super.performClick();return true}
}
