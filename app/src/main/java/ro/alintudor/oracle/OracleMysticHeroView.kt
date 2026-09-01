package ro.alintudor.oracle

import android.content.Context
import android.graphics.*
import android.view.MotionEvent
import android.view.View
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

/** B514 native Start reference composition. No bitmap/image asset. */
class OracleMysticHeroView(context: Context, private val onModule: (String) -> Unit) : View(context) {
    private val p = Paint(Paint.ANTI_ALIAS_FLAG)
    private val path = Path()
    private val gold = Color.rgb(235, 183, 52)
    private val goldBright = Color.rgb(255, 218, 91)
    private val ivory = Color.rgb(246, 240, 222)
    private val muted = Color.rgb(174, 164, 143)
    private val cards = arrayListOf<Pair<RectF, String>>()

    private data class Node(val key: String, val label: String, val sub: String, val color: Int)
    private val nodes = listOf(
        Node("portfolio", "PORTFOLIO", "OVERVIEW", Color.rgb(205, 70, 255)),
        Node("watchlist", "WATCHLIST", "TRACK & FOCUS", goldBright),
        Node("analysis", "ANALYSIS", "CHARTS & TOOLS", Color.rgb(34, 218, 255)),
        Node("growth", "GROWTH", "FUTURE SCAN", Color.rgb(118, 255, 42)),
        Node("alerts", "ALERTS", "STAY AHEAD", Color.rgb(255, 65, 42)),
        Node("news", "NEWS", "MARKET PULSE", Color.rgb(38, 220, 255)),
        Node("knowledge", "KNOWLEDGE", "LEARN & EVOLVE", goldBright),
        Node("", "STOCK", "INTELLIGENCE", Color.rgb(205, 70, 255))
    )

