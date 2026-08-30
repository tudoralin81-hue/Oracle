package ro.alintudor.oracle.nativeui

import android.content.Intent
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import ro.alintudor.oracle.OracleTickerAnalysisActivity
import ro.alintudor.oracle.core.*
import kotlin.math.abs

class OracleSimpleModule(private val host: OracleNativeModule, private val moduleTitle: String) {
    private val context get() = host.root.context
    private val watchlist = OracleWatchlistStore(context)

    fun render(actions: List<OracleAction> = emptyList(), knowledge: List<OracleKnowledgeItem> = emptyList(), positions: List<OraclePosition> = emptyList(), history: List<OracleHistoryPoint> = emptyList()) {
        host.content.removeAllViews()
        val normalized = OracleAnalytics.normalize(positions)
        val computed = OracleAnalytics.actions(normalized, history)
        when(moduleTitle) {
            "GROWTH" -> renderGrowth()
            "ANALYSIS" -> renderAnalysis(normalized, history)
            "WATCHLIST" -> renderWatchlist(normalized, history)
            "KNOWLEDGE" -> renderKnowledge(knowledge)
            else -> renderActions(if(computed.isNotEmpty()) computed else actions)
        }
    }

    private fun renderGrowth() {
        val repository = OracleRepository(context)
        OracleGrowthModule(host).render(repository.cachedGrowth(), repository.cachedNews())
    }

    private fun renderAnalysis(p: List<OraclePosition>, h: List<OracleHistoryPoint>) {
        host.addCard("ANALYSIS", "Selectează un ticker și îl poți păstra permanent în Watchlist.")
        if (p.isEmpty()) return
        val s = OracleAnalytics.summary(p)
        host.addCard("PORTFOLIO", "Valoare ${fmtMoney(s.value)}\nP/L ${fmtMoney(s.pnl)}  •  ${fmt(s.pnlPct)}%\nCâștigătoare ${s.winners}  •  Pierzătoare ${s.losers}\nConcentrare maximă ${fmt(s.concentration)}%  •  Risc ${s.riskLabel}")
        val tr = OracleAnalytics.trends(h).associateBy { it.ticker }
        p.sortedByDescending { abs(it.pnlPercent) }.take(50).forEach { q ->
            val t = tr[q.ticker]
            addAnalysisItem(q, "Trend ${t?.direction ?: "N/A"}  •  ${t?.let { fmt(it.changePct) + "%" } ?: "fără istoric suficient"}")
        }
    }

