package ro.alintudor.oracle.nativeui

import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.view.Gravity
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import ro.alintudor.oracle.core.*
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** Functional Position Monitor, matching the WordPress V5.8.18 feature set. */
class OraclePortfolioModule(private val host: OracleNativeModule) {
    private val context: Context get() = host.root.context
    private val repo by lazy { OracleRepository(context) }
    private val date = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())

    fun render(positions: List<OraclePosition>) {
        host.content.removeAllViews()
        val data = repo.snapshot()
        val items = OracleAnalytics.normalize(positions)
        host.addCard("PORTFOLIO", "Poziții, prognoză Oracle, randament real, indicatori și acțiuni")
        if (items.isEmpty()) { host.addCard("FĂRĂ POZIȚII", "Nu există poziții active în memoria locală."); return }
        val value = items.sumOf { it.marketValue }
        val invested = items.sumOf { it.shares * it.avgCost }
        val pnl = items.sumOf { it.pnl }
        addHero(value, pnl, if (invested == 0.0) 0.0 else pnl / invested * 100.0, items.size)
        addMetrics(items)
        val actions = OracleAnalytics.actions(items, data.history).associateBy { it.ticker }
        val tech = OracleTechnicalIndicators.all(data.history)
        items.sortedByDescending { it.marketValue }.forEachIndexed { i, p -> card(i + 1, p, actions[p.ticker], tech[p.ticker], data.journal) }
        addBottomActions(items, data.journal)
    }

    private fun addHero(value: Double, pnl: Double, pct: Double, count: Int) {
        val box = LinearLayout(context).apply { orientation = LinearLayout.VERTICAL; setPadding(host.dp(18),host.dp(15),host.dp(18),host.dp(15)); background = OracleNativeModule.rounded(Color.rgb(7,11,22),host.dp(16),Color.rgb(92,72,28),host.dp(1)) }
        val row = LinearLayout(context).apply { gravity = Gravity.CENTER_VERTICAL }
        row.addView(TextView(context).apply { text="◔"; textSize=32f; setTextColor(Color.rgb(255,210,55)); gravity=Gravity.CENTER },LinearLayout.LayoutParams(host.dp(45),host.dp(45)))
        row.addView(LinearLayout(context).apply { orientation=LinearLayout.VERTICAL; setPadding(host.dp(10),0,0,0); addView(TextView(context).apply{text="TOTAL PORTOFOLIU • $count ACȚIUNI";textSize=11f;setTextColor(Color.rgb(155,166,188))}); addView(TextView(context).apply{text=money(value);textSize=23f;typeface=Typeface.DEFAULT_BOLD;setTextColor(Color.WHITE);setPadding(0,host.dp(3),0,0)}) },LinearLayout.LayoutParams(0,-2,1f))
        row.addView(TextView(context).apply{text=signedPct(pct);textSize=18f;typeface=Typeface.DEFAULT_BOLD;setTextColor(if(pnl>=0)Color.rgb(145,245,35) else Color.rgb(255,80,65))})
        box.addView(row); box.addView(TextView(context).apply{text="P/L  ${money(pnl)}";textSize=13f;setTextColor(Color.rgb(175,183,201));setPadding(host.dp(55),host.dp(5),0,0)})
        host.content.addView(box,LinearLayout.LayoutParams(-1,-2).apply{setMargins(0,0,0,host.dp(9))})
    }

    private fun addMetrics(items: List<OraclePosition>) {
        val row1=LinearLayout(context).apply{orientation=LinearLayout.HORIZONTAL}; metric(row1,"CÂȘTIGĂTOARE",items.count{it.pnl>0}.toString()); metric(row1,"PIERZĂTOARE",items.count{it.pnl<0}.toString()); host.content.addView(row1)
        val row2=LinearLayout(context).apply{orientation=LinearLayout.HORIZONTAL}; metric(row2,"CONCENTRARE MAX.",pct(items.maxOf{it.weight})); metric(row2,"RISC",when{items.maxOf{it.weight}>=50->"HIGH";items.maxOf{it.weight}>=35->"MEDIUM";else->"CONTROLAT"}); host.content.addView(row2)
    }
    private fun metric(row:LinearLayout,label:String,value:String){ val b=LinearLayout(context).apply{orientation=LinearLayout.VERTICAL;setPadding(host.dp(13),host.dp(10),host.dp(13),host.dp(10));background=OracleNativeModule.rounded(Color.rgb(7,11,22),host.dp(11),Color.rgb(35,44,66),host.dp(1))}; b.addView(TextView(context).apply{text=label;textSize=9f;setTextColor(Color.rgb(145,155,176))}); b.addView(TextView(context).apply{text=value;textSize=16f;typeface=Typeface.DEFAULT_BOLD;setTextColor(Color.WHITE);setPadding(0,host.dp(3),0,0)}); row.addView(b,LinearLayout.LayoutParams(0,-2,1f).apply{setMargins(host.dp(2),host.dp(4),host.dp(2),host.dp(5))}) }

    private fun card(rank:Int,p:OraclePosition,a:OracleAction?,t:OracleTechnicalSnapshot?,journal:List<OracleJournalEntry>){
        val forecast=journal.filter{it.ticker.equals(p.ticker,true)&&it.action.contains("BUY / OPEN",true)}.minByOrNull{it.timestamp}?.score ?: a?.score ?: 0.0
        val action=decision(a?.action ?: "HOLD",t)
        val accent=when(action){"BUY"->Color.rgb(145,245,35);"SELL"->Color.rgb(255,80,95);else->Color.rgb(50,220,190)}
        val reason=when{t==null->"Date tehnice insuficiente; monitorizare locală";t.rsi>=70->"supraîncălzire RSI · trend și momentum încă acceptabile";t.rsi<=30->"RSI slab · presiune de vânzare";action=="BUY"->"trend și momentum favorabile";action=="SELL"->"semnal negativ · risc în creștere";else->"trend și momentum încă acceptabile"}
        val c=LinearLayout(context).apply{orientation=LinearLayout.VERTICAL;setPadding(host.dp(15),host.dp(13),host.dp(12),host.dp(13));background=OracleNativeModule.rounded(Color.rgb(6,10,20),host.dp(15),Color.rgb(42,52,76),host.dp(1))}
        val top=LinearLayout(context).apply{gravity=Gravity.CENTER_VERTICAL}; top.addView(TextView(context).apply{text="%02d".format(rank);textSize=11f;typeface=Typeface.DEFAULT_BOLD;setTextColor(accent)},LinearLayout.LayoutParams(host.dp(34),host.dp(30))); top.addView(LinearLayout(context).apply{orientation=LinearLayout.VERTICAL;addView(TextView(context).apply{text=p.ticker;textSize=20f;typeface=Typeface.DEFAULT_BOLD;setTextColor(Color.WHITE)});addView(TextView(context).apply{text="${p.company} • ${shares(p.shares)} acțiuni • intrare ${money(p.avgCost)}";textSize=10f;setTextColor(Color.rgb(155,166,188));setPadding(0,host.dp(2),0,0)})},LinearLayout.LayoutParams(0,-2,1f)); top.addView(TextView(context).apply{text=action;textSize=12f;typeface=Typeface.DEFAULT_BOLD;setTextColor(accent)},LinearLayout.LayoutParams(host.dp(62),host.dp(30))); c.addView(top)
        c.addView(TextView(context).apply{text="${money(p.marketValue)} ${p.currency}   •   ${pct(p.weight)} PONDERE";textSize=13f;setTextColor(Color.rgb(175,183,201));setPadding(host.dp(34),host.dp(5),0,0)})
        val forecasts=LinearLayout(context).apply{orientation=LinearLayout.HORIZONTAL;setPadding(host.dp(34),host.dp(10),0,0)}; forecasts.addView(valueBox("PROGNOZAT ORACLE",signedPct(forecast),Color.rgb(55,215,255)),LinearLayout.LayoutParams(0,-2,1f).apply{setMargins(0,0,host.dp(4),0)}); forecasts.addView(valueBox("REAL ACUM",signedPct(p.pnlPercent),if(p.pnlPercent>=0)Color.rgb(65,225,135) else Color.rgb(255,85,105)),LinearLayout.LayoutParams(0,-2,1f).apply{setMargins(host.dp(4),0,0,0)}); c.addView(forecasts)
        val decision=LinearLayout(context).apply{orientation=LinearLayout.VERTICAL;setPadding(host.dp(15),host.dp(9),host.dp(15),host.dp(9));background=OracleNativeModule.rounded(Color.rgb(8,16,25),host.dp(11),accent,host.dp(1))}; decision.addView(TextView(context).apply{text=action;textSize=18f;typeface=Typeface.DEFAULT_BOLD;setTextColor(accent)}); decision.addView(TextView(context).apply{text=reason;textSize=12f;setTextColor(Color.rgb(190,198,215));setPadding(0,host.dp(4),0,0)}); c.addView(decision,LinearLayout.LayoutParams(-1,-2).apply{setMargins(host.dp(34),host.dp(8),0,0)})
        val grid=LinearLayout(context).apply{orientation=LinearLayout.VERTICAL;setPadding(host.dp(34),host.dp(8),0,0)}; two(grid,"P/L","${money(p.pnl)} (${signedPct(p.pnlPercent)})","Score",a?.score?.let{String.format(Locale.US,"%.0f/100",it)}?:"N/A"); two(grid,"RSI",t?.rsi?.let{String.format(Locale.US,"%.1f",it)}?:"N/A","SMA50",money(t?.sma50?:p.currentPrice)); two(grid,"Momentum 5D",signedPct(t?.momentum5D?:0.0),"Momentum 20D",signedPct(t?.momentum20D?:0.0)); two(grid,"Suport 20D",money(t?.support20D?:p.currentPrice),"Rezistență 20D",money(t?.resistance20D?:p.currentPrice)); c.addView(grid)
        val buttons=LinearLayout(context).apply{orientation=LinearLayout.HORIZONTAL;setPadding(host.dp(34),host.dp(9),0,0)}; buttons.addView(btn("SELL ACȚIUNI",Color.rgb(255,205,65)){partialSell(p,forecast)},LinearLayout.LayoutParams(0,host.dp(43),1f).apply{setMargins(0,0,host.dp(4),0)}); buttons.addView(btn("FULL SELL",Color.rgb(255,80,105)){fullSell(p,forecast)},LinearLayout.LayoutParams(0,host.dp(43),1f).apply{setMargins(host.dp(4),0,0,0)}); c.addView(buttons); c.addView(TextView(context).apply{text="Actualizat local • ${date.format(Date())}";textSize=9f;setTextColor(Color.rgb(105,120,145));setPadding(host.dp(34),host.dp(7),0,0)}); host.content.addView(c,LinearLayout.LayoutParams(-1,-2).apply{setMargins(0,0,0,host.dp(9))})
    }

    private fun valueBox(label:String,value:String,color:Int)=LinearLayout(context).apply{orientation=LinearLayout.VERTICAL;setPadding(host.dp(9),host.dp(8),host.dp(9),host.dp(8));background=OracleNativeModule.rounded(Color.rgb(8,13,27),host.dp(10),color,host.dp(1));addView(TextView(context).apply{text=label;textSize=9f;typeface=Typeface.DEFAULT_BOLD;setTextColor(Color.rgb(155,166,188))});addView(TextView(context).apply{text=value;textSize=19f;typeface=Typeface.DEFAULT_BOLD;setTextColor(color);setPadding(0,host.dp(2),0,0)})}
    private fun two(g:LinearLayout,a:String,av:String,b:String,bv:String){val r=LinearLayout(context).apply{orientation=LinearLayout.HORIZONTAL}; metric(r,a,av); metric(r,b,bv); g.addView(r)}
    private fun btn(label:String,color:Int,click:()->Unit)=TextView(context).apply{text=label;textSize=10f;typeface=Typeface.DEFAULT_BOLD;gravity=Gravity.CENTER;setTextColor(color);background=OracleNativeModule.rounded(Color.rgb(8,12,25),host.dp(10),color,host.dp(1));setOnClickListener{click()}}

    private fun partialSell(p:OraclePosition,forecast:Double){val input=EditText(context).apply{inputType=2;setText(shares(p.shares/2))};AlertDialog.Builder(context).setTitle("SELL ACȚIUNI • ${p.ticker}").setMessage("Acțiunea este locală în Oracle; nu execută tranzacții la broker.\n\nCantitate:").setView(input).setNegativeButton("ANULEAZĂ",null).setPositiveButton("CONFIRMĂ"){_,_->val q=input.text.toString().toDoubleOrNull()?:0.0;if(q<=0||q>p.shares){toast("Cantitate invalidă");return@setPositiveButton};sell(p,q,false,forecast)}.show()}
    private fun fullSell(p:OraclePosition,forecast:Double){AlertDialog.Builder(context).setTitle("FULL SELL • ${p.ticker}").setMessage("Închide poziția locală la ${money(p.currentPrice)}. Nu se transmite brokerului.").setNegativeButton("ANULEAZĂ",null).setPositiveButton("FULL SELL"){_,_->sell(p,p.shares,true,forecast)}.show()}
    private fun sell(p:OraclePosition,q:Double,full:Boolean,forecast:Double){val now=System.currentTimeMillis();val old=repo.cachedPositions().filterNot{it.ticker.equals(p.ticker,true)}.toMutableList();val remain=p.shares-q;if(!full&&remain>0)old+=p.copy(shares=remain);repo.savePositions(OracleCalculations.withWeights(old));val j=repo.cachedJournal().toMutableList();j+=OracleJournalEntry(now,p.ticker,if(full)"SELL (FULL)" else "SELL (PARTIAL)",forecast,if(full)"Închidere poziție locală" else "Vânzare parțială locală",if(full)"CLOSED" else "ACTIVE",q,p.avgCost,p.currentPrice,if(p.shares==0.0)100.0 else q/p.shares*100.0,q*p.avgCost,q*p.currentPrice,q*(p.currentPrice-p.avgCost));repo.saveJournal(j);toast(if(full)"${p.ticker}: poziție închisă local" else "${p.ticker}: vânzare înregistrată");render(repo.cachedPositions())}

    private fun addBottomActions(p:List<OraclePosition>,j:List<OracleJournalEntry>){
        val row=LinearLayout(context).apply{orientation=LinearLayout.HORIZONTAL;setPadding(host.dp(2),host.dp(3),host.dp(2),0)}
        val journalText="AI STOCK ORACLE — JURNAL\n"+j.sortedByDescending{it.timestamp}.take(150).joinToString("\n"){entry->"${date.format(Date(entry.timestamp))} | ${entry.ticker} | ${entry.action} | score=${entry.score} | ${entry.reason}"}
        row.addView(btn("JURNAL ACTIVITATE",Color.rgb(55,215,255)){share(journalText)},LinearLayout.LayoutParams(0,host.dp(46),1f).apply{setMargins(0,0,host.dp(3),0)})
        row.addView(btn("DESCARCĂ EXCEL",Color.rgb(65,225,135)){share(csv(p,j))},LinearLayout.LayoutParams(0,host.dp(46),1f).apply{setMargins(host.dp(3),0,host.dp(3),0)})
        row.addView(btn("DESCARCĂ PDF",Color.rgb(255,205,65)){share(pdfText(p))},LinearLayout.LayoutParams(0,host.dp(46),1f).apply{setMargins(host.dp(3),0,0,0)})
        host.content.addView(row)
    }
    private fun csv(p:List<OraclePosition>,j:List<OracleJournalEntry>)="Ticker,Company,Shares,Entry,Price,Value,P/L,P/L%,Weight\n"+p.joinToString("\n"){"${it.ticker},${it.company},${it.shares},${it.avgCost},${it.currentPrice},${it.marketValue},${it.pnl},${it.pnlPercent},${it.weight}"}
    private fun pdfText(p:List<OraclePosition>)="AI STOCK ORACLE — PORTFOLIO\nGenerated ${date.format(Date())}\n\n"+p.joinToString("\n"){"${it.ticker} | ${shares(it.shares)} sh | ${money(it.currentPrice)} | P/L ${signedPct(it.pnlPercent)}"}
    private fun share(text:String){context.startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).apply{type="text/plain";putExtra(Intent.EXTRA_TEXT,text)},"Oracle export"))}
    private fun decision(action:String,t:OracleTechnicalSnapshot?)=when{(t?.rsi?:50)>=70->"HOLD";action=="BUY"->"BUY";action=="SELL"->"SELL";else->"HOLD"}
    private fun money(v:Double)=String.format(Locale.US,"%,.2f",v);private fun pct(v:Double)=String.format(Locale.US,"%.2f%%",v);private fun signedPct(v:Double)=String.format(Locale.US,"%+.1f%%",v);private fun shares(v:Double)=if(v%1.0==0.0)v.toInt().toString() else String.format(Locale.US,"%.2f",v);private fun toast(s:String)=Toast.makeText(context,s,Toast.LENGTH_LONG).show()
}