    override fun onDraw(c: Canvas) {
        super.onDraw(c)
        val w = width.toFloat()
        val h = height.toFloat()
        val scale = min(w / 720f, h / 1150f).coerceAtLeast(0.42f)
        val ox = (w - 720f * scale) / 2f
        fun x(v: Float): Float = ox + v * scale
        fun y(v: Float): Float = v * scale

        p.style = Paint.Style.FILL
        p.shader = LinearGradient(0f, 0f, w, h, Color.rgb(1, 2, 4), Color.rgb(8, 5, 3), Shader.TileMode.CLAMP)
        c.drawRect(0f, 0f, w, h, p)
        p.shader = null

        val cx = x(360f)
        val cy = y(360f)
        val rr = r(325f)

        p.style = Paint.Style.STROKE
        p.strokeWidth = r(0.65f)
        p.color = gold
        p.alpha = 38
        for (i in 1..11) c.drawCircle(cx, cy, rr * i / 11f, p)
        for (i in 0 until 24) {
            val a = i * Math.PI / 12.0
            val ca = cos(a).toFloat()
            val sa = sin(a).toFloat()
            c.drawLine(cx + ca * rr * .18f, cy + sa * rr * .18f, cx + ca * rr, cy + sa * rr, p)
        }
        p.style = Paint.Style.FILL
        p.alpha = 70
        p.color = goldBright
        for (i in 0 until 28) {
            val a = (i * 47 % 360) * Math.PI / 180.0
            val rad = rr * (.45f + (i % 5) * .095f)
            c.drawCircle(cx + cos(a).toFloat() * rad, cy + sin(a).toFloat() * rad, r(if (i % 3 == 0) 1.5f else .75f), p)
        }

        drawButton(c, x(54f), y(52f), r(58f), false)
        drawButton(c, x(666f), y(52f), r(58f), true)
        drawSigil(c, cx, y(126f), r(54f))
        text(c, "ORACLE", cx, y(204f), r(32f), goldBright, Typeface.SERIF, 0f)
        text(c, "STOCK INTELLIGENCE", cx, y(232f), r(11f), goldBright, Typeface.DEFAULT_BOLD, .22f)

        drawEye(c, cx, y(352f), r(208f), goldBright, r(3f))
        p.style = Paint.Style.FILL
        p.color = Color.BLACK
        p.alpha = 255
        c.drawCircle(cx, y(352f), r(48f), p)
        p.style = Paint.Style.STROKE
        p.color = goldBright
        p.alpha = 180
        p.strokeWidth = r(2f)
        c.drawCircle(cx, y(352f), r(51f), p)
        p.strokeWidth = r(.8f)
        p.alpha = 110
        for (i in 0 until 20) {
            val a = i * Math.PI / 10.0
            val ca = cos(a).toFloat()
            val sa = sin(a).toFloat()
            c.drawLine(cx + ca * r(58f), y(352f) + sa * r(58f), cx + ca * r(190f), y(352f) + sa * r(190f), p)
        }
        p.alpha = 190
        c.drawLine(cx, y(42f), cx, y(548f), p)

        text(c, "SEE MORE.  KNOW FIRST.", cx, y(566f), r(11f), ivory, Typeface.DEFAULT, .32f)
        p.strokeWidth = r(.8f)
        p.color = gold
        p.alpha = 190
        c.drawLine(x(220f), y(590f), x(500f), y(590f), p)
        drawDiamond(c, cx, y(590f), r(5f), goldBright)

        cards.clear()
        val left = 20f
        val top = 628f
        val cw = 164f
        val ch = 145f
        val gap = 12f
        for (i in nodes.indices) {
            val col = i % 4
            val row = i / 4
            val l = x(left + col * (cw + gap))
            val t = y(top + row * (ch + gap))
            val rect = RectF(l, t, l + r(cw), t + r(ch))
            if (nodes[i].key.isNotEmpty()) cards.add(rect to nodes[i].key)
            drawModuleCard(c, rect, nodes[i])
        }

        val sr = RectF(x(20f), y(958f), x(700f), y(1050f))
        p.style = Paint.Style.FILL
        p.color = Color.rgb(5, 7, 9)
        p.alpha = 235
        c.drawRoundRect(sr, r(10f), r(10f), p)
        p.style = Paint.Style.STROKE
        p.color = gold
        p.alpha = 170
        p.strokeWidth = r(.9f)
        c.drawRoundRect(sr, r(10f), r(10f), p)
        drawMiniEye(c, x(76f), y(1004f), r(25f), Color.rgb(103, 238, 88))
        textLeft(c, "ORACLE READY", x(112f), y(998f), r(15f), ivory, Typeface.DEFAULT_BOLD, 0f)
        textLeft(c, "Market Intelligence Active", x(112f), y(1021f), r(9.5f), Color.rgb(75, 218, 108), Typeface.DEFAULT, .02f)
        p.style = Paint.Style.STROKE
        p.color = goldBright
        p.alpha = 190
        p.strokeWidth = r(1f)
        c.drawCircle(x(378f), y(1004f), r(25f), p)
        for (i in -2..2) c.drawLine(x(378f + i * 6f), y(992f - abs(i) * 2f), x(378f + i * 6f), y(1016f + abs(i) * 2f), p)
        textLeft(c, "LOCAL INTELLIGENCE", x(416f), y(998f), r(14f), ivory, Typeface.DEFAULT_BOLD, 0f)
        textLeft(c, "Synced & Protected", x(416f), y(1021f), r(9.5f), Color.rgb(122, 202, 117), Typeface.DEFAULT, .02f)
        p.style = Paint.Style.FILL
        p.color = Color.rgb(63, 224, 91)
        p.alpha = 255
        c.drawCircle(x(675f), y(1006f), r(8f), p)
    }

    private fun drawButton(c: Canvas, cx: Float, cy: Float, rad: Float, gear: Boolean) {
        p.style = Paint.Style.STROKE
        p.color = gold
        p.alpha = 215
        p.strokeWidth = r(.95f)
        c.drawRoundRect(RectF(cx - rad, cy - rad, cx + rad, cy + rad), rad * .18f, rad * .18f, p)
        p.strokeWidth = r(2f)
        if (!gear) {
            for (i in -1..1) c.drawLine(cx - rad * .34f, cy + i * rad * .23f, cx + rad * .34f, cy + i * rad * .23f, p)
        } else {
            c.drawCircle(cx, cy, rad * .25f, p)
            c.drawCircle(cx, cy, rad * .42f, p)
            for (i in 0 until 8) {
                val a = i * Math.PI / 4
                c.drawLine(cx + cos(a).toFloat() * rad * .43f, cy + sin(a).toFloat() * rad * .43f, cx + cos(a).toFloat() * rad * .54f, cy + sin(a).toFloat() * rad * .54f, p)
            }
        }
    }

