package ro.alintudor.oracle

import android.content.Context
import android.graphics.*
import android.view.MotionEvent
import android.view.View
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

/**
 * B514 Start only. Native Canvas composition; deliberately no bitmap/image asset.
 * Frozen modules are reached only through their existing callbacks.
 */
class OracleMysticHeroView(context: Context, private val onModule: (String) -> Unit) : View(context) {
    private val p = Paint(Paint.ANTI_ALIAS_FLAG)
    private val path = Path()
    private val cards = ArrayList<Pair<RectF, String>>()
    private val gold = Color.rgb(236, 188, 64)
    private val brightGold = Color.rgb(255, 215, 82)
    private val ivory = Color.rgb(242, 237, 224)
    private val dim = Color.rgb(151, 143, 125)

    private data class Node(val key: String, val title: String, val sub: String, val color: Int)
    private val nodes = listOf(
        Node("portfolio", "PORTFOLIO", "OVERVIEW", Color.rgb(201, 65, 255)),
        Node("watchlist", "WATCHLIST", "TRACK & FOCUS", Color.rgb(247, 198, 55)),
        Node("analysis", "ANALYSIS", "CHARTS & TOOLS", Color.rgb(45, 214, 255)),
        Node("growth", "GROWTH", "FUTURE SCAN", Color.rgb(119, 245, 55)),
        Node("alerts", "ALERTS", "STAY AHEAD", Color.rgb(255, 68, 48)),
        Node("news", "NEWS", "MARKET PULSE", Color.rgb(42, 211, 255)),
        Node("knowledge", "KNOWLEDGE", "LEARN & EVOLVE", Color.rgb(246, 198, 52)),
        Node("stock", "STOCK", "INTELLIGENCE", Color.rgb(199, 72, 255))
    )

