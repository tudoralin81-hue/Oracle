package ro.alintudor.oracle.nativeui

import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.widget.*
import ro.alintudor.oracle.core.*
import java.util.Locale
import kotlin.math.abs

class OracleSimpleModule(private val host: OracleNativeModule, private val moduleTitle: String) {
    fun render(actions:List<OracleAction> = emptyList(),knowledge:List<OracleKnowledgeItem> = emptyList(),positions:List<OraclePosition> = emptyList(),history:List<OracleHistoryPoint> = emptyList(),watchlist:List<String> = OracleWatchlistStore(host.root.context).load()) {
        host.content.removeAllViews(); val p=OracleAnalytics.normalize(positions); val computed=OracleAnalytics.actions(p,history)
        when(moduleTitle){"GROWTH"->renderGrowth();"ANALYSIS"->renderAnalysis();"WATCHLIST"->renderWatchlist(watchlist);"KNOWLEDGE"->renderKnowledge(knowledge);else->renderActions(if(computed.isNotEmpty())computed else actions)}
    }
    private fun renderGrowth(){val r=OracleRepository(host.root.context);OracleGrowthModule(host).render(r.cachedGrowth(),r.cachedNews())}

    private fun renderAnalysis(){
        host.addSectionLabel("ANALYSIS • SINGLE TICKER")
        host.addCard("ANALIZĂ TICKER","Introdu un ticker pentru valorile parametrilor Oracle, analiza tehnică și opțiunea de Watchlist. Fără scoruri și fără știri.")
        val input=EditText(host.root.context).apply{hint="Introdu tickerul (ex. NVDA)";setSingleLine(true);textSize=18f;setTextColor(Color.WHITE);setHintTextColor(Color.rgb(130,145,170));setPadding(host.dp(16),0,host.dp(16),0);background=GradientDrawable().apply{setColor(Color.rgb(8,14,28));cornerRadius=host.dp(14).toFloat();setStroke(host.dp(1),host.accent)};inputType=3}
        host.fixedToolbar.addView(input,LinearLayout.LayoutParams(-1,host.dp(52)).apply{setMargins(0,host.dp(3),0,host.dp(6))})
        val button=Button(host.root.context).apply{text="ANALIZEAZĂ TICKER";textSize=13f;typeface=Typeface.DEFAULT_BOLD;setTextColor(Color.WHITE);background=GradientDrawable().apply{setColor(Color.rgb(15,75,110));cornerRadius=host.dp(13).toFloat()}}
        host.fixedToolbar.addView(button,LinearLayout.LayoutParams(-1,host.dp(48)).apply{setMargins(0,0,0,host.dp(8))})
        fun run(){val t=input.text.toString().trim().uppercase(Locale.US);if(t.isBlank()){input.error="Introdu un ticker";return};button.isEnabled=false;button.text="SE ANALIZEAZĂ…";Thread{val x=runCatching{OracleAnalysisEngine.analyze(t)};host.root.post{button.isEnabled=true;button.text="ANALIZEAZĂ TICKER";x.onSuccess{renderResult(it)}.onFailure{Toast.makeText(host.root.context,"Analiza a eșuat: ${it.message?:it.javaClass.simpleName}",Toast.LENGTH_LONG).show()}}}.start()}
        button.setOnClickListener{run()};input.setOnEditorActionListener{_,_,_->run();true}
        host.content.addView(TextView(host.root.context).apply{text="Caută un ticker și primești analiza tehnică Oracle, fără modul News.";textSize=12f;setTextColor(Color.rgb(150,165,188));setPadding(host.dp(4),host.dp(5),host.dp(4),host.dp(12))})
    }