    private fun drawSigil(c: Canvas, cx: Float, cy: Float, rad: Float) {
        p.style = Paint.Style.STROKE
        p.color = goldBright
        p.alpha = 240
        p.strokeWidth = r(1.8f)
        path.reset()
        path.moveTo(cx - rad * .62f, cy)
        path.lineTo(cx, cy - rad * .42f)
        path.lineTo(cx + rad * .62f, cy)
        path.lineTo(cx, cy + rad * .42f)
        path.close()
        c.drawPath(path, p)
        c.drawCircle(cx, cy, rad * .17f, p)
        p.style = Paint.Style.FILL
        c.drawCircle(cx, cy, rad * .055f, p)
        p.style = Paint.Style.STROKE
        p.strokeWidth = r(.8f)
        c.drawLine(cx, cy - rad * .75f, cx, cy - rad * .42f, p)
        c.drawCircle(cx, cy - rad * .82f, rad * .045f, p)
    }

    private fun drawModuleCard(c: Canvas, rect: RectF, node: Node) {
        p.style = Paint.Style.FILL
        p.color = Color.rgb(3, 5, 7)
        p.alpha = 242
        c.drawRoundRect(rect, r(8f), r(8f), p)
        p.style = Paint.Style.STROKE
        p.color = gold
        p.alpha = 210
        p.strokeWidth = r(.9f)
        c.drawRoundRect(rect, r(8f), r(8f), p)
        p.alpha = 90
        p.strokeWidth = r(.55f)
        c.drawLine(rect.left + r(8f), rect.top + r(25f), rect.left + r(8f), rect.top + r(8f), p)
        c.drawLine(rect.left + r(8f), rect.top + r(8f), rect.left + r(25f), rect.top + r(8f), p)
        c.drawLine(rect.right - r(8f), rect.bottom - r(25f), rect.right - r(8f), rect.bottom - r(8f), p)
        c.drawLine(rect.right - r(8f), rect.bottom - r(8f), rect.right - r(25f), rect.bottom - r(8f), p)
        val ix = rect.centerX()
        val iy = rect.top + rect.height() * .37f
        val ir = min(rect.width(), rect.height()) * .23f
        p.style = Paint.Style.STROKE
        p.color = node.color
        p.alpha = 245
        p.strokeWidth = r(1.15f)
        for (k in 0..2) c.drawCircle(ix, iy, ir * (1f - k * .23f), p)
        drawIcon(c, ix, iy, ir, node.key.ifEmpty { "stock" }, node.color)
        text(c, node.label, ix, rect.top + rect.height() * .72f, r(12f), ivory, Typeface.DEFAULT, .01f)
        text(c, node.sub, ix, rect.top + rect.height() * .85f, r(8.3f), muted, Typeface.DEFAULT, .04f)
        p.style = Paint.Style.STROKE
        p.color = node.color
        p.alpha = 230
        p.strokeWidth = r(.75f)
        c.drawLine(ix - r(22f), rect.bottom - r(12f), ix + r(22f), rect.bottom - r(12f), p)
        drawDiamond(c, ix, rect.bottom - r(12f), r(2.6f), node.color)
    }

