package ro.alintudor.oracle.nativeui

import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.view.Gravity
import android.widget.*

/** Native module container. Refresh is delegated to the activity so the module is rebuilt from local data. */
class OracleNativeModule(
    private val context: Context,
    private val title: String,
    private val onRefresh: () -> Unit = {}
) {
    val root: LinearLayout = LinearLayout(context).apply {
        orientation = LinearLayout.VERTICAL
        setBackgroundColor(Color.rgb(2,4,10))
        setPadding(18,18,18,18)
    }
    val content: LinearLayout = LinearLayout(context).apply {
        orientation = LinearLayout.VERTICAL
        setPadding(0,12,0,0)
    }

    init {
        val header = LinearLayout(context).apply { gravity=Gravity.CENTER_VERTICAL }
        val home = Button(context).apply { text="⌂"; textSize=22f; setOnClickListener { (context as? android.app.Activity)?.onBackPressed() } }
        val label = TextView(context).apply {
            text="ORACLE  •  $title"; textSize=18f; typeface=Typeface.DEFAULT_BOLD; setTextColor(Color.WHITE); gravity=Gravity.CENTER
        }
        val refresh = Button(context).apply { text="↻"; textSize=22f; contentDescription="Refresh $title"; setOnClickListener { onRefresh() } }
        header.addView(home, LinearLayout.LayoutParams(52,52))
        header.addView(label, LinearLayout.LayoutParams(0,52,1f))
        header.addView(refresh, LinearLayout.LayoutParams(52,52))
        root.addView(header)
        root.addView(ScrollView(context).apply { addView(content) }, LinearLayout.LayoutParams(-1,0,1f))
    }

    fun render() {
        content.removeAllViews()
        addCard(title, "Modul Oracle nativ")
    }
    fun addCard(heading:String, body:String) {
        val card=LinearLayout(context).apply { orientation=LinearLayout.VERTICAL; setPadding(18,16,18,16); setBackgroundColor(Color.rgb(9,13,26)) }
        card.addView(TextView(context).apply { text=heading; textSize=19f; typeface=Typeface.DEFAULT_BOLD; setTextColor(Color.WHITE) })
        card.addView(TextView(context).apply { text=body; textSize=16f; setTextColor(Color.LTGRAY); setPadding(0,8,0,0) })
        val lp=LinearLayout.LayoutParams(-1,-2); lp.setMargins(0,0,0,12); content.addView(card,lp)
    }
}