    private fun renderResult(r:OracleAnalysisEngine.Result?){
        if(r==null){Toast.makeText(host.root.context,"Tickerul nu a putut fi găsit / analizat.",Toast.LENGTH_LONG).show();return}
        if(host.content.childCount>2)host.content.removeViews(2,host.content.childCount-2)
        val top=LinearLayout(host.root.context).apply{orientation=LinearLayout.VERTICAL;setPadding(host.dp(16),host.dp(14),host.dp(16),host.dp(14));background=GradientDrawable().apply{setColor(Color.rgb(7,13,25));cornerRadius=host.dp(16).toFloat();setStroke(host.dp(1),host.accent)}}
        top.addView(TextView(host.root.context).apply{text=r.ticker;textSize=31f;typeface=Typeface.DEFAULT_BOLD;setTextColor(Color.WHITE)})
        top.addView(TextView(host.root.context).apply{text=money(r.price);textSize=14f;setTextColor(host.accent);setPadding(0,host.dp(5),0,0)})
        top.addView(TextView(host.root.context).apply{text="RISC ${r.risk} • Alocare max ${fmt(r.allocation)}%";textSize=13f;typeface=Typeface.DEFAULT_BOLD;setTextColor(Color.rgb(255,205,55));setPadding(0,host.dp(8),0,0)})
        host.content.addView(top,LinearLayout.LayoutParams(-1,-2).apply{setMargins(0,0,0,host.dp(10))})

        host.addSectionLabel("PARAMETRII ORACLE")
        val grid=LinearLayout(host.root.context).apply{orientation=LinearLayout.VERTICAL}
        OracleAnalysisEngine.factorNames.forEachIndexed{i,n->
            if(n.equals("News",true)) return@forEachIndexed
            val row=LinearLayout(host.root.context).apply{orientation=LinearLayout.HORIZONTAL;gravity=android.view.Gravity.CENTER_VERTICAL;setPadding(host.dp(14),host.dp(11),host.dp(12),host.dp(11));background=GradientDrawable().apply{setColor(if(i%2==0)Color.rgb(7,12,23) else Color.rgb(10,16,29));cornerRadius=host.dp(9).toFloat()}}
            row.addView(TextView(host.root.context).apply{text=n;textSize=14f;setTextColor(Color.rgb(215,222,235))},LinearLayout.LayoutParams(0,-2,1f))
            row.addView(TextView(host.root.context).apply{text=fmt(r.factors[i]);textSize=15f;typeface=Typeface.DEFAULT_BOLD;setTextColor(factorColor(r.factors[i]));gravity=android.view.Gravity.END},LinearLayout.LayoutParams(host.dp(58),-2))
            grid.addView(row,LinearLayout.LayoutParams(-1,-2).apply{setMargins(0,0,0,host.dp(3))})
        }
        host.content.addView(grid,LinearLayout.LayoutParams(-1,-2).apply{setMargins(0,0,0,host.dp(10))})

        host.addCard("DATE TEHNICE","RSI ${fmt(r.rsi)} • M5 ${signed(r.momentum5D)}% • M20 ${signed(r.momentum20D)}%\nVolum ${fmt(r.volumeRatio)}× media 20D • SMA50 ${moneyOrDash(r.sma50)} • SMA200 ${moneyOrDash(r.sma200)}\nADX ${r.adx?.let{fmt(it)}?:"—"} • ATR ${fmt(r.atrPct)}%")
        host.addSectionLabel("ANALIZĂ ORACLE")
        val card=LinearLayout(host.root.context).apply{orientation=LinearLayout.VERTICAL;setPadding(host.dp(15),host.dp(13),host.dp(15),host.dp(13));background=GradientDrawable().apply{setColor(Color.rgb(7,12,23));cornerRadius=host.dp(15).toFloat();setStroke(host.dp(1),Color.rgb(34,55,82))}}
        analysisLines(r).forEachIndexed{i,line->card.addView(TextView(host.root.context).apply{text=line;textSize=13f;setTextColor(Color.rgb(205,213,228));setPadding(0,if(i==0)0 else host.dp(6),0,0)})}
        host.content.addView(card,LinearLayout.LayoutParams(-1,-2).apply{setMargins(0,0,0,host.dp(10))})

        val store=OracleWatchlistStore(host.root.context);val list=store.load().toMutableList();val inWatch=list.any{it.equals(r.ticker,true)}
        val w=Button(host.root.context).apply{text=if(inWatch)"✓  ESTE ÎN WATCHLIST" else "＋  ADAUGĂ ÎN WATCHLIST";textSize=13f;typeface=Typeface.DEFAULT_BOLD;setTextColor(Color.WHITE);background=GradientDrawable().apply{setColor(if(inWatch)Color.rgb(25,75,45) else Color.rgb(95,55,10));cornerRadius=host.dp(13).toFloat()};isEnabled=!inWatch}
        w.setOnClickListener{if(list.none{it.equals(r.ticker,true)}){list.add(r.ticker);store.save(list);w.text="✓  ADAUGAT ÎN WATCHLIST";w.isEnabled=false;Toast.makeText(host.root.context,"${r.ticker} adăugat în Watchlist",Toast.LENGTH_SHORT).show()}}
        host.content.addView(w,LinearLayout.LayoutParams(-1,host.dp(50)).apply{setMargins(0,0,0,host.dp(16))})
    }