    private fun drawIcon(c: Canvas, cx: Float, cy: Float, rad: Float, key: String, color: Int) {
        p.style = Paint.Style.STROKE
        p.color = color
        p.alpha = 245
        p.strokeWidth = r(2f)
        when (key) {
            "portfolio" -> { c.drawRect(cx-rad*.45f,cy-rad*.35f,cx+rad*.45f,cy+rad*.4f,p); c.drawCircle(cx+rad*.23f,cy+rad*.18f,rad*.18f,p) }
            "watchlist" -> drawMiniEye(c,cx,cy,rad*.78f,color)
            "analysis" -> { path.reset(); path.moveTo(cx-rad*.6f,cy+rad*.36f); path.lineTo(cx-rad*.22f,cy); path.lineTo(cx+rad*.03f,cy+rad*.15f); path.lineTo(cx+rad*.58f,cy-rad*.5f); c.drawPath(path,p) }
            "growth" -> { path.reset(); path.moveTo(cx-rad*.58f,cy+rad*.42f); path.lineTo(cx-rad*.18f,cy+rad*.08f); path.lineTo(cx+rad*.08f,cy+rad*.2f); path.lineTo(cx+rad*.58f,cy-rad*.48f); c.drawPath(path,p) }
            "alerts" -> { c.drawArc(RectF(cx-rad*.43f,cy-rad*.42f,cx+rad*.43f,cy+rad*.45f),210f,120f,false,p); c.drawLine(cx-rad*.58f,cy+rad*.48f,cx+rad*.58f,cy+rad*.48f,p) }
            "news" -> { c.drawRect(cx-rad*.46f,cy-rad*.43f,cx+rad*.46f,cy+rad*.43f,p); for(i in -1..1)c.drawLine(cx-rad*.28f,cy+i*rad*.18f,cx+rad*.27f,cy+i*rad*.18f,p) }
            "knowledge" -> { c.drawRect(cx-rad*.5f,cy-rad*.45f,cx,cy+rad*.43f,p); c.drawRect(cx,cy-rad*.45f,cx+rad*.5f,cy+rad*.43f,p) }
            else -> drawSigil(c,cx,cy,rad*.8f)
        }
    }

    private fun drawEye(c: Canvas, cx: Float, cy: Float, rad: Float, color: Int, stroke: Float) {
        p.style = Paint.Style.STROKE
        p.color = color
        p.alpha = 245
        p.strokeWidth = stroke
        path.reset()
        path.moveTo(cx-rad,cy)
        path.cubicTo(cx-rad*.52f,cy-rad*.55f,cx+rad*.52f,cy-rad*.55f,cx+rad,cy)
        path.cubicTo(cx+rad*.52f,cy+rad*.55f,cx-rad*.52f,cy+rad*.55f,cx-rad,cy)
        c.drawPath(path,p)
        c.drawCircle(cx,cy,rad*.23f,p)
        p.style = Paint.Style.FILL
        c.drawCircle(cx,cy,rad*.08f,p)
    }

    private fun drawMiniEye(c:Canvas,cx:Float,cy:Float,rad:Float,color:Int)=drawEye(c,cx,cy,rad,color,r(1.25f))

    private fun drawDiamond(c:Canvas,cx:Float,cy:Float,rad:Float,color:Int){
        p.style=Paint.Style.STROKE;p.color=color;p.alpha=225;p.strokeWidth=r(.75f)
        path.reset();path.moveTo(cx,cy-rad);path.lineTo(cx+rad,cy);path.lineTo(cx,cy+rad);path.lineTo(cx-rad,cy);path.close();c.drawPath(path,p)
    }

    private fun text(c:Canvas,value:String,cx:Float,baseY:Float,size:Float,color:Int,typeface:Typeface,spacing:Float){
        p.style=Paint.Style.FILL;p.color=color;p.alpha=255;p.textAlign=Paint.Align.CENTER;p.typeface=typeface;p.textSize=size;p.letterSpacing=spacing;c.drawText(value,cx,baseY,p);p.letterSpacing=0f
    }

    private fun textLeft(c:Canvas,value:String,x:Float,baseY:Float,size:Float,color:Int,typeface:Typeface,spacing:Float){
        p.style=Paint.Style.FILL;p.color=color;p.alpha=255;p.textAlign=Paint.Align.LEFT;p.typeface=typeface;p.textSize=size;p.letterSpacing=spacing;c.drawText(value,x,baseY,p);p.letterSpacing=0f
    }

    private fun r(v:Float):Float= v * min(width/720f,height/1150f).coerceAtLeast(.42f)

    override fun onTouchEvent(e:MotionEvent):Boolean{
        if(e.action==MotionEvent.ACTION_UP){
            for((rect,key) in cards){ if(rect.contains(e.x,e.y)){onModule(key);return true} }
        }
        return true
    }
}

// B514_START_REFERENCE_COMPOSITION_NATIVE_VECTOR_FINAL