    private fun addAnalysisItem(q: OraclePosition, trend: String) {
        val ticker = q.ticker.trim().uppercase()
        val selected = watchlist.contains(ticker)
        val card = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(host.dp(16), host.dp(13), host.dp(16), host.dp(13))
            background = GradientDrawable().apply { setColor(Color.rgb(6,10,20)); cornerRadius = host.dp(14).toFloat(); setStroke(host.dp(1), Color.rgb(34,43,65)) }
        }
        val row = LinearLayout(context).apply { gravity = Gravity.CENTER_VERTICAL }
        row.addView(TextView(context).apply { text = ticker; textSize = 18f; typeface = Typeface.DEFAULT_BOLD; setTextColor(Color.WHITE) }, LinearLayout.LayoutParams(0,-2,1f))
        row.addView(TextView(context).apply { text = "›"; textSize = 28f; setTextColor(host.accent); gravity = Gravity.CENTER }, LinearLayout.LayoutParams(host.dp(30),host.dp(38)))
        row.setOnClickListener { openTicker(ticker) }
        card.addView(row)
        card.addView(TextView(context).apply { text = "${q.status}  •  Preț ${fmtMoney(q.currentPrice)}  •  Pondere ${fmt(q.weight)}%\nP/L ${fmtMoney(q.pnl)}  •  ${fmt(q.pnlPercent)}%\n$trend"; textSize=14f; setTextColor(Color.rgb(175,183,201)); setPadding(0,host.dp(5),0,host.dp(7)) })
        val action = Button(context).apply {
            text = if (selected) "✓ ÎN WATCHLIST" else "+ ADAUGĂ ÎN WATCHLIST"
            setTextColor(Color.WHITE); isAllCaps=false; setBackgroundColor(Color.rgb(10,70,105))
            setOnClickListener { if(watchlist.contains(ticker)) watchlist.remove(ticker) else watchlist.add(ticker); text=if(watchlist.contains(ticker)) "✓ ÎN WATCHLIST" else "+ ADAUGĂ ÎN WATCHLIST" }
        }
        card.addView(action, LinearLayout.LayoutParams(-1,host.dp(44)))
        host.content.addView(card,LinearLayout.LayoutParams(-1,-2).apply{setMargins(0,0,0,host.dp(9))})
    }

    private fun renderWatchlist(p: List<OraclePosition>, h: List<OracleHistoryPoint>) {
        host.content.removeAllViews()
        val saved = watchlist.load()
        host.addCard("WATCHLIST • TICKERE SALVATE", if(saved.isEmpty()) "Niciun ticker selectat. Adaugă tickere din Analysis." else "${saved.size} ticker(e) salvate local • lista este separată de Portfolio.")
        if(saved.isEmpty()) return
        val byTicker=p.associateBy{it.ticker.uppercase()}; val tr=OracleAnalytics.trends(h).associateBy{it.ticker}
        saved.forEach { ticker ->
            val q=byTicker[ticker]; val trend=tr[ticker]?.let{"Trend ${it.direction}  •  ${fmt(it.changePct)}%"} ?: "Trend N/A"
            addWatchlistItem(ticker,q,trend,p,h)
        }
    }

    private fun addWatchlistItem(ticker:String,q:OraclePosition?,trend:String,p:List<OraclePosition>,h:List<OracleHistoryPoint>) {
        val card=LinearLayout(context).apply{orientation=LinearLayout.HORIZONTAL;gravity=Gravity.CENTER_VERTICAL;setPadding(host.dp(16),host.dp(12),host.dp(10),host.dp(12));background=GradientDrawable().apply{setColor(Color.rgb(6,10,20));cornerRadius=host.dp(14).toFloat();setStroke(host.dp(1),Color.rgb(34,43,65))};isClickable=true;isFocusable=true}
        val info=LinearLayout(context).apply{orientation=LinearLayout.VERTICAL}
        info.addView(TextView(context).apply{text=ticker;textSize=20f;typeface=Typeface.DEFAULT_BOLD;setTextColor(Color.WHITE)})
        info.addView(TextView(context).apply{text=if(q!=null) "${q.status}  •  ${fmtMoney(q.currentPrice)}  •  P/L ${fmt(q.pnlPercent)}%\n$trend" else trend;textSize=14f;setTextColor(Color.rgb(175,183,201));setPadding(0,host.dp(3),0,0)})
        card.addView(info,LinearLayout.LayoutParams(0,-2,1f));card.addView(TextView(context).apply{text="›";textSize=30f;setTextColor(host.accent);gravity=Gravity.CENTER},LinearLayout.LayoutParams(host.dp(34),host.dp(46)))
        card.setOnClickListener{openTicker(ticker)}
        host.content.addView(card,LinearLayout.LayoutParams(-1,-2).apply{setMargins(0,0,0,host.dp(5))})
        val delete=Button(context).apply{text="ȘTERGE";isAllCaps=false;textSize=13f;setTextColor(Color.rgb(255,90,90));setBackgroundColor(Color.TRANSPARENT);setOnClickListener{watchlist.remove(ticker);renderWatchlist(p,h)}}
        host.content.addView(delete,LinearLayout.LayoutParams(-1,host.dp(42)).apply{setMargins(0,0,0,host.dp(8))})
    }

    private fun openTicker(ticker:String){context.startActivity(Intent(context,OracleTickerAnalysisActivity::class.java).putExtra("ticker",ticker))}

    private fun renderKnowledge(items:List<OracleKnowledgeItem>){host.addCard("KNOWLEDGE","Biblioteca Oracle — conținut local");if(items.isEmpty())return;items.sortedByDescending{it.publishedAt}.forEach{addItem(it.title,"${it.category}\n${it.content}")}}
    private fun renderActions(actions:List<OracleAction>){host.addCard("ACTIONS","Motor local de semnale — prioritizare după scor");if(actions.isEmpty())return;val buys=actions.count{it.action.equals("BUY",true)};val sells=actions.count{it.action.equals("SELL",true)};host.addCard("SIGNAL SUMMARY","BUY $buys  •  HOLD ${actions.size-buys-sells}  •  SELL $sells\nTotal semnale ${actions.size}");actions.sortedByDescending{abs(it.score)}.take(50).forEachIndexed{i,a->addItem("${i+1}. ${a.action} • ${a.ticker}","Scor ${fmt(a.score)}\n${a.reason}")}}
    private fun addItem(title:String,body:String){val card=LinearLayout(context).apply{orientation=LinearLayout.VERTICAL;setPadding(host.dp(16),host.dp(13),host.dp(16),host.dp(13));background=GradientDrawable().apply{setColor(Color.rgb(6,10,20));cornerRadius=host.dp(14).toFloat();setStroke(host.dp(1),Color.rgb(34,43,65))}};val row=LinearLayout(context).apply{gravity=Gravity.CENTER_VERTICAL};row.addView(TextView(context).apply{text="◆";textSize=9f;setTextColor(host.accent);gravity=Gravity.CENTER},LinearLayout.LayoutParams(host.dp(22),host.dp(22)));row.addView(TextView(context).apply{text=title.uppercase();textSize=15f;typeface=Typeface.DEFAULT_BOLD;setTextColor(Color.WHITE)},LinearLayout.LayoutParams(0,-2,1f));row.addView(TextView(context).apply{text="›";textSize=24f;setTextColor(host.accent);gravity=Gravity.CENTER},LinearLayout.LayoutParams(host.dp(24),host.dp(30)));card.addView(row);card.addView(TextView(context).apply{text=body;textSize=14f;setTextColor(Color.rgb(175,183,201));setPadding(host.dp(22),host.dp(5),0,0)});host.content.addView(card,LinearLayout.LayoutParams(-1,-2).apply{setMargins(0,0,0,host.dp(9))})}
    private fun fmt(v:Double)="%.1f".format(v)
    private fun fmtMoney(v:Double)="%.2f USD".format(v)
}

private class TrendChartView(context:android.content.Context):View(context){private val paint=Paint(Paint.ANTI_ALIAS_FLAG);override fun onDraw(c:Canvas){val w=width.toFloat();val h=height.toFloat();paint.style=Paint.Style.STROKE;paint.strokeWidth=2f;paint.color=Color.rgb(145,245,35);val path=Path();val pts=floatArrayOf(0f,.72f,.62f,.70f,.48f,.55f,.38f,.44f,.20f,.29f,.12f,.22f,.05f);path.moveTo(0f,h*pts[0]);for(i in 1 until pts.size)path.lineTo(w*i/(pts.size-1),h*pts[i]);c.drawPath(path,paint);paint.style=Paint.Style.FILL;c.drawCircle(w,h*pts.last(),3f,paint)}}