    private fun analysisLines(r:OracleAnalysisEngine.Result):List<String>{
        val f=r.factors
        val trend=when{f[2]>=75->"Trendul este puternic pozitiv, cu prețul peste mediile relevante.";f[2]>=50->"Trendul este constructiv, dar fără confirmare puternică.";else->"Trendul este fragil și cere confirmare înaintea unei intrări."}
        val mom=when{f[3]>=70->"Momentum-ul susține continuarea mișcării.";f[3]>=50->"Momentum-ul este mixt și nu oferă avantaj clar.";else->"Momentum-ul este slab și reduce calitatea semnalului."}
        val br=when{f[1]>=90->"Breakout-ul este confirmat de volum.";f[1]>=60->"Prețul testează breakout-ul, dar confirmarea este incompletă.";else->"Nu există breakout tehnic convingător."}
        val vol=if(r.volumeRatio>=1.25)"Volumul peste media 20D validează mai bine mișcarea." else "Volumul nu validează decisiv mișcarea curentă."
        val sr=when{f[5]>=70->"Poziționarea față de suport/rezistență este favorabilă.";f[5]>=45->"Poziționarea față de suport/rezistență este intermediară.";else->"Poziționarea în intervalul tehnic recent este nefavorabilă."}
        val ichi=if(f[8]>=80)"Ichimoku confirmă structura bullish." else "Ichimoku nu confirmă o structură bullish completă."
        val adx=when{(r.adx?:0.0)>=25->"ADX indică o tendință suficient de puternică.";(r.adx?:0.0)>=20->"ADX indică o tendință moderată.";else->"ADX indică o tendință slabă / neconfirmată."}
        val risk="Riscul este ${r.risk.lowercase()} iar ATR este ${fmt(r.atrPct)}%; volatilitatea trebuie controlată."
        val verdict=if(r.signal.contains("BUY"))"Verdict: configurația tehnică este favorabilă, dar intrarea trebuie raportată la risc și suport." else "Verdict: avantajul tehnic nu este suficient pentru o intrare agresivă; monitorizarea este preferabilă."
        return listOf(trend,mom,br,vol,sr,ichi,adx,risk,verdict).take(10)
    }

