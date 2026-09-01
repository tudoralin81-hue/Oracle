package ro.alintudor.oracle

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Typeface
import android.view.MotionEvent
import android.view.View
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

/** B514 native Start UI. No image asset. Protected modules are untouched. */
class OracleMysticStartView(
    context: Context,
    private val onModule: (String) -> Unit
) : View(context) {
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val path = Path()
    private val hits = mutableListOf<Pair<RectF, String>>()
    private val gold = Color.rgb(238, 190, 60)
    private val brightGold = Color.rgb(255, 215, 82)
    private val white = Color.rgb(244, 239, 226)
    private val muted = Color.rgb(151, 143, 126)
    private val modules = listOf(
        Module("portfolio", "PORTFOLIO", "OVERVIEW", Color.rgb(205, 65, 255)),
        Module("watchlist", "WATCHLIST", "TRACK & FOCUS", Color.rgb(255, 202, 35)),
        Module("analysis", "ANALYSIS", "CHARTS & TOOLS", Color.rgb(25, 220, 255)),
        Module("growth", "GROWTH", "FUTURE SCAN", Color.rgb(120, 255, 45)),
        Module("alerts", "ALERTS", "STAY AHEAD", Color.rgb(255, 55, 35)),
        Module("news", "NEWS", "MARKET PULSE", Color.rgb(30, 210, 255)),
        Module("knowledge", "KNOWLEDGE", "LEARN & EVOLVE", Color.rgb(255, 205, 45)),
        Module("stock", "STOCK", "INTELLIGENCE", Color.rgb(215, 60, 255))
    )
    private data class Module(val key: String, val title: String, val subtitle: String, val color: Int)

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val w = width.toFloat(); val h = height.toFloat()
        val scale = min(w / 720f, h / 1180f).coerceAtLeast(0.42f)
        val ox = (w - 720f * scale) / 2f
        fun sx(v: Float) = ox + v * scale
        fun sy(v: Float) = v * scale
        fun ss(v: Float) = v * scale
        paint.style = Paint.Style.FILL; paint.color = Color.rgb(1, 2, 4); paint.alpha = 255
        canvas.drawRect(0f, 0f, w, h, paint)

        val cx = sx(360f); val eyeY = sy(270f)
        // Fine orbital geometry: thin, quiet background lines.
        paint.style = Paint.Style.STROKE; paint.color = gold; paint.alpha = 38; paint.strokeWidth = ss(0.55f)
        for (i in 0 until 13) canvas.drawCircle(cx, eyeY, ss(105f + i * 19f), paint)
        for (i in 0 until 32) {
            val a = i * Math.PI / 16.0; val c = cos(a).toFloat(); val s = sin(a).toFloat()
            canvas.drawLine(cx + c * ss(90f), eyeY + s * ss(90f), cx + c * ss(335f), eyeY + s * ss(335f), paint)
        }

        drawCentered(canvas, "STOCK INTELLIGENCE", cx, sy(30f), ss(9f), gold, Typeface.DEFAULT_BOLD, 0.08f)
        drawSigil(canvas, cx, sy(75f), ss(28f), brightGold)
        drawCentered(canvas, "ORACLE", cx, sy(130f), ss(31f), brightGold, Typeface.SERIF, 0.18f)
        drawCentered(canvas, "STOCK INTELLIGENCE", cx, sy(155f), ss(9.5f), gold, Typeface.DEFAULT, 0.25f)

        val t = System.nanoTime() / 1_000_000_000.0
        drawAnimatedEye(canvas, cx, eyeY, ss(140f), t)
        drawCentered(canvas, "SEE MORE.  KNOW FIRST.", cx, sy(468f), ss(10.5f), white, Typeface.DEFAULT, 0.27f)
        paint.style = Paint.Style.STROKE; paint.color = gold; paint.alpha = 185; paint.strokeWidth = ss(0.7f)
        canvas.drawLine(sx(220f), sy(490f), sx(500f), sy(490f), paint)
        drawDiamond(canvas, cx, sy(490f), ss(4f), brightGold)

        hits.clear()
        val left = 20f; val top = 514f; val cw = 164f; val ch = 132f; val gap = 12f
        for (i in modules.indices) {
            val col = i % 4; val row = i / 4
            val r = RectF(sx(left + col * (cw + gap)), sy(top + row * (ch + gap)), sx(left + col * (cw + gap) + cw), sy(top + row * (ch + gap) + ch))
            hits += r to modules[i].key
            drawCard(canvas, r, modules[i], scale, t, i)
        }

        drawStatus(canvas, ::sx, ::sy, ::ss, t)
        drawCentered(canvas, "ORACLE", cx, sy(955f), ss(20f), gold, Typeface.SERIF, 0.34f)
        drawCentered(canvas, "SEE MORE.  KNOW FIRST.", cx, sy(981f), ss(8f), muted, Typeface.DEFAULT, 0.22f)
        paint.style = Paint.Style.STROKE; paint.color = gold; paint.alpha = 145; paint.strokeWidth = ss(0.6f)
        canvas.drawLine(sx(285f), sy(1002f), sx(435f), sy(1002f), paint); drawDiamond(canvas, cx, sy(1002f), ss(3f), gold)
        drawCentered(canvas, "357AT2026", cx, sy(1045f), ss(11f), brightGold, Typeface.DEFAULT_BOLD, 0.16f)
        postInvalidateDelayed(32L)
    }

    private fun drawAnimatedEye(c: Canvas, x: Float, y: Float, r: Float, time: Double) {
        val pulse = (0.5 + 0.5 * sin(time * 2.2)).toFloat()
        val colors = intArrayOf(Color.rgb(255, 65, 170), Color.rgb(30, 220, 255), Color.rgb(255, 195, 40))
        paint.style = Paint.Style.STROKE
        for (i in 0..2) {
            paint.color = colors[i]; paint.alpha = (95 + pulse * 100).toInt(); paint.strokeWidth = r * (0.012f + i * 0.004f)
            path.reset(); path.moveTo(x-r, y)
            path.cubicTo(x-r*.58f, y-r*.55f, x+r*.58f, y-r*.55f, x+r, y)
            path.cubicTo(x+r*.58f, y+r*.55f, x-r*.58f, y+r*.55f, x-r, y)
            c.drawPath(path, paint)
        }
        paint.style = Paint.Style.FILL; paint.color = Color.BLACK; paint.alpha = 255; c.drawCircle(x, y, r*.36f, paint)
        paint.style = Paint.Style.STROKE; paint.color = brightGold; paint.alpha = 255; paint.strokeWidth = r*.022f; c.drawCircle(x, y, r*.39f, paint)
        paint.style = Paint.Style.FILL; paint.color = Color.rgb(255, 190, 45); paint.alpha = 255
        c.drawCircle(x, y, r*(.13f + .025f*pulse), paint)
        paint.style = Paint.Style.STROKE; paint.color = Color.rgb(255, 100, 35); paint.alpha = 170; paint.strokeWidth = r*.012f
        for (i in 0 until 28) { val a=i*Math.PI/14.0; val inner=r*(.47f); val outer=r*(.66f+.05f*pulse); c.drawLine(x+cos(a).toFloat()*inner,y+sin(a).toFloat()*inner,x+cos(a).toFloat()*outer,y+sin(a).toFloat()*outer,paint) }
    }

    private fun drawCard(c: Canvas, r: RectF, m: Module, scale: Float, time: Double, index: Int) {
        fun s(v: Float)=v*scale
        val pulse=(.5+.5*sin(time*1.8+index*.55)).toFloat()
        val cx=r.centerX(); val cy=r.top+r.height()*.37f; val ir=min(r.width(),r.height())*.25f
        paint.style=Paint.Style.FILL; paint.color=Color.rgb(3,4,7); paint.alpha=248; c.drawRoundRect(r,s(10f),s(10f),paint)
        // Vivid animated border and glow rings.
        paint.style=Paint.Style.STROKE; paint.color=m.color; paint.alpha=(155+80*pulse).toInt(); paint.strokeWidth=s(1.15f); c.drawRoundRect(r,s(10f),s(10f),paint)
        paint.alpha=(45+70*pulse).toInt(); paint.strokeWidth=s(1.0f); c.drawCircle(cx,cy,ir*(1.10f+.07f*pulse),paint)
        paint.alpha=210; paint.strokeWidth=s(1.1f); c.drawCircle(cx,cy,ir,paint); paint.alpha=125; c.drawCircle(cx,cy,ir*.78f,paint); paint.alpha=95; c.drawCircle(cx,cy,ir*.58f,paint)
        paint.alpha=255; paint.strokeWidth=s(1.9f)
        when(m.key){
            "watchlist" -> drawEyeIcon(c,cx,cy,ir*.70f,m.color)
            "portfolio" -> { c.drawRect(cx-ir*.45f,cy-ir*.35f,cx+ir*.45f,cy+ir*.35f,paint); c.drawCircle(cx+ir*.22f,cy+ir*.15f,ir*.17f,paint) }
            "analysis","growth" -> { path.reset(); path.moveTo(cx-ir*.60f,cy+ir*.35f); path.lineTo(cx-ir*.20f,cy); path.lineTo(cx+ir*.02f,cy+ir*.15f); path.lineTo(cx+ir*.58f,cy-ir*.48f); c.drawPath(path,paint) }
            "alerts" -> { c.drawArc(RectF(cx-ir*.48f,cy-ir*.45f,cx+ir*.48f,cy+ir*.45f),210f,120f,false,paint); c.drawLine(cx-ir*.22f,cy+ir*.40f,cx+ir*.22f,cy+ir*.40f,paint) }
            "news" -> { c.drawRect(cx-ir*.46f,cy-ir*.43f,cx+ir*.46f,cy+ir*.43f,paint); for(i in -1..1)c.drawLine(cx-ir*.28f,cy+i*ir*.18f,cx+ir*.28f,cy+i*ir*.18f,paint) }
            "knowledge" -> { c.drawRect(cx-ir*.48f,cy-ir*.43f,cx,cy+ir*.43f,paint); c.drawRect(cx,cy-ir*.43f,cx+ir*.48f,cy+ir*.43f,paint) }
            else -> { paint.strokeWidth=s(1.8f); drawSigil(c,cx,cy,ir*.70f,m.color) }
        }
        drawCentered(c,m.title,cx,r.top+r.height()*.72f,s(11.5f),white,Typeface.DEFAULT,0.01f)
        drawCentered(c,m.subtitle,cx,r.top+r.height()*.87f,s(7.8f),m.color,Typeface.DEFAULT,0.02f)
        paint.style=Paint.Style.STROKE; paint.color=m.color; paint.alpha=(130+100*pulse).toInt(); paint.strokeWidth=s(1f)
        c.drawLine(cx-s(35f),r.bottom-s(13f),cx+s(35f),r.bottom-s(13f),paint); drawDiamond(c,cx,r.bottom-s(13f),s(3.5f),m.color)
    }

    private fun drawStatus(c:Canvas,sx:(Float)->Float,sy:(Float)->Float,ss:(Float)->Float,time:Double){
        val r=RectF(sx(20f),sy(814f),sx(700f),sy(902f)); val p=(.5+.5*sin(time*1.5)).toFloat()
        paint.style=Paint.Style.FILL;paint.color=Color.rgb(4,6,9);paint.alpha=248;c.drawRoundRect(r,ss(10f),ss(10f),paint)
        paint.style=Paint.Style.STROKE;paint.color=Color.rgb(120,220,60);paint.alpha=(130+80*p).toInt();paint.strokeWidth=ss(1f);c.drawRoundRect(r,ss(10f),ss(10f),paint)
        drawMiniEye(c,sx(72f),sy(858f),ss(24f),Color.rgb(105,245,88),p)
        drawLeft(c,"ORACLE READY",sx(112f),sy(852f),ss(14f),white,Typeface.DEFAULT_BOLD);drawLeft(c,"Market Intelligence Active",sx(112f),sy(875f),ss(9f),Color.rgb(70,235,105),Typeface.DEFAULT)
        paint.color=gold;paint.alpha=210;paint.strokeWidth=ss(.9f);c.drawCircle(sx(378f),sy(858f),ss(24f),paint)
        for(i in -2..2){val x=sx(378f+i*6f);val half=sy(10f+kotlin.math.abs(i)*2f);c.drawLine(x,sy(858f)-half,x,sy(858f)+half,paint)}
        drawLeft(c,"LOCAL INTELLIGENCE",sx(416f),sy(852f),ss(13f),white,Typeface.DEFAULT_BOLD);drawLeft(c,"Synced & Protected",sx(416f),sy(875f),ss(9f),Color.rgb(135,215,130),Typeface.DEFAULT)
        paint.style=Paint.Style.FILL;paint.color=Color.rgb(65,240,95);paint.alpha=255;c.drawCircle(sx(675f),sy(860f),ss(8f+2f*p),paint)
    }

    private fun drawMiniEye(c:Canvas,x:Float,y:Float,r:Float,color:Int,pulse:Float){paint.style=Paint.Style.STROKE;paint.color=color;paint.alpha=(170+70*pulse).toInt();paint.strokeWidth=r*.10f;path.reset();path.moveTo(x-r,y);path.cubicTo(x-r*.55f,y-r*.55f,x+r*.55f,y-r*.55f,x+r,y);path.cubicTo(x+r*.55f,y+r*.55f,x-r*.55f,y+r*.55f,x-r,y);c.drawPath(path,paint);c.drawCircle(x,y,r*.24f,paint)}
    private fun drawEyeIcon(c:Canvas,x:Float,y:Float,r:Float,color:Int){paint.style=Paint.Style.STROKE;paint.color=color;paint.alpha=255;paint.strokeWidth=r*.09f;path.reset();path.moveTo(x-r,y);path.cubicTo(x-r*.55f,y-r*.55f,x+r*.55f,y-r*.55f,x+r,y);path.cubicTo(x+r*.55f,y+r*.55f,x-r*.55f,y+r*.55f,x-r,y);c.drawPath(path,paint);c.drawCircle(x,y,r*.23f,paint)}
    private fun drawSigil(c:Canvas,x:Float,y:Float,r:Float,color:Int){paint.style=Paint.Style.STROKE;paint.color=color;paint.alpha=245;paint.strokeWidth=r*.065f;path.reset();path.moveTo(x-r*.75f,y);path.lineTo(x,y-r*.5f);path.lineTo(x+r*.75f,y);path.lineTo(x,y+r*.5f);path.close();c.drawPath(path,paint);c.drawCircle(x,y,r*.18f,paint)}
    private fun drawDiamond(c:Canvas,x:Float,y:Float,r:Float,color:Int){paint.style=Paint.Style.STROKE;paint.color=color;paint.alpha=230;paint.strokeWidth=r*.32f;path.reset();path.moveTo(x,y-r);path.lineTo(x+r,y);path.lineTo(x,y+r);path.lineTo(x-r,y);path.close();c.drawPath(path,paint)}
    private fun drawCentered(c:Canvas,text:String,x:Float,y:Float,size:Float,color:Int,typeface:Typeface,spacing:Float){paint.style=Paint.Style.FILL;paint.color=color;paint.alpha=255;paint.typeface=typeface;paint.textSize=size;paint.textAlign=Paint.Align.CENTER;paint.letterSpacing=spacing;c.drawText(text,x,y,paint);paint.letterSpacing=0f}
    private fun drawLeft(c:Canvas,text:String,x:Float,y:Float,size:Float,color:Int,typeface:Typeface){paint.style=Paint.Style.FILL;paint.color=color;paint.alpha=255;paint.typeface=typeface;paint.textSize=size;paint.textAlign=Paint.Align.LEFT;c.drawText(text,x,y,paint)}

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if(event.action==MotionEvent.ACTION_UP){ for((r,key) in hits) if(r.contains(event.x,event.y)){ onModule(key); performClick(); return true } }
        return true
    }
    override fun performClick(): Boolean { super.performClick(); return true }
}
