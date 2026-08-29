package ro.alintudor.oracle

import android.app.Activity
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
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
        window.statusBarColor = Color.rgb(1, 3, 8)
        window.navigationBarColor = Color.rgb(1, 3, 8)
        root = FrameLayout(this).apply { setBackgroundColor(Color.rgb(1, 3, 8)) }
        setContentView(root)
        runCatching { OracleBootstrap.ensure(repository); showHub() }.onFailure { showFatalError("Pornirea Oracle a eșuat", it) }
    }

    private fun showHub() {
        currentModule = null
        root.removeAllViews()
        val scroll = ScrollView(this).apply { isFillViewport = true; setBackgroundColor(Color.rgb(1, 3, 8)) }
        val page = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(10), dp(6), dp(10), dp(24)) }
        val hero = OracleHeroView(this) { openModule(it) }
        val heroSize = minOf(resources.displayMetrics.widthPixels - dp(20), dp(560))
        page.addView(hero, LinearLayout.LayoutParams(-1, heroSize))

        val status = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL; setPadding(dp(14), dp(11), dp(14), dp(11)); setBackgroundColor(Color.rgb(8, 12, 24)) }
        status.addView(View(this).apply { setBackgroundColor(Color.rgb(50, 220, 135)) }, LinearLayout.LayoutParams(dp(8), dp(8)))
        status.addView(TextView(this).apply { text = "  ORACLE READY"; textSize = 13f; typeface = Typeface.DEFAULT_BOLD; setTextColor(Color.WHITE) }, LinearLayout.LayoutParams(0, -2, 1f))
        status.addView(TextView(this).apply { text = "LOCAL INTELLIGENCE"; textSize = 10f; setTextColor(Color.rgb(140,150,170)) })
        page.addView(status, LinearLayout.LayoutParams(-1, -2).apply { setMargins(0, dp(8), 0, dp(8)) })
        page.addView(makeHomeCard(8, "JURNAL ACTIVITATE", subtitles["journal"]!!, "journal"), LinearLayout.LayoutParams(-1, dp(82)).apply { setMargins(dp(2), 0, dp(2), 0) })
        page.addView(TextView(this).apply { text = "Atinge un modul din hartă pentru a-l deschide"; textSize = 11f; gravity = Gravity.CENTER; setTextColor(Color.rgb(95,105,125)); setPadding(0, dp(12), 0, 0) })
        scroll.addView(page)
        root.addView(scroll, FrameLayout.LayoutParams(-1, -1))
    }

    private fun makeHomeCard(number: Int, label: String, description: String, key: String): LinearLayout = LinearLayout(this).apply {
        orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL; setPadding(dp(16), dp(10), dp(14), dp(10)); setBackgroundColor(Color.rgb(8,12,24)); isClickable = true; isFocusable = true; elevation = dp(2).toFloat(); setOnClickListener { openModule(key) }
        addView(TextView(this@MainActivity).apply { text = "%02d".format(number); textSize = 11f; typeface = Typeface.DEFAULT_BOLD; setTextColor(Color.rgb(100,155,235)) }, LinearLayout.LayoutParams(dp(34), -2))
        addView(LinearLayout(this@MainActivity).apply { orientation = LinearLayout.VERTICAL; addView(TextView(this@MainActivity).apply { text = label; textSize = 17f; typeface = Typeface.DEFAULT_BOLD; setTextColor(Color.WHITE) }); addView(TextView(this@MainActivity).apply { text = description; textSize = 12f; setTextColor(Color.rgb(165,172,190)) }) }, LinearLayout.LayoutParams(0,-2,1f))
        addView(TextView(this@MainActivity).apply { text = "›"; textSize = 26f; gravity = Gravity.CENTER; setTextColor(Color.rgb(125,140,165)) }, LinearLayout.LayoutParams(dp(28), dp(40)))
    }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()
    private fun openModule(key: String) {
        currentModule = key
        runCatching { renderModule(key, false) }.onFailure { showModuleError(key, it) }
        Thread { val result = runCatching { OracleLocalProcessor.refresh(repository) }; mainHandler.post { if (currentModule != key || isFinishing) return@post; result.onSuccess { runCatching { renderModule(key, false) }.onFailure { showModuleError(key, it) } }.onFailure { error -> Toast.makeText(this, "Refresh local eșuat: ${error.message ?: error.javaClass.simpleName}", Toast.LENGTH_LONG).show() } } }.start()
    }
    private fun renderModule(key: String, refresh: Boolean = false) {
        root.removeAllViews()
        val host = OracleNativeModule(this, titles[key] ?: key.uppercase()) { showHub() }
        root.addView(host.root, FrameLayout.LayoutParams(-1,-1))
        val data = if (refresh) OracleLocalProcessor.refresh(repository) else repository.snapshot()
        when(key) { "portfolio" -> OraclePortfolioModule(host).render(data.positions); "alerts" -> OracleAlertsModule(host).render(data.alerts); "news" -> OracleNewsModule(host).render(data.news); "journal" -> OracleJournalModule(host).render(data.journal,data.history,data.alerts); "growth","analysis","watchlist","knowledge" -> OracleSimpleModule(host,titles[key] ?: key.uppercase()).render(actions=data.actions,knowledge=data.knowledge,positions=data.positions,history=data.history) }
    }
    private fun showModuleError(key:String,error:Throwable) { root.removeAllViews(); val box=LinearLayout(this).apply { orientation=LinearLayout.VERTICAL; gravity=Gravity.CENTER; setPadding(dp(32),dp(32),dp(32),dp(32)); setBackgroundColor(Color.rgb(2,4,10)) }; box.addView(TextView(this).apply { text="ORACLE  •  ${titles[key] ?: key.uppercase()}"; textSize=22f; gravity=Gravity.CENTER; setTextColor(Color.WHITE) }); box.addView(TextView(this).apply { text="Modulul nu s-a putut încărca.\n\n${error.message ?: error.javaClass.simpleName}"; textSize=16f; gravity=Gravity.CENTER; setTextColor(Color.LTGRAY); setPadding(0,dp(24),0,dp(24)) }); box.addView(Button(this).apply { text="REÎNCEARCĂ"; setOnClickListener { openModule(key) } }); box.addView(Button(this).apply { text="ÎNAPOI LA ORACLE"; setOnClickListener { showHub() } }); root.addView(box,FrameLayout.LayoutParams(-1,-1)) }
    private fun showFatalError(title:String,error:Throwable) { root.removeAllViews(); root.addView(TextView(this).apply { text="$title\n\n${error.message ?: error.javaClass.simpleName}\n\nAplicația nu va rămâne blocată pe loading."; textSize=17f; gravity=Gravity.CENTER; setTextColor(Color.WHITE); setPadding(dp(32),dp(32),dp(32),dp(32)) },FrameLayout.LayoutParams(-1,-1)) }
    @Suppress("DEPRECATION") override fun onBackPressed() { if(currentModule != null) showHub() else super.onBackPressed() }
}

