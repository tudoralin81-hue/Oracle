package ro.alintudor.oracle.nativeui

import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowInsets
import android.widget.*
import android.widget.FrameLayout

/** Shared Oracle module shell. Header semantics are fixed: left=Back, right=Refresh. */
class OracleNativeModule(
    private val context: Context,
    private val title: String,
    private val onBack: () -> Unit = {},
    private val onRefresh: () -> Unit = {}
) {
    val root = LinearLayout(context).apply {
        orientation = LinearLayout.VERTICAL
        setBackgroundColor(Color.rgb(1,3,8))
        setPadding(dp(10),0,dp(10),0)
    }
    val fixedToolbar = LinearLayout(context).apply {
        orientation = LinearLayout.VERTICAL
        setPadding(dp(2),0,dp(2),dp(4))
    }
    val content = LinearLayout(context).apply {
        orientation = LinearLayout.VERTICAL
        setPadding(dp(2),dp(10),dp(2),dp(24))
    }
    val accent = when(title.uppercase()) {
        "ALERTS" -> Color.rgb(255,75,40)
        "NEWS","ANALYSIS" -> Color.rgb(25,205,255)
        "GROWTH" -> Color.rgb(145,245,35)
        "PORTFOLIO" -> Color.rgb(190,65,255)
        else -> Color.rgb(255,210,45)
    }
    private lateinit var scrollView: ScrollView

    init {
        val header = LinearLayout(context).apply {
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(2),dp(5),dp(2),dp(5))
        }
        header.addView(button("‹","Back",Color.rgb(255,205,45)) { onBack() }, LinearLayout.LayoutParams(dp(46),dp(46)))
        val center = LinearLayout(context).apply { orientation=LinearLayout.VERTICAL; gravity=Gravity.CENTER }
        center.addView(TextView(context).apply { text="ORACLE";textSize=21f;typeface=Typeface.create(Typeface.SERIF,Typeface.BOLD);setTextColor(Color.WHITE);gravity=Gravity.CENTER;includeFontPadding=true })
        center.addView(TextView(context).apply { text=title;textSize=11f;typeface=Typeface.DEFAULT_BOLD;letterSpacing=.18f;setTextColor(accent);gravity=Gravity.CENTER;includeFontPadding=true })
        header.addView(center,LinearLayout.LayoutParams(0,dp(54),1f))
        header.addView(button("↻","Refresh",Color.rgb(255,205,45)) { onRefresh() }, LinearLayout.LayoutParams(dp(46),dp(46)))
        root.addView(header,LinearLayout.LayoutParams(-1,dp(62)))
        root.addView(View(context).apply{setBackgroundColor(accent)},LinearLayout.LayoutParams(-1,dp(1)).apply{setMargins(dp(6),0,dp(6),dp(5))})
        root.addView(fixedToolbar,LinearLayout.LayoutParams(-1,-2))

        scrollView = ScrollView(context).apply {
            clipToPadding=false
            isFillViewport=true
            overScrollMode=View.OVER_SCROLL_ALWAYS
            isNestedScrollingEnabled=false
            addView(content)
            setOnScrollChangeListener { _, _, scrollY, _, _ -> scrollPositions[title] = scrollY }
        }
        val pullContainer = PullRefreshLayout(context) { onRefresh() }
        pullContainer.addView(scrollView, FrameLayout.LayoutParams(-1,-1))
        root.addView(pullContainer, LinearLayout.LayoutParams(-1,0,1f))
        root.setOnApplyWindowInsetsListener { _, insets ->
            val top = if (android.os.Build.VERSION.SDK_INT >= 30) insets.getInsets(WindowInsets.Type.statusBars()).top else 0
            val bottom = if (android.os.Build.VERSION.SDK_INT >= 30) insets.getInsets(WindowInsets.Type.navigationBars()).bottom else 0
            root.setPadding(dp(10), top + dp(2), dp(10), bottom)
            content.setPadding(dp(2),dp(10),dp(2),bottom + dp(24))
            insets
        }
        root.requestApplyInsets()
        scrollView.post { scrollView.scrollTo(0, scrollPositions[title] ?: 0) }
    }

    fun getScrollY(): Int = if (::scrollView.isInitialized) scrollView.scrollY else 0
    fun restoreScrollY(value: Int) {
        if (!::scrollView.isInitialized) return
        scrollPositions[title] = value.coerceAtLeast(0)
        scrollView.post { scrollView.scrollTo(0, value.coerceAtLeast(0)) }
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
    companion object{
        private val scrollPositions = mutableMapOf<String, Int>()
        fun rememberedScroll(title:String): Int = scrollPositions[title] ?: 0
        fun rounded(fill:Int,radius:Int,stroke:Int=Color.TRANSPARENT,strokeWidth:Int=0)=GradientDrawable().apply{setColor(fill);cornerRadius=radius.toFloat();if(strokeWidth>0)setStroke(strokeWidth,stroke)}
    }
}

/** Dependency-free pull-to-refresh. It takes the gesture only when the list is at the top. */
private class PullRefreshLayout(
    context: Context,
    private val refresh: () -> Unit
) : FrameLayout(context) {
    private var downY = 0f
    private var dragging = false
    private var triggered = false
    private val threshold = (72f * resources.displayMetrics.density)

    init {
        clipChildren = false
        setWillNotDraw(false)
    }

    override fun onInterceptTouchEvent(ev: MotionEvent): Boolean {
        val child = getChildAt(0)
        if (child == null) return false
        when (ev.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                downY = ev.rawY
                dragging = false
                triggered = false
                return false
            }
            MotionEvent.ACTION_MOVE -> {
                val dy = ev.rawY - downY
                // Only intercept a clear downward pull from the very top.
                if (dy > (12f * resources.displayMetrics.density) && !child.canScrollVertically(-1)) {
                    dragging = true
                    return true
                }
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> dragging = false
        }
        return dragging
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        val child = getChildAt(0) ?: return false
        when (event.actionMasked) {
            MotionEvent.ACTION_MOVE -> {
                val pull = (event.rawY - downY).coerceAtLeast(0f)
                child.translationY = pull * 0.55f
                triggered = pull >= threshold
                return true
            }
            MotionEvent.ACTION_UP -> {
                if (triggered) refresh()
                child.animate().translationY(0f).setDuration(180L).start()
                dragging = false
                triggered = false
                return true
            }
            MotionEvent.ACTION_CANCEL -> {
                child.animate().translationY(0f).setDuration(120L).start()
                dragging = false
                triggered = false
                return true
            }
        }
        return true
    }
}
