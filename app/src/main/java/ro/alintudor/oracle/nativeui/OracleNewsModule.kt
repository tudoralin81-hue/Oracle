package ro.alintudor.oracle.nativeui

import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.view.Gravity
import android.widget.LinearLayout
import android.widget.TextView
import ro.alintudor.oracle.core.OracleNews
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

/** News UI mirrors the source-oriented web layout: one publisher block with its latest stories. */
class OracleNewsModule(private val host: OracleNativeModule) {
    private val time = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.US).apply { timeZone = TimeZone.getTimeZone("Europe/Bucharest") }
    private val sourceOrder = listOf("CNBC","BBC Business","Financial Times","Bloomberg","MarketWatch","The Wall Street Journal","The New York Times Business","Reuters","Investing.com","Google News • Markets")

    fun render(news: List<OracleNews>) {
        host.content.removeAllViews()
        val clean = dedupe(news).sortedByDescending { it.publishedAt }
        if (clean.isEmpty()) {
            host.addCard("NEWS", "Nu există știri disponibile momentan. Feed-urile se actualizează în fundal.")
            return
        }
        host.addCard("ȘTIRI ECONOMICE INTERNAȚIONALE", "Piețe, economie, companii și finanțe — organizate pe surse.")
        val groups = clean.groupBy { sourceName(it) }
        sourceOrder.filter { groups.containsKey(it) }.forEach { source -> addSource(source, groups.getValue(source)) }
        groups.keys.filterNot { sourceOrder.contains(it) }.sorted().forEach { source -> addSource(source, groups.getValue(source)) }
    }

    private fun dedupe(items: List<OracleNews>): List<OracleNews> = items.filter { it.title.isNotBlank() }.groupBy { key(it) }
        .values.map { group -> group.maxByOrNull { it.publishedAt }!! }

    private fun key(n: OracleNews): String {
        val url=n.url.trim().lowercase(Locale.US).substringBefore("?").removeSuffix("/")
        if(url.isNotBlank()) return "url:$url"
        return "title:"+n.title.trim().lowercase(Locale.US).replace(Regex("\\s+")," ").replace(Regex("[^a-z0-9 ]"),"")
    }

    private fun sourceName(n: OracleNews): String = n.publisher.ifBlank { n.source }.ifBlank { "Other News" }

    private fun addSource(source: String, items: List<OracleNews>) {
        val accent = sourceAccent(source)
        val box = LinearLayout(host.root.context).apply {
            orientation=LinearLayout.VERTICAL
            background=GradientDrawable().apply { setColor(Color.rgb(9,15,29)); cornerRadius=host.dp(16).toFloat(); setStroke(host.dp(1),Color.rgb(39,53,80)) }
            setPadding(host.dp(14),host.dp(13),host.dp(14),host.dp(12))
        }
        box.addView(TextView(host.root.context).apply { text=source; textSize=19f; typeface=Typeface.DEFAULT_BOLD; setTextColor(Color.WHITE); setPadding(host.dp(2),0,0,host.dp(8)) })
        items.sortedWith(compareByDescending<OracleNews>{it.breaking}.thenByDescending{it.publishedAt}).take(8).forEachIndexed { index,n -> addStory(box,n,index,accent) }
        box.addView(TextView(host.root.context).apply { text="VEZI $source  →"; textSize=11f; typeface=Typeface.DEFAULT_BOLD; gravity=Gravity.CENTER; setTextColor(Color.WHITE); background=GradientDrawable().apply{setColor(Color.rgb(18,34,58));cornerRadius=host.dp(11).toFloat()}; setPadding(0,host.dp(10),0,host.dp(10)); isClickable=true; if(items.firstOrNull()?.url?.isNotBlank()==true)setOnClickListener{open(items.first().url)} }, LinearLayout.LayoutParams(-1,-2).apply{setMargins(0,host.dp(8),0,0)})
        host.content.addView(box,LinearLayout.LayoutParams(-1,-2).apply{setMargins(0,0,0,host.dp(12))})
    }

    private fun addStory(box:LinearLayout,n:OracleNews,index:Int,accent:Int){
        val row=LinearLayout(host.root.context).apply{orientation=LinearLayout.VERTICAL;setPadding(host.dp(3),host.dp(9),host.dp(3),host.dp(9));isClickable=n.url.isNotBlank();if(isClickable)setOnClickListener{open(n.url)}}
        val title=TextView(host.root.context).apply{text=n.title;textSize=15f;typeface=Typeface.DEFAULT_BOLD;setTextColor(Color.rgb(232,237,248));setLineSpacing(0f,1.05f)}
        row.addView(LinearLayout(host.root.context).apply{gravity=Gravity.CENTER_VERTICAL;addView(TextView(host.root.context).apply{text=if(n.breaking)"BREAKING" else "%02d".format(index+1);textSize=9f;typeface=Typeface.DEFAULT_BOLD;setTextColor(accent)},LinearLayout.LayoutParams(host.dp(72),-2));addView(title,LinearLayout.LayoutParams(0,-2,1f))})
        val meta=buildList{if(n.publishedAt>0)add(time.format(Date(n.publishedAt)));n.sentimentScore?.let{add("Sent %+.2f".format(it))};if(n.relevanceScore>0)add("Rel %.0f".format(n.relevanceScore))}.joinToString("  •  ")
        if(meta.isNotBlank())row.addView(TextView(host.root.context).apply{text=meta;textSize=10f;setTextColor(Color.rgb(132,145,170));setPadding(host.dp(72),host.dp(3),0,0)})
        box.addView(row)
        if(index<7)box.addView(TextView(host.root.context).apply{setBackgroundColor(Color.rgb(31,43,64))},LinearLayout.LayoutParams(-1,1))
    }

    private fun open(url:String){runCatching{host.root.context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))}}

    private fun sourceAccent(source:String)=when{source.contains("CNBC",true)->Color.rgb(40,150,255);source.contains("BBC",true)->Color.rgb(235,40,60);source.contains("Financial Times",true)->Color.rgb(30,190,165);source.contains("Bloomberg",true)->Color.rgb(145,70,245);source.contains("MarketWatch",true)->Color.rgb(35,150,245);source.contains("Wall Street",true)->Color.rgb(70,90,120);source.contains("York Times",true)->Color.rgb(215,165,50);source.contains("Reuters",true)->Color.rgb(235,190,40);else->host.accent}
}