private class OracleHeroView(context: android.content.Context, private val onModule: (String) -> Unit) : View(context) {
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val nodes = listOf(
        Node("portfolio","PORTFOLIO",Color.rgb(190,60,255),.50f,.16f), Node("alerts","ALERTS",Color.rgb(255,70,35),.18f,.29f), Node("news","NEWS",Color.rgb(0,215,255),.82f,.29f), Node("growth","GROWTH",Color.rgb(145,245,35),.14f,.62f), Node("knowledge","KNOWLEDGE",Color.rgb(255,210,40),.86f,.62f), Node("analysis","ANALYSIS",Color.rgb(30,205,255),.28f,.86f), Node("watchlist","WATCHLIST",Color.rgb(255,220,35),.72f,.86f)
    )
    private data class Node(val key:String,val label:String,val color:Int,val x:Float,val y:Float)
    override fun onDraw(c:Canvas) {
        val w=width.toFloat(); val h=height.toFloat(); val cx=w*.5f; val cy=h*.53f; val r=minOf(w,h)*.235f
        c.drawColor(Color.rgb(1,2,6)); paint.shader=null; paint.style=Paint.Style.STROKE
        for(i in 1..5){ paint.strokeWidth=if(i==1)3f else 1.1f; paint.color=Color.argb(150-i*18,255,190,20); c.drawCircle(cx,cy,r*(1f+i*.48f),paint) }
        paint.color=Color.rgb(255,210,45); paint.strokeWidth=3.2f; c.drawCircle(cx,cy,r*1.08f,paint)
        for(n in nodes){ val nx=w*n.x; val ny=h*n.y; paint.color=Color.argb(180,255,205,45); paint.strokeWidth=1.4f; c.drawLine(cx,cy,nx,ny,paint); drawNode(c,nx,ny,minOf(w,h)*.115f,n) }
        paint.style=Paint.Style.FILL; paint.shader=LinearGradient(cx-r,cy-r,cx+r,cy+r,Color.rgb(255,220,70),Color.rgb(245,145,10),Shader.TileMode.CLAMP); c.drawCircle(cx,cy,r,paint); paint.shader=null; paint.color=Color.argb(238,0,3,12); c.drawCircle(cx,cy,r*.91f,paint)
        paint.textAlign=Paint.Align.CENTER; paint.typeface=Typeface.create(Typeface.SERIF,Typeface.BOLD); paint.color=Color.WHITE; paint.textSize=r*.27f; c.drawText("ORACLE",cx,cy+r*.10f,paint); paint.textSize=r*.105f; paint.color=Color.rgb(255,205,65); c.drawText("STOCK INTELLIGENCE",cx,cy+r*.32f,paint)
        paint.typeface=Typeface.DEFAULT_BOLD; paint.textSize=r*.40f; paint.color=Color.rgb(255,205,35); c.drawText("↗",cx,cy-r*.18f,paint)
        paint.textSize=minOf(w,h)*.032f; paint.color=Color.WHITE; c.drawText("ORACLE",cx,h*.035f,paint); paint.textSize=minOf(w,h)*.018f; paint.color=Color.rgb(150,155,175); c.drawText("PORTFOLIO INTELLIGENCE",cx,h*.065f,paint)
    }
    private fun drawNode(c:Canvas,x:Float,y:Float,rad:Float,n:Node){
        paint.style=Paint.Style.FILL; paint.color=Color.argb(245,3,5,12); c.drawCircle(x,y,rad,paint); paint.style=Paint.Style.STROKE; paint.strokeWidth=3.4f; paint.color=n.color; c.drawCircle(x,y,rad,paint); paint.style=Paint.Style.FILL; paint.color=n.color; c.drawCircle(x,y-rad*.72f,rad*.045f,paint)
        paint.textAlign=Paint.Align.CENTER; paint.typeface=Typeface.DEFAULT_BOLD; paint.textSize=rad*.34f; c.drawText(n.label,x,y+rad*.08f,paint); paint.textSize=rad*.115f; paint.color=Color.WHITE; val d=when(n.key){"portfolio"->"Performanță și poziții";"alerts"->"Semnale și evenimente";"news"->"Știri financiare";"growth"->"Acțiuni cu potențial";"knowledge"->"Idei și documentație";"analysis"->"Analiză detaliată";else->"Acțiuni favorite"}; c.drawText(d,x,y+rad*.34f,paint); paint.textSize=rad*.30f; c.drawText("›",x,y+rad*.70f,paint)
    }
    override fun onTouchEvent(e:MotionEvent):Boolean { if(e.action!=MotionEvent.ACTION_UP)return true; val w=width.toFloat(); val h=height.toFloat(); val hit=minOf(w,h)*.15f; for(n in nodes){val dx=e.x-w*n.x;val dy=e.y-h*n.y;if(dx*dx+dy*dy<=hit*hit){onModule(n.key);performClick();return true}};return true }
    override fun performClick():Boolean { super.performClick(); return true }
}
