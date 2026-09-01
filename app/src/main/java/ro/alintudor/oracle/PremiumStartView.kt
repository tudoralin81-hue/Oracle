package ro.alintudor.oracle

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import android.os.SystemClock
import android.view.MotionEvent
import android.view.View
import kotlin.math.min
import kotlin.math.sin

/** Premium Oracle Start screen. UI-only surface; module logic remains untouched. */
class PremiumStartView(context: android.content.Context, private val onModule: (String) -> Unit) : View(context) {
    private data class Card(val key: String, val title: String, val subtitle: String, val glyph: String)
    private val cards = listOf(
        Card("portfolio", "PORTFOLIO", "Positions & P/L", "P"), Card("growth", "GROWTH", "Trend & return", "G"),
        Card("analysis", "ANALYSIS", "Oracle signals", "A"), Card("watchlist", "WATCHLIST", "Tracked tickers", "W"),
        Card("alerts", "ALERTS", "Active signals", "!"), Card("news", "NEWS", "Market events", "N"),
        Card("knowledge", "KNOWLEDGE", "Oracle intelligence", "K"), Card("journal", "JOURNAL", "Activity history", "J")
    )
    private val bg = Color.rgb(2, 5, 12); private val panel = Color.rgb(7, 12, 23); private val panel2 = Color.rgb(10, 17, 31)
    private val line = Color.rgb(34, 55, 82); private val cyan = Color.rgb(62, 214, 255); private val white = Color.WHITE
    private val muted = Color.rgb(137, 153, 177); private val gold = Color.rgb(255, 204, 64)
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG); private val cardsBounds = ArrayList<Pair<RectF, String>>()
    private var pressedKey: String? = null; private var animationStart = SystemClock.uptimeMillis()
    init { isFocusable = true; setBackgroundColor(bg) }

    override fun onDraw(c: Canvas) {
        super.onDraw(c); val w = width.toFloat(); val h = height.toFloat(); if (w <= 0f || h <= 0f) return
        paint.shader = LinearGradient(0f, 0f, w, h, bg, Color.rgb(5, 10, 22), Shader.TileMode.CLAMP); c.drawRect(0f, 0f, w, h, paint); paint.shader = null
        drawGrid(c, w, h); drawHeader(c, w); drawPulse(c, w); drawCards(c, w, h); drawFooter(c, w, h)
        val elapsed = SystemClock.uptimeMillis() - animationStart; val sweep = ((elapsed % 5000L) / 5000f) * (w + 180f) - 90f
        paint.color = Color.argb(28, 62, 214, 255); c.drawRect(sweep, 0f, sweep + 2f, h, paint); postInvalidateDelayed(40L)
    }
    private fun drawGrid(c: Canvas, w: Float, h: Float) { paint.style = Paint.Style.STROKE; paint.strokeWidth = 1f; paint.color = Color.argb(22, 100, 150, 220); val step = dp(32f); var x = 0f; while (x <= w) { c.drawLine(x, 0f, x, h, paint); x += step }; var y = 0f; while (y <= h) { c.drawLine(0f, y, w, y, paint); y += step }; paint.style = Paint.Style.FILL }
    private fun drawHeader(c: Canvas, w: Float) { val left = dp(22f); text(c, "ORACLE", left, dp(48f), 30f, white, true, .16f); text(c, "MARKET INTELLIGENCE", left, dp(72f), 10f, cyan, true, .20f); val dotX=w-dp(30f); val pulse=(sin((SystemClock.uptimeMillis()-animationStart)/350.0)+1.0)*.5; paint.color=Color.argb((110+pulse*110).toInt(),62,214,255); c.drawCircle(dotX,dp(42f),dp(4f),paint); text(c,"LIVE",dotX-dp(30f),dp(47f),9f,muted,true,.12f); paint.color=gold; c.drawRect(left,dp(88f),left+dp(54f),dp(90f),paint); paint.color=Color.rgb(27,42,64); c.drawRect(left+dp(62f),dp(89f),w-left,dp(90f),paint); text(c,"COMMAND CENTER",left,dp(119f),11f,muted,true,.14f); text(c,"Choose a module",left,dp(141f),21f,white,true,.01f) }
    private fun drawPulse(c: Canvas, w: Float) { val top=dp(160f); val r=RectF(dp(18f),top,w-dp(18f),top+dp(70f)); rounded(c,r,panel,18f); stroke(c,r,line,1f,18f); text(c,"ORACLE STATUS",r.left+dp(14f),r.top+dp(20f),9f,muted,true,.13f); text(c,"READY",r.left+dp(14f),r.top+dp(43f),18f,cyan,true,.05f); val baseX=r.left+dp(130f); val baseY=r.top+dp(47f); paint.style=Paint.Style.STROKE; paint.strokeWidth=dp(2f); paint.color=Color.rgb(47,93,145); val path=android.graphics.Path(); val samples=34; for(i in 0..samples){val xx=baseX+(i.toFloat()/samples)*(r.width()-dp(148f));val yy=baseY-dp(17f)*(0.45f+0.55f*sin(i*.72+.4).toFloat());if(i==0)path.moveTo(xx,yy)else path.lineTo(xx,yy)}; c.drawPath(path,paint);paint.color=cyan;paint.strokeWidth=dp(2.4f);c.drawPath(path,paint);paint.style=Paint.Style.FILL;text(c,"LOCAL INTELLIGENCE",r.right-dp(116f),r.top+dp(22f),8f,muted,true,.08f);text(c,"8 MODULES",r.right-dp(116f),r.top+dp(44f),12f,white,true,.04f) }
    private fun drawCards(c: Canvas,w: Float,h: Float){cardsBounds.clear();val gap=dp(10f);val side=dp(18f);val top=dp(248f);val footerReserve=dp(70f);val cardW=(w-side*2f-gap)/2f;val cardH=min(dp(84f),(h-top-footerReserve-gap*3f)/4f);cards.forEachIndexed{index,card->val col=index%2;val row=index/2;val l=side+col*(cardW+gap);val t=top+row*(cardH+gap);val rect=RectF(l,t,l+cardW,t+cardH);cardsBounds.add(rect to card.key);val active=pressedKey==card.key;rounded(c,rect,if(active)Color.rgb(12,25,43)else panel2,16f);stroke(c,rect,if(active)cyan else line,if(active)2f else 1f,16f);paint.color=if(active)cyan else Color.rgb(50,80,120);c.drawRect(rect.left,rect.top,rect.left+dp(3f),rect.bottom,paint);val cx=rect.left+dp(25f);val cy=rect.centerY();paint.style=Paint.Style.STROKE;paint.strokeWidth=dp(1.5f);paint.color=if(active)cyan else Color.rgb(68,108,164);c.drawCircle(cx,cy,dp(14f),paint);paint.style=Paint.Style.FILL;textCentered(c,card.glyph,cx,cy+dp(4f),12f,white,true,0f);text(c,card.title,rect.left+dp(50f),rect.top+dp(27f),13f,white,true,.06f);text(c,card.subtitle,rect.left+dp(50f),rect.top+dp(49f),10f,muted,false,0f);text(c,"›",rect.right-dp(22f),rect.centerY()+dp(7f),22f,if(active)cyan else muted,false,0f)}}
    private fun drawFooter(c: Canvas,w: Float,h: Float){val y=h-dp(24f);text(c,"ORACLE  •  PRECISION OVER NOISE",dp(20f),y,9f,muted,true,.12f);text(c,"B514",w-dp(48f),y,9f,gold,true,.10f)}
    override fun onTouchEvent(event: MotionEvent): Boolean { when(event.actionMasked){MotionEvent.ACTION_DOWN->{pressedKey=hit(event.x,event.y);invalidate();return true};MotionEvent.ACTION_UP->{val key=hit(event.x,event.y);val was=pressedKey;pressedKey=null;invalidate();if(key!=null&&key==was){performClick();onModule(key)};return true};MotionEvent.ACTION_CANCEL->{pressedKey=null;invalidate();return true}};return true }
    override fun performClick(): Boolean { super.performClick(); return true }
    private fun hit(x:Float,y:Float):String?=cardsBounds.firstOrNull{it.first.contains(x,y)}?.second
    private fun rounded(c:Canvas,r:RectF,color:Int,radius:Float){paint.style=Paint.Style.FILL;paint.color=color;c.drawRoundRect(r,dp(radius),dp(radius),paint)}
    private fun stroke(c:Canvas,r:RectF,color:Int,width:Float,radius:Float){paint.style=Paint.Style.STROKE;paint.strokeWidth=dp(width);paint.color=color;c.drawRoundRect(r,dp(radius),dp(radius),paint);paint.style=Paint.Style.FILL}
    private fun text(c:Canvas,value:String,x:Float,baseline:Float,size:Float,color:Int,bold:Boolean,spacing:Float){paint.style=Paint.Style.FILL;paint.color=color;paint.textSize=dp(size);paint.typeface=if(bold)Typeface.create(Typeface.DEFAULT,Typeface.BOLD)else Typeface.DEFAULT;paint.letterSpacing=spacing;c.drawText(value,x,baseline,paint)}
    private fun textCentered(c:Canvas,value:String,x:Float,baseline:Float,size:Float,color:Int,bold:Boolean,spacing:Float){paint.textSize=dp(size);paint.typeface=if(bold)Typeface.create(Typeface.DEFAULT,Typeface.BOLD)else Typeface.DEFAULT;paint.letterSpacing=spacing;paint.color=color;c.drawText(value,x-paint.measureText(value)/2f,baseline,paint)}
    private fun dp(v:Float):Float=v*resources.displayMetrics.density
}