    override fun onDraw(c: Canvas) {
        super.onDraw(c)
        val w = width.toFloat()
        val h = height.toFloat()
        val s = min(w / 720f, h / 1180f).coerceAtLeast(.45f)
        val ox = (w - 720f * s) / 2f
        fun X(v: Float) = ox + v * s
        fun Y(v: Float) = v * s
        fun R(v: Float) = v * s

        p.shader = null
        p.style = Paint.Style.FILL
        p.color = Color.rgb(2, 3, 6)
        p.alpha = 255
        c.drawRect(0f, 0f, w, h, p)

        // Fine mystical geometry behind the hero; thin, restrained lines.
        val cx = X(360f)
        val heroCy = Y(302f)
        p.style = Paint.Style.STROKE
        p.strokeWidth = R(.55f)
        p.color = gold
        p.alpha = 55
        for (r0 in 155..330 step 18) c.drawCircle(cx, heroCy, R(r0.toFloat()), p)
        for (i in 0 until 20) {
            val a = i * Math.PI / 10.0
            val x1 = cx + cos(a).toFloat() * R(110f)
            val y1 = heroCy + sin(a).toFloat() * R(110f)
            val x2 = cx + cos(a).toFloat() * R(330f)
            val y2 = heroCy + sin(a).toFloat() * R(330f)
            c.drawLine(x1, y1, x2, y2, p)
        }
        p.alpha = 150
        for (i in 0 until 34) {
            val a = (i * 71 % 360) * Math.PI / 180.0
            val rad = R(165f + (i % 7) * 21f)
            c.drawCircle(cx + cos(a).toFloat() * rad, heroCy + sin(a).toFloat() * rad, R(if (i % 4 == 0) 1.6f else .7f), p)
        }

        drawTopButton(c, X(58f), Y(55f), R(29f), false)
        drawTopButton(c, X(662f), Y(55f), R(29f), true)

        text(c, "STOCK INTELLIGENCE", cx, Y(24f), R(10f), gold, Typeface.DEFAULT_BOLD, .08f)
        drawOracleEye(c, cx, Y(82f), R(31f), brightGold)
        text(c, "ORACLE", cx, Y(139f), R(31f), brightGold, Typeface.SERIF, .17f)
        text(c, "STOCK INTELLIGENCE", cx, Y(165f), R(10f), gold, Typeface.DEFAULT, .26f)

        // Hero eye: organic eye silhouette, but fully native/vector.
        drawEye(c, cx, heroCy + Y(8f), R(145f), brightGold)
        p.style = Paint.Style.FILL
        p.color = Color.BLACK
        p.alpha = 255
        c.drawCircle(cx, heroCy + Y(8f), R(54f), p)
        p.style = Paint.Style.STROKE
        p.color = brightGold
        p.alpha = 225
        p.strokeWidth = R(2.2f)
        c.drawCircle(cx, heroCy + Y(8f), R(59f), p)
        p.strokeWidth = R(.8f)
        p.alpha = 145
        for (i in 0 until 28) {
            val a = i * Math.PI / 14.0
            c.drawLine(cx + cos(a).toFloat() * R(66f), heroCy + Y(8f) + sin(a).toFloat() * R(66f), cx + cos(a).toFloat() * R(145f), heroCy + Y(8f) + sin(a).toFloat() * R(145f), p)
        }
        p.alpha = 210
        c.drawLine(cx, Y(45f), cx, heroCy - Y(145f), p)
        c.drawLine(cx, heroCy + Y(153f), cx, Y(500f), p)

        text(c, "SEE MORE.  KNOW FIRST.", cx, Y(505f), R(10.5f), ivory, Typeface.DEFAULT, .28f)
        p.color = gold
        p.alpha = 190
        p.strokeWidth = R(.7f)
        c.drawLine(X(220f), Y(528f), X(500f), Y(528f), p)
        drawDiamond(c, cx, Y(528f), R(4f), brightGold)

        // Eight rectangular cards: this is intentionally NOT the old circular/orbital hub.
        cards.clear()
        val left = 20f
        val top = 552f
        val cw = 164f
        val ch = 132f
        val gap = 12f
        for (i in nodes.indices) {
            val col = i % 4
            val row = i / 4
            val rect = RectF(X(left + col * (cw + gap)), Y(top + row * (ch + gap)), X(left + col * (cw + gap) + cw), Y(top + row * (ch + gap) + ch))
            cards.add(rect to nodes[i].key)
            drawCard(c, rect, nodes[i], R)
        }

        val status = RectF(X(20f), Y(840f), X(700f), Y(928f))
        p.style = Paint.Style.FILL
        p.color = Color.rgb(5, 6, 8)
        p.alpha = 245
        c.drawRoundRect(status, R(9f), R(9f), p)
        p.style = Paint.Style.STROKE
        p.color = gold
        p.alpha = 180
        p.strokeWidth = R(.85f)
        c.drawRoundRect(status, R(9f), R(9f), p)
        drawMiniEye(c, X(72f), Y(884f), R(25f), Color.rgb(108, 236, 89))
        textLeft(c, "ORACLE READY", X(112f), Y(878f), R(14f), ivory, Typeface.DEFAULT_BOLD, 0f)
        textLeft(c, "Market Intelligence Active", X(112f), Y(901f), R(9f), Color.rgb(71, 218, 106), Typeface.DEFAULT, .01f)
        p.color = goldBright
        p.alpha = 180
        p.strokeWidth = R(.9f)
        c.drawCircle(X(378f), Y(884f), R(24f), p)
        for (i in -2..2) c.drawLine(X(378f + i * 6f), Y(872f - kotlin.math.abs(i) * 2f), X(378f + i * 6f), Y(896f + kotlin.math.abs(i) * 2f), p)
        textLeft(c, "LOCAL INTELLIGENCE", X(416f), Y(878f), R(13f), ivory, Typeface.DEFAULT_BOLD, 0f)
        textLeft(c, "Synced & Protected", X(416f), Y(901f), R(9f), Color.rgb(132, 201, 126), Typeface.DEFAULT, .01f)
        p.style = Paint.Style.FILL
        p.color = Color.rgb(66, 228, 95)
        p.alpha = 255
        c.drawCircle(X(675f), Y(886f), R(8f), p)

        text(c, "ORACLE", cx, Y(965f), R(20f), gold, Typeface.SERIF, .34f)
        text(c, "SEE MORE.  KNOW FIRST.", cx, Y(991f), R(8.5f), dim, Typeface.DEFAULT, .22f)
        p.style = Paint.Style.STROKE
        p.color = gold
        p.alpha = 150
        p.strokeWidth = R(.65f)
        c.drawLine(X(285f), Y(1012f), X(435f), Y(1012f), p)
        drawDiamond(c, cx, Y(1012f), R(3f), gold)
        text(c, "357AT2026", cx, Y(1058f), R(11f), brightGold, Typeface.DEFAULT_BOLD, .16f)
    }

