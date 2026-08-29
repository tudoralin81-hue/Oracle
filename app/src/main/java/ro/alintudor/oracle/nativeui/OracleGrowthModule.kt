package ro.alintudor.oracle.nativeui

import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.widget.LinearLayout
import android.widget.TextView
import ro.alintudor.oracle.core.OracleGrowthRecommendation
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

/** Growth module translated from the supplied WordPress reference. */
class OracleGrowthModule(private val host: OracleNativeModule) {
    private val bg = Color.rgb(6,10,20)
    private val border = Color.rgb(49,82,125)
    private val muted = Color.rgb(165,174,195)
    private val cyan = Color.rgb(75,225,255)
    private val orange = Color.rgb(255,160,25)
    private val green = Color.rgb(105,245,35)
    private val red = Color.rgb(255,80,90)
    private val parameterNames = listOf("News","BO","Trend","Mom","Vol","S/R","Fund","BB","Ichimoku","Mkt","R/R","ADX")

    fun render(items: List<OracleGrowthRecommendation>, fallbackNews: List<ro.alintudor.oracle.core.OracleNews> = emptyList()) {
        host.content.removeAllViews()
        if (items.isEmpty()) { host.addCard("GROWTH","Nu există încă un snapshot Growth local. Refresh va afișa ultimul rezultat Oracle disponibil."); return }
        addIntro(items)
        addParameters(items)
        listOf(Triple("SHORT","TERMEN SCURT","1–10 zile bursiere"),Triple("MEDIUM","TERMEN MEDIU","2–12 săptămâni"),Triple("LONG","TERMEN LUNG","3–12 luni")).forEach { (horizon, label, range) ->
            val recommendation = items.firstOrNull { it.horizon.equals(horizon, true) }
            addHorizon(label, range, recommendation, fallbackNews)
        }
        addHistory(items)
    }

    private fun addIntro(items: List<OracleGrowthRecommendation>) {
        val card = card(16)
        card.addView(text("RECOMANDĂRILE DE CREȘTERE",18f,Typeface.DEFAULT_BOLD,green,0,0))
        card.addView(text("Oracle Growth • snapshot zilnic 16:00 • univers țintă 300 companii",13f,Typeface.DEFAULT,muted,0,5))
        val line=LinearLayout(host.root.context).apply{orientation=LinearLayout.HORIZONTAL;gravity=Gravity.CENTER_VERTICAL;setPadding(0,host.dp(12),0,0)}
        line.addView(metric("ORIZONTURI",items.map{it.horizon}.distinct().size.toString(),cyan),LinearLayout.LayoutParams(0,-2,1f))
        line.addView(metric("RECOMANDĂRI",items.size.toString(),orange),LinearLayout.LayoutParams(0,-2,1f))
        line.addView(metric("ANCHOR",formatT0(items.first().referenceTimestamp),Color.WHITE),LinearLayout.LayoutParams(0,-2,1.35f))
        card.addView(line);host.content.addView(card,LinearLayout.LayoutParams(-1,-2).apply{setMargins(0,0,0,host.dp(10))})
    }

    private fun addParameters(items: List<OracleGrowthRecommendation>) {
        val card=card(14)
        card.addView(text("PARAMETRII LUAȚI ÎN CALCUL",16f,Typeface.DEFAULT_BOLD,cyan,0,0))
        card.addView(text("Ponderile sunt cele ale profilului Oracle primit pentru fiecare orizont.",13f,Typeface.DEFAULT,muted,0,5))
        val reference=items.firstOrNull{it.horizon.equals("SHORT",true)}
        addWeightGrid(card,reference?.weights ?: emptyList())
        card.addView(text("Selectarea SHORT / MEDIUM / LONG schimbă profilul; Android nu modifică formulele Oracle.",11f,Typeface.DEFAULT,Color.rgb(125,135,155),0,9))
        host.content.addView(card,LinearLayout.LayoutParams(-1,-2).apply{setMargins(0,0,0,host.dp(12))})
    }

    private fun addHorizon(label:String,range:String,item:OracleGrowthRecommendation?,news:List<ro.alintudor.oracle.core.OracleNews>){
        val header=LinearLayout(host.root.context).apply{orientation=LinearLayout.VERTICAL;setPadding(host.dp(4),host.dp(5),host.dp(4),host.dp(9))}
        val titleRow=LinearLayout(host.root.context).apply{gravity=Gravity.CENTER_VERTICAL}
        titleRow.addView(TextView(host.root.context).apply{text="●";textSize=22f;setTextColor(green);gravity=Gravity.CENTER},LinearLayout.LayoutParams(host.dp(34),host.dp(32)))
        titleRow.addView(TextView(host.root.context).apply{text=label;textSize=22f;typeface=Typeface.DEFAULT_BOLD;setTextColor(orange)},LinearLayout.LayoutParams(0,-2,1f))
        header.addView(titleRow);header.addView(text(range,14f,Typeface.DEFAULT,muted,host.dp(34),3))
        host.content.addView(header,LinearLayout.LayoutParams(-1,-2).apply{setMargins(0,0,0,host.dp(1))})
        if(item==null){host.addCard("DATE INSUFICIENTE","Nu există încă un snapshot ${label.lowercase()} în cache.");return}
        addRecommendation(item,news)
    }