    private fun renderWatchlist(items:List<String>){
        host.addSectionLabel("WATCHLIST • TICKERE SALVATE")
        if(items.isEmpty()){host.addCard("WATCHLIST GOALĂ","Adaugă un ticker din Analysis. Lista este separată de Portofoliu.");return}
        val store=OracleWatchlistStore(host.root.context)
        items.distinct().sorted().forEach{t->val row=LinearLayout(host.root.context).apply{orientation=LinearLayout.HORIZONTAL;android.widget.LinearLayout.LayoutParams(-1,-2);setPadding(host.dp(15),host.dp(12),host.dp(10),host.dp(12));background=GradientDrawable().apply{setColor(Color.rgb(7,12,23));cornerRadius=host.dp(14).toFloat();setStroke(host.dp(1),Color.rgb(45,70,105))}}
            row.addView(TextView(host.root.context).apply{text=t;textSize=19f;typeface=Typeface.DEFAULT_BOLD;setTextColor(Color.WHITE)},LinearLayout.LayoutParams(0,-2,1f))
            row.addView(TextView(host.root.context).apply{text="ȘTERGE";textSize=10f;typeface=Typeface.DEFAULT_BOLD;setTextColor(Color.rgb(255,90,90));setPadding(host.dp(8),host.dp(8),host.dp(8),host.dp(8));setOnClickListener{val next=store.load().filterNot{it.equals(t,true)};store.save(next);renderWatchlist(next)}},LinearLayout.LayoutParams(-2,-2))
            host.content.addView(row,LinearLayout.LayoutParams(-1,-2).apply{setMargins(0,0,0,host.dp(8))})}
    }
    private fun renderKnowledge(items:List<OracleKnowledgeItem>){host.addCard("KNOWLEDGE","Biblioteca Oracle — conținut local");if(items.isEmpty())return;items.sortedByDescending{it.publishedAt}.forEach{addItem(it.title,"${it.category}\n${it.content}")}}
    private fun renderActions(actions:List<OracleAction>){host.addCard("ACTIONS","Motor local de semnale — prioritizare după scor");if(actions.isEmpty())return;val buys=actions.count{it.action.equals("BUY",true)};val sells=actions.count{it.action.equals("SELL",true)};host.addCard("SIGNAL SUMMARY","BUY $buys • HOLD ${actions.size-buys-sells} • SELL $sells\nTotal semnale ${actions.size}");actions.sortedByDescending{abs(it.score)}.take(50).forEachIndexed{i,a->addItem("${i+1}. ${a.action} • ${a.ticker}","Scor ${fmt(a.score)}\n${a.reason}")}}
    private fun addItem(title:String,body:String){val c=LinearLayout(host.root.context).apply{orientation=LinearLayout.VERTICAL;setPadding(host.dp(16),host.dp(13),host.dp(16),host.dp(13));background=GradientDrawable().apply{setColor(Color.rgb(6,10,20));cornerRadius=host.dp(14).toFloat();setStroke(host.dp(1),Color.rgb(34,43,65))}};val row=LinearLayout(host.root.context).apply{android.view.Gravity.CENTER;};row.addView(TextView(host.root.context).apply{text="◆";textSize=9f;setTextColor(host.accent)},LinearLayout.LayoutParams(host.dp(22),host.dp(22)));row.addView(TextView(host.root.context).apply{text=title.uppercase();textSize=15f;typeface=Typeface.DEFAULT_BOLD;setTextColor(Color.WHITE)},LinearLayout.LayoutParams(0,-2,1f));row.addView(TextView(host.root.context).apply{text="›";textSize=24f;setTextColor(host.accent)},LinearLayout.LayoutParams(host.dp(24),host.dp(30)));c.addView(row);c.addView(TextView(host.root.context).apply{text=body;textSize=14f;setTextColor(Color.rgb(175,183,201));setPadding(host.dp(22),host.dp(5),0,0)});host.content.addView(c,LinearLayout.LayoutParams(-1,-2).apply{setMargins(0,0,0,host.dp(9))})}
    private fun fmt(v:Double)="%.1f".format(Locale.US,v);private fun money(v:Double)="%.2f USD".format(Locale.US,v);private fun moneyOrDash(v:Double?)=v?.let{money(it)}?:"—";private fun signed(v:Double)=if(v>=0)"+${fmt(v)}" else fmt(v);private fun factorColor(v:Double)=when{v>=75->Color.rgb(105,245,35);v>=55->Color.rgb(255,210,55);else->Color.rgb(255,90,90)}
}