    private fun drawTopButton(c: Canvas, cx: Float, cy: Float, r: Float, gear: Boolean) {
        p.style = Paint.Style.STROKE
        p.color = gold
        p.alpha = 220
        p.strokeWidth = r * .035f
        c.drawRoundRect(RectF(cx-r, cy-r, cx+r, cy+r), r*.20f, r*.20f, p)
        p.strokeWidth = r * .075f
        if (!gear) {
            for (i in -1..1) c.drawLine(cx-r*.35f, cy+i*r*.22f, cx+r*.35f, cy+i*r*.22f, p)
        } else {
            c.drawCircle(cx, cy, r*.25f, p)
            c.drawCircle(cx, cy, r*.42f, p)
            for (i in 0 until 8) {
                val a = i*Math.PI/4.0
                c.drawLine(cx+cos(a).toFloat()*r*.45f, cy+sin(a).toFloat()*r*.45f, cx+cos(a).toFloat()*r*.58f, cy+sin(a).toFloat()*r*.58f, p)
            }
        }
    }

    private fun drawOracleEye(c: Canvas, cx: Float, cy: Float, r: Float, color: Int) {
        p.style = Paint.Style.STROKE
        p.color = color
        p.alpha = 240
        p.strokeWidth = r*.055f
        path.reset()
        path.moveTo(cx-r*.75f, cy)
        path.lineTo(cx, cy-r*.50f)
        path.lineTo(cx+r*.75f, cy)
        path.lineTo(cx, cy+r*.50f)
        path.close()
        c.drawPath(path,p)
        c.drawCircle(cx,cy,r*.18f,p)
        p.style=Paint.Style.FILL
        c.drawCircle(cx,cy,r*.055f,p)
        p.style=Paint.Style.STROKE
        p.strokeWidth=r*.025f
        c.drawLine(cx,cy-r*.78f,cx,cy-r*.50f,p)
        c.drawCircle(cx,cy-r*.86f,r*.045f,p)
    }

    private fun drawEye(c: Canvas, cx: Float, cy: Float, r: Float, color: Int) {
        p.style = Paint.Style.STROKE
        p.color = color
        p.alpha = 235
        p.strokeWidth = r*.018f
        path.reset()
        path.moveTo(cx-r,cy)
        path.cubicTo(cx-r*.58f,cy-r*.56f,cx+r*.58f,cy-r*.56f,cx+r,cy)
        path.cubicTo(cx+r*.58f,cy+r*.56f,cx-r*.58f,cy+r*.56f,cx-r,cy)
        c.drawPath(path,p)
        p.strokeWidth=r*.010f
        path.reset()
        path.moveTo(cx-r*.78f,cy)
        path.cubicTo(cx-r*.42f,cy-r*.34f,cx+r*.42f,cy-r*.34f,cx+r*.78f,cy)
        path.cubicTo(cx+r*.42f,cy+r*.34f,cx-r*.42f,cy+r*.34f,cx-r*.78f,cy)
        c.drawPath(path,p)
    }

    private fun drawCard(c: Canvas, rect: RectF, node: Node, R: (Float)->Float) {
        p.style=Paint.Style.FILL
        p.color=Color.rgb(3,4,6)
        p.alpha=246
        c.drawRoundRect(rect,R(8f),R(8f),p)
        p.style=Paint.Style.STROKE
        p.color=gold
        p.alpha=205
        p.strokeWidth=R(.85f)
        c.drawRoundRect(rect,R(8f),R(8f),p)
        p.alpha=75
        p.strokeWidth=R(.5f)
        c.drawLine(rect.left+R(9f),rect.top+R(25f),rect.left+R(9f),rect.top+R(9f),p)
        c.drawLine(rect.left+R(9f),rect.top+R(9f),rect.left+R(25f),rect.top+R(9f),p)
        c.drawLine(rect.right-R(9f),rect.bottom-R(25f),rect.right-R(9f),rect.bottom-R(9f),p)
        c.drawLine(rect.right-R(9f),rect.bottom-R(9f),rect.right-R(25f),rect.bottom-R(9f),p)
        val ix=rect.centerX(); val iy=rect.top+rect.height()*.37f; val ir=min(rect.width(),rect.height())*.25f
        p.style=Paint.Style.STROKE; p.color=node.color; p.alpha=245; p.strokeWidth=R(1.0f)
        for(k in 0..2)c.drawCircle(ix,iy,ir*(1f-k*.22f),p)
        drawIcon(c,ix,iy,ir,node.key,node.color,R)
        text(c,node.title,ix,rect.top+rect.height()*.71f,R(11.5f),ivory,Typeface.DEFAULT,.01f)
        text(c,node.sub,ix,rect.top+rect.height()*.84f,R(7.5f),dim,Typeface.DEFAULT,.035f)
        p.color=node.color;p.alpha=220;p.strokeWidth=R(.65f)
        c.drawLine(ix-R(20f),rect.bottom-R(11f),ix+R(20f),rect.bottom-R(11f),p)
        drawDiamond(c,ix,rect.bottom-R(11f),R(2.4f),node.color)
    }

