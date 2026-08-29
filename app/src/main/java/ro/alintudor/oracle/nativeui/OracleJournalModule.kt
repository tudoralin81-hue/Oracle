package ro.alintudor.oracle.nativeui

import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.widget.LinearLayout
import android.widget.TextView
import ro.alintudor.oracle.core.*
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** Rich offline activity journal and history timeline. */
class OracleJournalModule(private val host: OracleNativeModule) {
    private val date = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault())

    fun render(journal: List<OracleJournalEntry>, history: List<OracleHistoryPoint>, alerts: List<OracleAlert>) {
        host.content.removeAllViews()
        host.addCard("JURNAL ACTIVITATE", "Istoric complet al acțiunilor, alertelor și mișcărilor Oracle")
        val actions=journal.map{OracleAction(it.ticker,it.action,it.score,it.reason,it.timestamp)}
        val timeline=OracleLocalTimeline.build(history,actions,alerts)
        addSummary(timeline.size,actions.size,alerts.count{it.active})
        if(timeline.isEmpty()){host.addCard("FĂRĂ ACTIVITATE","Nu există încă evenimente locale.");return}
        timeline.take(150).forEachIndexed{i,item->addItem(i+1,item)}
    }
    private fun addSummary(events:Int,actions:Int,activeAlerts:Int){
        val row=LinearLayout(host.root.context).apply{orientation=LinearLayout.HORIZONTAL}
        stat(row,"EVENIMENTE",events.toString(),Color.rgb(70,185,255));stat(row,"ACȚIUNI",actions.toString(),Color.rgb(255,205,45));stat(row,"ALERTE ACTIVE",activeAlerts.toString(),Color.rgb(255,75,60))
        host.content.addView(row,LinearLayout.LayoutParams(-1,-2).apply{setMargins(0,0,0,host.dp(10))})
    }
    private fun stat(row:LinearLayout,label:String,value:String,color:Int){
        val box=LinearLayout(host.root.context).apply{orientation=LinearLayout.VERTICAL;gravity=Gravity.CENTER;setPadding(host.dp(8),host.dp(10),host.dp(8),host.dp(10));background=GradientDrawable().apply{setColor(Color.rgb(7,11,22));cornerRadius=host.dp(11).toFloat();setStroke(host.dp(1),Color.rgb(35,44,66))}}
        box.addView(TextView(host.root.context).apply{text=label;textSize=9f;setTextColor(Color.rgb(145,155,176));gravity=Gravity.CENTER})
        box.addView(TextView(host.root.context).apply{text=value;textSize=17f;typeface=Typeface.DEFAULT_BOLD;setTextColor(color);gravity=Gravity.CENTER})
        row.addView(box,LinearLayout.LayoutParams(0,-2,1f).apply{setMargins(host.dp(3),0,host.dp(3),0)})
    }
    private fun addItem(rank:Int,item:OracleTimelineItem){
        val accent=when(item.severity.uppercase(Locale.getDefault())){"HIGH"->Color.rgb(255,75,60);"MEDIUM"->Color.rgb(255,205,45);else->Color.rgb(70,185,255)}
        val card=LinearLayout(host.root.context).apply{orientation=LinearLayout.VERTICAL;setPadding(host.dp(14),host.dp(12),host.dp(14),host.dp(12));background=GradientDrawable().apply{setColor(Color.rgb(6,10,20));cornerRadius=host.dp(13).toFloat();setStroke(host.dp(1),Color.rgb(38,47,68))}}
        val top=LinearLayout(host.root.context).apply{gravity=Gravity.CENTER_VERTICAL}
        top.addView(TextView(host.root.context).apply{text="%02d".format(rank);textSize=10f;typeface=Typeface.DEFAULT_BOLD;setTextColor(accent);gravity=Gravity.CENTER},LinearLayout.LayoutParams(host.dp(30),host.dp(25)))
        top.addView(TextView(host.root.context).apply{text=item.ticker;textSize=15f;typeface=Typeface.DEFAULT_BOLD;setTextColor(Color.WHITE)},LinearLayout.LayoutParams(0,-2,1f))
        top.addView(TextView(host.root.context).apply{text=item.type.uppercase(Locale.getDefault());textSize=9f;typeface=Typeface.DEFAULT_BOLD;setTextColor(accent);gravity=Gravity.CENTER},LinearLayout.LayoutParams(host.dp(80),host.dp(25)))
        card.addView(top)
        card.addView(TextView(host.root.context).apply{text=date.format(Date(item.timestamp));textSize=10f;setTextColor(Color.rgb(125,137,158));setPadding(host.dp(30),host.dp(3),0,0)})
        card.addView(TextView(host.root.context).apply{text=item.title;textSize=14f;typeface=Typeface.DEFAULT_BOLD;setTextColor(Color.WHITE);setPadding(host.dp(30),host.dp(5),0,0)})
        card.addView(TextView(host.root.context).apply{text=item.detail;textSize=12f;setTextColor(Color.rgb(175,183,201));setPadding(host.dp(30),host.dp(3),0,0)})
        host.content.addView(card,LinearLayout.LayoutParams(-1,-2).apply{setMargins(0,0,0,host.dp(8))})
    }
}
