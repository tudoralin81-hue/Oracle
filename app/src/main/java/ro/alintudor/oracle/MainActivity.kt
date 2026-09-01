package ro.alintudor.oracle

import android.app.Activity
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
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
import ro.alintudor.oracle.core.OracleGrowthLiveData
import ro.alintudor.oracle.core.OracleLocalProcessor
import ro.alintudor.oracle.core.OracleRepository
import ro.alintudor.oracle.core.OracleWatchlistStore
import ro.alintudor.oracle.core.OracleKnowledgeSync
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
        OracleKnowledgeSync.scheduleDaily(this)
        runCatching { OracleBootstrap.ensure(repository); showHub() }.onFailure { showFatalError("Pornirea Oracle a eșuat",it) }
    }

    private fun showHub() {
        currentModule=null
        root.removeAllViews()
        root.addView(PremiumStartView(this){ openModule(it) }, FrameLayout.LayoutParams(-1,-1))
    }
    private fun makeHomeCard(number:Int,label:String,description:String,key:String)=LinearLayout(this).apply{
        orientation=LinearLayout.HORIZONTAL;gravity=Gravity.CENTER_VERTICAL;setPadding(dp(16),dp(10),dp(14),dp(10));setBackgroundColor(Color.rgb(8,12,24));isClickable=true;isFocusable=true;elevation=dp(2).toFloat();setOnClickListener{openModule(key)}
        addView(TextView(this@MainActivity).apply{text="%02d".format(number);textSize=11f;typeface=Typeface.DEFAULT_BOLD;setTextColor(Color.rgb(100,155,235))},LinearLayout.LayoutParams(dp(34),-2))
        addView(LinearLayout(this@MainActivity).apply{orientation=LinearLayout.VERTICAL;addView(TextView(this@MainActivity).apply{text=label;textSize=17f;typeface=Typeface.DEFAULT_BOLD;setTextColor(Color.WHITE)});addView(TextView(this@MainActivity).apply{text=description;textSize=12f;setTextColor(Color.rgb(165,172,190))})},LinearLayout.LayoutParams(0,-2,1f))
        addView(TextView(this@MainActivity).apply{text="›";textSize=26f;gravity=Gravity.CENTER;setTextColor(Color.rgb(125,140,165))},LinearLayout.LayoutParams(dp(28),dp(40)))
    }
    private fun dp(v:Int)= (v*resources.displayMetrics.density).toInt()

    private fun openModule(key:String){
        currentModule=key
        runCatching{renderModule(key,false)}.onFailure{showModuleError(key,it)}
        if (key == "analysis") return
        if (key == "knowledge") {
            if (OracleKnowledgeSync.isStale(this)) {
                OracleKnowledgeSync.refreshAsync(this) { ok, error ->
                    if (currentModule != "knowledge" || isFinishing) return@refreshAsync
                    if (ok) runCatching { renderModule("knowledge", false) }.onFailure { showModuleError("knowledge", it) }
                    else if (error != null) Toast.makeText(this, "Knowledge refresh eșuat: $error", Toast.LENGTH_LONG).show()
                }
            }
            return
        }
        Thread{val result=runCatching{OracleLocalProcessor.refresh(repository)};mainHandler.post{if(currentModule!=key||isFinishing)return@post;result.onSuccess{runCatching{renderModule(key,false)}.onFailure{showModuleError(key,it)}}.onFailure{e->Toast.makeText(this,"Refresh local eșuat: ${e.message?:e.javaClass.simpleName}",Toast.LENGTH_LONG).show()}}}.start()
    }

    private fun openWatchlistTicker(ticker: String) {
        val normalized = ticker.trim().uppercase(java.util.Locale.US)
        if (normalized.isBlank()) return
        OracleSimpleModule.setTickerDraft(normalized)
        openModule("analysis")
    }

    private fun refreshModule(key:String){
        if(currentModule!=key || isFinishing)return
        Toast.makeText(this,"Se actualizează ${titles[key]?:key.uppercase()}…",Toast.LENGTH_SHORT).show()
        Thread{
            val result=runCatching{OracleLocalProcessor.refresh(repository)}
            mainHandler.post{
                if(currentModule!=key || isFinishing)return@post
                result.onSuccess{runCatching{renderModule(key,false)}.onFailure{showModuleError(key,it)}}
                    .onFailure{e->Toast.makeText(this,"Refresh local eșuat: ${e.message?:e.javaClass.simpleName}",Toast.LENGTH_LONG).show()}
            }
        }.start()
    }

    private fun renderModule(key:String,refresh:Boolean=false){
        val title = titles[key]?:key.uppercase()
        val preservedScrollY = OracleNativeModule.rememberedScroll(title)
        root.removeAllViews()
        val host=OracleNativeModule(this,title,{showHub()},{refreshModule(key)})
        root.addView(host.root,FrameLayout.LayoutParams(-1,-1))
        val data=if(refresh)OracleLocalProcessor.refresh(repository)else repository.snapshot()
        when(key){
            "portfolio"->OraclePortfolioModule(host).render(data.positions)
            "alerts"->OracleAlertsModule(host).render(data.alerts)
            "news"->OracleNewsModule(host).render(data.news)
            "journal"->OracleJournalModule(host).render(data.journal,data.history,data.alerts)
            "growth"->{ val liveGrowth=OracleGrowthLiveData.refresh(data.growth); OracleGrowthModule(host).render(liveGrowth,data.news) }
            "analysis"->OracleSimpleModule(host,title).render(actions=data.actions,knowledge=data.knowledge,positions=data.positions,history=data.history)
            "watchlist"->renderWatchlistDirect()
            "knowledge"->renderKnowledgeDirect(host)
        }
        host.restoreScrollY(preservedScrollY)
    }
    private fun renderKnowledgeDirect(host: OracleNativeModule) {
        host.content.removeAllViews()
        val context = this
        val card = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(host.dp(18), host.dp(18), host.dp(18), host.dp(18))
            background = android.graphics.drawable.GradientDrawable().apply {
                setColor(Color.rgb(8, 14, 27)); cornerRadius = host.dp(16).toFloat(); setStroke(host.dp(1), Color.rgb(255, 205, 55))
            }
            isClickable = true; isFocusable = true
            setOnClickListener { openKnowledgeUrl("https://alintudor.ro/knowledge/") }
        }
        card.addView(TextView(context).apply { text="KNOWLEDGE"; textSize=20f; typeface=Typeface.DEFAULT_BOLD; setTextColor(Color.rgb(255,215,45)) })
        card.addView(TextView(context).apply { text="Deschide alintudor.ro/knowledge/"; textSize=14f; setTextColor(Color.WHITE); setPadding(0,host.dp(8),0,host.dp(12)) })
        card.addView(Button(context).apply { text="DESCHIDE KNOWLEDGE"; textSize=13f; typeface=Typeface.DEFAULT_BOLD; setTextColor(Color.WHITE); background=android.graphics.drawable.GradientDrawable().apply{setColor(Color.rgb(12,54,82));cornerRadius=host.dp(11).toFloat()}; setOnClickListener{openKnowledgeUrl("https://alintudor.ro/knowledge/")} }, LinearLayout.LayoutParams(-1,host.dp(46)))
        host.content.addView(card, LinearLayout.LayoutParams(-1,-2).apply{setMargins(0,0,0,host.dp(14))})
    }

    private fun renderWatchlistDirect() {
        root.removeAllViews()
        val scroll = ScrollView(this).apply { isFillViewport=true; setBackgroundColor(Color.rgb(1,3,8)); overScrollMode=View.OVER_SCROLL_IF_CONTENT_SCROLLS }
        val page = LinearLayout(this).apply { orientation=LinearLayout.VERTICAL; setPadding(dp(20),dp(8),dp(20),dp(30)) }
        val header = LinearLayout(this).apply { orientation=LinearLayout.HORIZONTAL; gravity=Gravity.CENTER_VERTICAL; setPadding(dp(4),dp(2),dp(4),dp(6)) }
        val back = Button(this).apply { text="‹"; textSize=28f; setTextColor(Color.WHITE); setPadding(0,0,0,0); background=android.graphics.drawable.GradientDrawable().apply{setColor(Color.rgb(5,9,18));cornerRadius=dp(12).toFloat();setStroke(dp(1),Color.rgb(255,205,55))}; setOnClickListener{showHub()} }
        header.addView(back,LinearLayout.LayoutParams(dp(70),dp(52)))
        header.addView(TextView(this).apply{text="WATCHLIST";textSize=22f;typeface=Typeface.DEFAULT_BOLD;letterSpacing=.12f;gravity=Gravity.CENTER;setTextColor(Color.rgb(255,215,45))},LinearLayout.LayoutParams(0,dp(52),1f))
        header.addView(Space(this),LinearLayout.LayoutParams(dp(70),dp(52))); page.addView(header)
        page.addView(View(this).apply{setBackgroundColor(Color.rgb(255,205,55))},LinearLayout.LayoutParams(-1,dp(1)).apply{setMargins(0,0,0,dp(28))})
        page.addView(TextView(this).apply{text="WATCHLIST • TICKERE SALVATE";textSize=21f;typeface=Typeface.DEFAULT_BOLD;letterSpacing=.10f;setTextColor(Color.rgb(255,215,45));setPadding(dp(8),0,0,dp(14))},LinearLayout.LayoutParams(-1,-2))
        val store=OracleWatchlistStore(this); val tickers=store.load().map{it.trim().uppercase(java.util.Locale.US)}.filter{it.isNotBlank()}.distinct()
        if(tickers.isEmpty()) page.addView(TextView(this).apply{text="WATCHLIST GOALĂ";textSize=18f;setTextColor(Color.WHITE);gravity=Gravity.CENTER;setPadding(0,dp(30),0,dp(30))})
        else tickers.forEach{ticker->
            val row=LinearLayout(this).apply{orientation=LinearLayout.HORIZONTAL;gravity=Gravity.CENTER_VERTICAL;setPadding(dp(14),dp(8),dp(8),dp(8));background=android.graphics.drawable.GradientDrawable().apply{setColor(Color.rgb(6,11,22));cornerRadius=dp(16).toFloat();setStroke(dp(1),Color.rgb(45,65,95))}}
            val open=Button(this).apply{text="$ticker   ›";textSize=20f;typeface=Typeface.DEFAULT_BOLD;gravity=Gravity.CENTER_VERTICAL or Gravity.START;setTextColor(Color.WHITE);setPadding(dp(10),0,dp(8),0);background=android.graphics.drawable.GradientDrawable().apply{setColor(Color.TRANSPARENT);cornerRadius=dp(12).toFloat()};isAllCaps=false;contentDescription="Deschide analiza pentru $ticker";setOnClickListener{openWatchlistTicker(ticker)}}
            row.addView(open,LinearLayout.LayoutParams(0,dp(76),1f)); row.setOnClickListener{openWatchlistTicker(ticker)}
            val delete=Button(this).apply{text="ȘTERGE";textSize=14f;typeface=Typeface.DEFAULT_BOLD;setTextColor(Color.rgb(255,105,105));setPadding(0,0,0,0);background=android.graphics.drawable.GradientDrawable().apply{setColor(Color.TRANSPARENT);cornerRadius=dp(10).toFloat()};contentDescription="Șterge $ticker";setOnClickListener{store.remove(ticker);renderWatchlistDirect()}}
            row.addView(delete,LinearLayout.LayoutParams(dp(84),dp(58))); page.addView(row,LinearLayout.LayoutParams(-1,dp(86)).apply{setMargins(0,0,0,dp(9))})
        }
        scroll.addView(page); root.addView(scroll,FrameLayout.LayoutParams(-1,-1))
    }

    private fun showFatalError(title:String,error:Throwable){root.removeAllViews();val text=TextView(this).apply{setTextColor(Color.WHITE);textSize=16f;text="$title\n\n${error.message?:error.javaClass.simpleName}";setPadding(dp(24),dp(40),dp(24),dp(24))};root.addView(ScrollView(this).apply{addView(text)})}
    private fun showModuleError(key:String,error:Throwable){Toast.makeText(this,"${titles[key]?:key}: ${error.message?:"eroare"}",Toast.LENGTH_LONG).show()}
}