    private fun drawIcon(c: Canvas,cx:Float,cy:Float,r:Float,key:String,color:Int,R:(Float)->Float){
        p.style=Paint.Style.STROKE;p.color=color;p.alpha=245;p.strokeWidth=R(1.8f)
        when(key){
            "portfolio"->{c.drawRect(cx-r*.45f,cy-r*.38f,cx+r*.45f,cy+r*.38f,p);c.drawCircle(cx+r*.18f,cy+r*.17f,r*.18f,p)}
            "watchlist"->drawEye(c,cx,cy,r*.78f,color)
            "analysis"->{path.reset();path.moveTo(cx-r*.60f,cy+r*.35f);path.lineTo(cx-r*.20f,cy);path.lineTo(cx+r*.05f,cy+r*.16f);path.lineTo(cx+r*.58f,cy-r*.48f);c.drawPath(path,p)}
            "growth"->{path.reset();path.moveTo(cx-r*.58f,cy+r*.40f);path.lineTo(cx-r*.18f,cy+r*.08f);path.lineTo(cx+r*.08f,cy+r*.20f);path.lineTo(cx+r*.58f,cy-r*.48f);c.drawPath(path,p)}
            "alerts"->{c.drawArc(RectF(cx-r*.44f,cy-r*.42f,cx+r*.44f,cy+r*.45f),210f,120f,false,p);c.drawLine(cx-r*.58f,cy+r*.48f,cx+r*.58f,cy+r*.48f,p);c.drawCircle(cx,cy+r*.58f,r*.07f,p)}
            "news"->{c.drawRect(cx-r*.46f,cy-r*.43f,cx+r*.46f,cy+r*.43f,p);for(i in -1..1)c.drawLine(cx-r*.27f,cy+i*r*.18f,cx+r*.27f,cy+i*r*.18f,p)}
            "knowledge"->{c.drawRect(cx-r*.48f,cy-r*.44f,cx,cy+r*.43f,p);c.drawRect(cx,cy-r*.44f,cx+r*.48f,cy+r*.43f,p)}
            else->drawOracleEye(c,cx,cy,r*.85f,color)
        }
    }

    private fun drawMiniEye(c:Canvas,cx:Float,cy:Float,r:Float,color:Int){
        p.style=Paint.Style.STROKE;p.color=color;p.alpha=245;p.strokeWidth=r*.075f
        path.reset();path.moveTo(cx-r,cy);path.cubicTo(cx-r*.55f,cy-r*.52f,cx+r*.55f,cy-r*.52f,cx+r,cy);path.cubicTo(cx+r*.55f,cy+r*.52f,cx-r*.55f,cy+r*.52f,cx-r,cy);c.drawPath(path,p);c.drawCircle(cx,cy,r*.25f,p)
    }

    private fun drawDiamond(c:Canvas,cx:Float,cy:Float,r:Float,color:Int){
        p.style=Paint.Style.STROKE;p.color=color;p.alpha=220;p.strokeWidth=Rlocal(.7f)
        path.reset();path.moveTo(cx,cy-r);path.lineTo(cx+r,cy);path.lineTo(cx,cy+r);path.lineTo(cx-r,cy);path.close();c.drawPath(path,p)
    }
    private fun Rlocal(v:Float)=v*resources.displayMetrics.density

    private fun text(c:Canvas,s:String,cx:Float,baseline:Float,size:Float,color:Int,typeface:Typeface,spacing:Float){
        p.style=Paint.Style.FILL;p.shader=null;p.alpha=255;p.color=color;p.typeface=typeface;p.textSize=size;p.textAlign=Paint.Align.CENTER;p.letterSpacing=spacing;c.drawText(s,cx,baseline,p)
    }
    private fun textLeft(c:Canvas,s:String,x:Float,baseline:Float,size:Float,color:Int,typeface:Typeface,spacing:Float){
        p.style=Paint.Style.FILL;p.shader=null;p.alpha=255;p.color=color;p.typeface=typeface;p.textSize=size;p.textAlign=Paint.Align.LEFT;p.letterSpacing=spacing;c.drawText(s,x,baseline,p)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if(event.action==MotionEvent.ACTION_UP){
            for((rect,key) in cards){if(rect.contains(event.x,event.y)){if(key!="stock")onModule(key);return true}}
        }
        return true
    }
}