    private fun addRecommendation(item:OracleGrowthRecommendation,news:List<ro.alintudor.oracle.core.OracleNews>){
        val card=card(15)
        val timeRow=LinearLayout(host.root.context).apply{gravity=Gravity.CENTER_VERTICAL}
        timeRow.addView(text("◷  ${formatT0(item.referenceTimestamp)}",13f,Typeface.DEFAULT,muted,0,0),LinearLayout.LayoutParams(0,-2,1f))
        timeRow.addView(text("ANCHOR 16:00",11f,Typeface.DEFAULT_BOLD,cyan,0,0));card.addView(timeRow)
        val identity=LinearLayout(host.root.context).apply{orientation=LinearLayout.HORIZONTAL;gravity=Gravity.CENTER_VERTICAL;setPadding(0,host.dp(14),0,host.dp(10))}
        identity.addView(text(item.ticker,34f,Typeface.DEFAULT_BOLD,Color.WHITE,0,0),LinearLayout.LayoutParams(host.dp(125),-2))
        val company=LinearLayout(host.root.context).apply{orientation=LinearLayout.VERTICAL};company.addView(text(item.company,17f,Typeface.DEFAULT_BOLD,Color.WHITE,0,0));company.addView(text(item.sector,14f,Typeface.DEFAULT_BOLD,Color.rgb(160,175,205),0,5));identity.addView(company,LinearLayout.LayoutParams(0,-2,1f));card.addView(identity);card.addView(divider())
        val metrics=LinearLayout(host.root.context).apply{orientation=LinearLayout.HORIZONTAL;setPadding(0,host.dp(10),0,host.dp(5))}
        metrics.addView(metric("SCOR","${item.score}/100",cyan),LinearLayout.LayoutParams(0,-2,1f));metrics.addView(metric("SEMNAL",item.signal,orange),LinearLayout.LayoutParams(0,-2,1.2f));metrics.addView(metric("RISC",item.risk,red),LinearLayout.LayoutParams(0,-2,1f));metrics.addView(metric("ALOCARE MAX.","${format(item.allocationMax)}%",orange),LinearLayout.LayoutParams(0,-2,1f));card.addView(metrics)
        val forecast=LinearLayout(host.root.context).apply{orientation=LinearLayout.VERTICAL;gravity=Gravity.CENTER;setPadding(0,host.dp(13),0,host.dp(13));background=GradientDrawable().apply{setColor(Color.rgb(5,13,15));cornerRadius=host.dp(15).toFloat();setStroke(host.dp(1),Color.rgb(47,95,125))}}
        forecast.addView(text("Potențial estimat",18f,Typeface.DEFAULT,Color.WHITE,0,0).apply{gravity=Gravity.CENTER});forecast.addView(text("${if(item.forecastPct>=0)"+" else ""}${format(item.forecastPct)}%",42f,Typeface.DEFAULT_BOLD,green,0,5).apply{gravity=Gravity.CENTER});card.addView(forecast,LinearLayout.LayoutParams(-1,-2).apply{setMargins(0,host.dp(6),0,host.dp(10))})
        val mom=LinearLayout(host.root.context).apply{orientation=LinearLayout.HORIZONTAL;setPadding(0,host.dp(2),0,host.dp(9))}
        mom.addView(metric("MOMENTUM 5D","${format(item.momentum5D)}%",cyan),LinearLayout.LayoutParams(0,-2,1f));mom.addView(metric("MOMENTUM 20D","${format(item.momentum20D)}%",cyan),LinearLayout.LayoutParams(0,-2,1f));item.currentActualPct?.let{mom.addView(metric("ACTUAL","${format(it)}%",Color.WHITE),LinearLayout.LayoutParams(0,-2,1f))};card.addView(mom)
        card.addView(text("Ponderi (importanță)",16f,Typeface.DEFAULT_BOLD,Color.WHITE,0,4));addWeightGrid(card,item.weights)
        val linkedNews=news.firstOrNull{it.ticker.equals(item.ticker,true)};val newsTitle=if(item.newsTitle.isNotBlank())item.newsTitle else linkedNews?.title.orEmpty();val source=if(item.newsSource.isNotBlank())item.newsSource else linkedNews?.source.orEmpty()
        if(newsTitle.isNotBlank()){val newsCard=LinearLayout(host.root.context).apply{orientation=LinearLayout.VERTICAL;setPadding(host.dp(2),host.dp(11),host.dp(2),host.dp(2))};newsCard.addView(text("▣  ${if(source.isBlank())"NEWS" else source}",12f,Typeface.DEFAULT_BOLD,cyan,0,0));newsCard.addView(text(newsTitle,14f,Typeface.DEFAULT,Color.WHITE,0,5));card.addView(newsCard)}
        card.addView(text("Datele sunt informative și nu constituie recomandări de investiții.",11f,Typeface.DEFAULT,Color.rgb(125,135,155),0,11));host.content.addView(card,LinearLayout.LayoutParams(-1,-2).apply{setMargins(0,0,0,host.dp(13))})
    }

