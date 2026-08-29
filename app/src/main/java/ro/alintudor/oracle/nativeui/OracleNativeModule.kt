package ro.alintudor.oracle.nativeui

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.View
import android.view.WindowInsets
import android.widget.*

class OracleNativeModule(private val context: Context, private val title: String, private val onRefresh: () -> Unit = {}) {
    val root = LinearLayout(context).apply {
        orientation = LinearLayout.VERTICAL
        setBackgroundColor(Color.rgb(1,3,8))
        setPadding(dp(10),dp(6),dp(10),dp(0))
        setOnApplyWindowInsetsListener { view, insets ->
            val top = if (android.os.Build.VERSION.SDK_INT >= 30) insets.getInsets(WindowInsets.Type.statusBars()).top else insets.systemWindowInsetTop
            val bottom = if (android.os.Build.VERSION.SDK_INT >= 30) insets.getInsets(WindowInsets.Type.navigationBars()).bottom else insets.systemWindowInsetBottom
            // Keep every Oracle module inside the Android safe area. The shared shell
            // is used by Portfolio, Alerts, News, Growth, Knowledge, Analysis,
            // Watchlist and Journal, so the fix applies consistently everywhere.
            view.setPadding(dp(10), dp(6) + top, dp(10), dp(10) + bottom)
            insets
        }
        post { requestApplyInsets() }
    }
    val content = LinearLayout(context).apply {
        orientation=LinearLayout.VERTICAL
        // Extra scrollable space keeps the last cards/buttons above Android navigation controls.
        setPadding(dp(2),dp(6),dp(2),dp(48))
    }
    val accent = when(title.uppercase()) {
        "ALERTS" -> Color.rgb(255,75,40)
        "NEWS","ANALYSIS" -> Color.rgb(25,205,255)
        "GROWTH" -> Color.rgb(145,245,35)
        "PORTFOLIO" -> Color.rgb(190,65,255)
        else -> Color.rgb(255,210,45)
    }
    init {
        // Taller shared header prevents the module title from being clipped by the divider.
        val header=LinearLayout(context).apply { gravity=Gravity.CENTER_VERTICAL; setPadding(dp(2),dp(3),dp(2),dp(5)) }
        header.addView(button("‹","Home",Color.rgb(255,205,45)){(context as? android.app.Activity)?.onBackPressed()},LinearLayout.LayoutParams(dp(46),dp(46)))
        val center=LinearLayout(context).apply{orientation=LinearLayout.VERTICAL;gravity=Gravity.CENTER}
        center.addView(TextView(context).apply{text="ORACLE";textSize=21f;typeface=Typeface.create(Typeface.SERIF,Typeface.BOLD);setTextColor(Color.WHITE);gravity=Gravity.CENTER;includeFontPadding=true})
        center.addView(TextView(context).apply{text=title;textSize=11f;typeface=Typeface.DEFAULT_BOLD;letterSpacing=.18f;setTextColor(accent);gravity=Gravity.CENTER;includeFontPadding=true})
        header.addView(center,LinearLayout.LayoutParams(0,dp(54),1f))
        header.addView(button("↻","Refresh",Color.rgb(255,205,45)){onRefresh()},LinearLayout.LayoutParams(dp(46),dp(46)))
        root.addView(header,LinearLayout.LayoutParams(-1,dp(62)))
        if(title.uppercase()=="GROWTH") root.addView(GrowthBanner(context),LinearLayout.LayoutParams(-1,dp(132)).apply{setMargins(dp(2),dp(1),dp(2),dp(8))})
        root.addView(View(context).apply{setBackgroundColor(accent)},LinearLayout.LayoutParams(-1,dp(1)).apply{setMargins(dp(6),0,dp(6),dp(5))})
        root.addView(ScrollView(context).apply{clipToPadding=false;addView(content)},LinearLayout.LayoutParams(-1,0,1f))
    }
    private fun button(symbol:String,desc:String,color:Int,click:()->Unit)=TextView(context).apply{
        text=symbol;textSize=30f;gravity=Gravity.CENTER;contentDescription=desc;typeface=Typeface.DEFAULT_BOLD;setTextColor(Color.WHITE)
        background=rounded(Color.rgb(5,8,17),dp(13),color,dp(1));isClickable=true;isFocusable=true;setOnClickListener{click()}
    }
    fun addCard(heading:String,body:String){
        val card=LinearLayout(context).apply{orientation=LinearLayout.VERTICAL;setPadding(dp(16),dp(14),dp(16),dp(14));background=rounded(Color.rgb(7,11,22),dp(15),Color.rgb(42,52,76),dp(1))}
        card.addView(TextView(context).apply{text=heading.uppercase();textSize=17f;typeface=Typeface.DEFAULT_BOLD;letterSpacing=.04f;setTextColor(Color.WHITE)})
        card.addView(TextView(context).apply{text=body;textSize=14f;setTextColor(Color.rgb(175,182,198));setPadding(0,dp(7),0,0)})
        content.addView(card,LinearLayout.LayoutParams(-1,-2).apply{setMargins(0,0,0,dp(10))})
    }
    fun addSectionLabel(text:String,sectionAccent:Int=accent){content.addView(TextView(context).apply{this.text=text.uppercase();textSize=11f;typeface=Typeface.DEFAULT_BOLD;letterSpacing=.14f;setTextColor(sectionAccent);setPadding(dp(5),dp(8),dp(5),dp(7))})}
    fun dp(v:Int)= (v*context.resources.displayMetrics.density).toInt()
    companion object{fun rounded(fill:Int,radius:Int,stroke:Int=Color.TRANSPARENT,strokeWidth:Int=0)=GradientDrawable().apply{setColor(fill);cornerRadius=radius.toFloat();if(strokeWidth>0)setStroke(strokeWidth,stroke)}}
}

private class GrowthBanner(context:Context):View(context){
    private val p=Paint(Paint.ANTI_ALIAS_FLAG)
    override fun onDraw(c:Canvas){
        val w=width.toFloat();val h=height.toFloat();val d=resources.displayMetrics.density;val g=Color.rgb(145,245,35)
        p.style=Paint.Style.FILL;p.color=Color.rgb(3,14,9);c.drawRoundRect(0f,0f,w,h,16*d,16*d,p)
        p.style=Paint.Style.STROKE;p.strokeWidth=1.2f*d;p.color=Color.rgb(75,135,30);c.drawRoundRect(.5f,.5f,w-.5f,h-.5f,16*d,16*d,p)
        p.style=Paint.Style.FILL;p.color=Color.argb(55,145,245,35);c.drawCircle(70*d,h*.5f,34*d,p)
        p.color=g;p.textAlign=Paint.Align.CENTER;p.typeface=Typeface.DEFAULT_BOLD;p.textSize=38*d;c.drawText("↗",70*d,h*.61f,p)
        p.textAlign=Paint.Align.LEFT;p.textSize=23*d;c.drawText("GROWTH",126*d,48*d,p)
        p.textSize=14*d;p.color=Color.WHITE;p.typeface=Typeface.DEFAULT;c.drawText("Randament, trend local și",126*d,76*d,p);c.drawText("contribuție la portofoliu",126*d,97*d,p)
        p.style=Paint.Style.STROKE;p.strokeWidth=2*d;p.color=g;val path=Path();val x=w*.70f;val b=h*.80f
        path.moveTo(x,b);for(i in 1..12){val xx=x+w*.025f*i;val yy=b-d*(8+i*4+(i%3)*8);path.lineTo(xx,yy)};c.drawPath(path,p);p.style=Paint.Style.FILL;c.drawCircle(x+w*.30f,b-d*76,4*d,p)
    }
}