    private fun addWeightGrid(parent:LinearLayout,weights:List<Int>){
        if(weights.isEmpty())return
        val columns=if(host.root.context.resources.configuration.screenWidthDp>=600)6 else 3;val rows=(parameterNames.size+columns-1)/columns
        for(r in 0 until rows){val row=LinearLayout(host.root.context).apply{orientation=LinearLayout.HORIZONTAL;gravity=Gravity.CENTER_VERTICAL};for(c in 0 until columns){val index=r*columns+c;if(index>=parameterNames.size)break;val value=weights.getOrNull(index)?:0;val cell=LinearLayout(host.root.context).apply{orientation=LinearLayout.VERTICAL;gravity=Gravity.CENTER;setPadding(host.dp(2),host.dp(8),host.dp(2),host.dp(8))};cell.addView(text(parameterNames[index],10f,Typeface.DEFAULT,muted,0,0).apply{gravity=Gravity.CENTER});cell.addView(text(if(value>0)value.toString() else "—",15f,Typeface.DEFAULT_BOLD,cyan,0,3).apply{gravity=Gravity.CENTER});row.addView(cell,LinearLayout.LayoutParams(0,-2,1f))};parent.addView(row)}
    }

    private fun addHistory(items:List<OracleGrowthRecommendation>){
        val label=TextView(host.root.context).apply{text="JURNAL GROWTH • ISTORIC FORECAST";textSize=17f;typeface=Typeface.DEFAULT_BOLD;letterSpacing=.06f;setTextColor(cyan);setPadding(host.dp(4),host.dp(9),host.dp(4),host.dp(8))};host.content.addView(label)
        items.sortedBy{it.referenceTimestamp}.forEach{item->val row=LinearLayout(host.root.context).apply{orientation=LinearLayout.HORIZONTAL;gravity=Gravity.CENTER_VERTICAL;setPadding(host.dp(14),host.dp(12),host.dp(14),host.dp(12));background=GradientDrawable().apply{setColor(bg);cornerRadius=host.dp(13).toFloat();setStroke(host.dp(1),Color.rgb(34,43,65))}};row.addView(text(item.ticker,16f,Typeface.DEFAULT_BOLD,Color.WHITE,0,0),LinearLayout.LayoutParams(host.dp(64),-2));row.addView(text("${item.horizon}\nT0 ${formatT0(item.referenceTimestamp)}",11f,Typeface.DEFAULT,muted,0,0),LinearLayout.LayoutParams(0,-2,1.4f));row.addView(text("Forecast\n${if(item.forecastPct>=0)"+" else ""}${format(item.forecastPct)}%",12f,Typeface.DEFAULT_BOLD,green,0,0),LinearLayout.LayoutParams(0,-2,1f));row.addView(text("Scor\n${item.score}/100",12f,Typeface.DEFAULT_BOLD,cyan,0,0),LinearLayout.LayoutParams(0,-2,.8f));host.content.addView(row,LinearLayout.LayoutParams(-1,-2).apply{setMargins(0,0,0,host.dp(8))})}
        host.content.addView(text("Istoricul este append-only: o schimbare de forecast nu rescrie T0.",11f,Typeface.DEFAULT,Color.rgb(125,135,155),host.dp(4),4),LinearLayout.LayoutParams(-1,-2).apply{setMargins(0,0,0,host.dp(18))})
    }

    private fun card(padding:Int)=LinearLayout(host.root.context).apply{orientation=LinearLayout.VERTICAL;setPadding(host.dp(padding),host.dp(padding),host.dp(padding),host.dp(padding));background=GradientDrawable().apply{setColor(bg);cornerRadius=host.dp(16).toFloat();setStroke(host.dp(1),border)}}
    private fun divider()=android.view.View(host.root.context).apply{setBackgroundColor(Color.rgb(35,48,70));layoutParams=LinearLayout.LayoutParams(-1,host.dp(1))}
    private fun metric(label:String,value:String,color:Int)=LinearLayout(host.root.context).apply{orientation=LinearLayout.VERTICAL;setPadding(host.dp(3),host.dp(4),host.dp(3),host.dp(4));gravity=Gravity.CENTER;addView(text(label,10f,Typeface.DEFAULT,muted,0,0).apply{gravity=Gravity.CENTER});addView(text(value,16f,Typeface.DEFAULT_BOLD,color,0,4).apply{gravity=Gravity.CENTER})}
    private fun text(value:String,size:Float,typeface:Typeface,color:Int,left:Int,top:Int)=TextView(host.root.context).apply{text=value;textSize=size;this.typeface=typeface;setTextColor(color);setPadding(left,top,0,0)}
    private fun format(v:Double)="%.1f".format(Locale.US,v)
    private fun formatT0(timestamp:Long):String{if(timestamp<=0L)return "—";val f=SimpleDateFormat("dd.MM.yyyy HH:mm",Locale("ro","RO"));f.timeZone=TimeZone.getTimeZone("Europe/Bucharest");return f.format(Date(timestamp))}
}
