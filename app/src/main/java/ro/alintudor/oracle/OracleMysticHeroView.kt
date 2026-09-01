package ro.alintudor.oracle

import android.content.Context
import android.graphics.*
import android.view.MotionEvent
import android.view.View
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

/** Native/vector Start hero. No bitmap/image is used. */
class OracleMysticHeroView(context: Context, private val onModule: (String) -> Unit) : View(context) {
    private val p = Paint(Paint.ANTI_ALIAS_FLAG)
    private val path = Path()
    private val gold = Color.rgb(255, 194, 38)
    private val goldBright = Color.rgb(255, 225, 105)
    private val white = Color.rgb(248, 245, 235)
    private val bg = Color.rgb(1, 3, 7)
    private val nodes = listOf(
        Node("portfolio", "PORTFOLIO", Color.rgb(211, 76, 255), .17f, .82f, "OVERVIEW"),
        Node("watchlist", "WATCHLIST", goldBright, .39f, .82f, "TRACK & FOCUS"),
        Node("analysis", "ANALYSIS", Color.rgb(22, 218, 255), .61f, .82f, "CHARTS & TOOLS"),
        Node("growth", "GROWTH", Color.rgb(118, 255, 40), .83f, .82f, "FUTURE SCAN"),
        Node("alerts", "ALERTS", Color.rgb(255, 65, 40), .17f, .985f, "STAY AHEAD"),
        Node("news", "NEWS", Color.rgb(20, 218, 255), .39f, .985f, "MARKET PULSE"),
        Node("knowledge", "KNOWLEDGE", goldBright, .61f, .985f, "LEARN & EVOLVE"),
        Node("stock", "STOCK", Color.rgb(211, 76, 255), .83f, .985f, "INTELLIGENCE")
    )
    private data class Node(val key:String,val label:String,val color:Int,val x:Float,val y:Float,val sub:String)
    private var hit = ArrayList<RectF>()

    override fun onDraw(c: Canvas) {
        val w=width.toFloat(); val h=height.toFloat(); val base=min(w,h); val cx=w*.5f
        p.style=Paint.Style.FILL; p.shader=LinearGradient(0f,0f,w,h,Color.rgb(1,2,5),Color.rgb(9,6,4),Shader.TileMode.CLAMP); c.drawRect(0f,0f,w,h,p); p.shader=null
        // orbital rings and fine geometry
        val cy=h*.34f; val r=base*.31f
        p.style=Paint.Style.STROKE; p.strokeWidth=dp(.75f); p.color=gold; p.alpha=42
        for(i in 1..9)c.drawCircle(cx,cy,r*i/9f,p)
        for(i in 0 until 18){val a=i*Math.PI/9; c.drawLine(cx+cos(a)*r*.05,cy+sin(a)*r*.05,cx+cos(a)*r,cy+sin(a)*r,p)}
        p.alpha=105; p.strokeWidth=dp(1f); c.drawCircle(cx,cy,r*.68f,p); c.drawCircle(cx,cy,r*.53f,p)
        // top oracle eye glyph
        drawEye(c,cx,dp(67f).toFloat(),dp(30f).toFloat(),goldBright)
        p.style=Paint.Style.FILL; p.alpha=255; p.textAlign=Paint.Align.CENTER
        p.typeface=Typeface.create(Typeface.SERIF,Typeface.NORMAL); p.textSize=dp(28f); p.color=goldBright; c.drawText("ORACLE",cx,dp(112f).toFloat(),p)
        p.typeface=Typeface.DEFAULT_BOLD; p.textSize=dp(8f); p.letterSpacing=.25f; c.drawText("STOCK INTELLIGENCE",cx,dp(129f).toFloat(),p); p.letterSpacing=0f
        // central mystical eye
        val ey=cy+dp(28f); drawEye(c,cx,ey,r*.72f,goldBright)
        // iris / pupil / rays
        p.style=Paint.Style.STROKE; p.strokeWidth=dp(1.5f); p.color=goldBright; p.alpha=230
        c.drawCircle(cx,ey,r*.13f,p); p.style=Paint.Style.FILL; c.drawCircle(cx,ey,r*.055f,p)
        p.style=Paint.Style.STROKE; p.strokeWidth=dp(.7f); p.alpha=95
        for(i in 0 until 32){val a=i*Math.PI/16; c.drawLine(cx+cos(a)*r*.16,ey+sin(a)*r*.16,cx+cos(a)*r*.30,ey+sin(a)*r*.30,p)}
        // vertical axis and tiny diamond
        p.color=gold; p.alpha=150; p.strokeWidth=dp(.7f); c.drawLine(cx,dp(15f).toFloat(),cx,h*.78f,p)
        drawDiamond(c,cx,dp(145f).toFloat(),dp(5f).toFloat(),goldBright)
        drawDiamond(c,cx,h*.75f,dp(5f).toFloat(),goldBright)
        // tagline inside hero
        p.style=Paint.Style.FILL; p.alpha=255; p.color=white; p.textSize=dp(9f); p.typeface=Typeface.DEFAULT_BOLD; p.letterSpacing=.28f
        c.drawText("SEE MORE.  KNOW FIRST.",cx,h*.70f,p); p.letterSpacing=0f
        p.style=Paint.Style.STROKE; p.strokeWidth=dp(.8f); p.color=gold; p.alpha=200; c.drawLine(cx-dp(105f),h*.735f,cx+dp(105f),h*.735f,p)
        // card grid
        hit.clear(); for(n in nodes){ val x=w*n.x; val y=h*n.y; val cw=w*.195f; val ch=h*.105f; val rect=RectF(x-cw/2,y-ch/2,x+cw/2,y+ch/2); hit.add(rect); drawCard(c,rect,n) }
    }

    private fun drawCard(c:Canvas,rect:RectF,n:Node){
        val rr=dp(8f); p.style=Paint.Style.FILL; p.color=Color.argb(225,2,5,10); p.alpha=225; c.drawRoundRect(rect,rr,rr,p)
        p.style=Paint.Style.STROKE; p.strokeWidth=dp(.9f); p.color=gold; p.alpha=220; c.drawRoundRect(rect,rr,rr,p)
        val cx=rect.centerX(); val cy=rect.top+rect.height()*.43f; val r=min(rect.width(),rect.height())*.25f
        p.color=n.color; p.alpha=235; p.strokeWidth=dp(1f); c.drawCircle(cx,cy,r,p); c.drawCircle(cx,cy,r*.72f,p); c.drawCircle(cx,cy,r*.45f,p)
        drawIcon(c,cx,cy,r,n.key,n.color)
        p.style=Paint.Style.FILL; p.color=white; p.alpha=255; p.textAlign=Paint.Align.CENTER; p.typeface=Typeface.DEFAULT_BOLD; p.textSize=dp(8.2f); c.drawText(n.label,cx,rect.top+rect.height()*.72f,p)
        p.typeface=Typeface.DEFAULT; p.textSize=dp(6f); p.color=Color.rgb(195,185,163); c.drawText(n.sub,cx,rect.top+rect.height()*.86f,p)
        p.color=n.color; c.drawCircle(cx,rect.bottom-dp(8f),dp(1.7f),p)
    }

    private fun drawIcon(c:Canvas,cx:Float,cy:Float,r:Float,key:String,color:Int){
        p.style=Paint.Style.STROKE; p.strokeWidth=dp(1.8f); p.color=color; p.alpha=245
        when(key){
            "portfolio"->{c.drawRect(cx-r*.48f,cy-r*.38f,cx+r*.48f,cy+r*.38f,p);c.drawCircle(cx+r*.32f,cy+r*.25f,r*.18f,p)}
            "watchlist"->{drawEye(c,cx,cy,r*.8f,color)}
            "analysis","growth"->{path.reset();path.moveTo(cx-r*.65f,cy+r*.42f);path.lineTo(cx-r*.2f,cy);path.lineTo(cx+r*.08f,cy+r*.16f);path.lineTo(cx+r*.65f,cy-r*.48f);c.drawPath(path,p)}
            "alerts"->{c.drawCircle(cx,cy-r*.05f,r*.46f,p);c.drawLine(cx-r*.58f,cy+r*.5f,cx+r*.58f,cy+r*.5f,p)}
            "news"->{c.drawRect(cx-r*.48f,cy-r*.45f,cx+r*.48f,cy+r*.45f,p);for(i in -1..1)c.drawLine(cx-r*.28f,cy+i*r*.18f,cx+r*.28f,cy+i*r*.18f,p)}
            "knowledge"->{c.drawRect(cx-r*.5f,cy-r*.43f,cx,cy+r*.43f,p);c.drawRect(cx,cy-r*.43f,cx+r*.5f,cy+r*.43f,p)}
            else->{path.reset();path.moveTo(cx-r*.6f,cy);path.lineTo(cx-r*.2f,cy-r*.35f);path.lineTo(cx+r*.1f,cy+r*.25f);path.lineTo(cx+r*.6f,cy-r*.45f);c.drawPath(path,p)}
        }
    }
    private fun drawEye(c:Canvas,cx:Float,cy:Float,r:Float,color:Int){
        p.style=Paint.Style.STROKE;p.color=color;p.alpha=245;p.strokeWidth=dp(1.4f);path.reset();path.moveTo(cx-r,cy);path.cubicTo(cx-r*.52f,cy-r*.62f,cx+r*.52f,cy-r*.62f,cx+r,cy);path.cubicTo(cx+r*.52f,cy+r*.62f,cx-r*.52f,cy+r*.62f,cx-r,cy);c.drawPath(path,p);c.drawCircle(cx,cy,r*.22f,p);p.style=Paint.Style.FILL;c.drawCircle(cx,cy,r*.08f,p)
    }
    private fun drawDiamond(c:Canvas,cx:Float,cy:Float,r:Float,color:Int){p.style=Paint.Style.STROKE;p.color=color;p.alpha=220;p.strokeWidth=dp(.8f);path.reset();path.moveTo(cx,cy-r);path.lineTo(cx+r,cy);path.lineTo(cx,cy+r);path.lineTo(cx-r,cy);path.close();c.drawPath(path,p)}
    private fun dp(v:Float)=v*resources.displayMetrics.density

    override fun onTouchEvent(e:MotionEvent):Boolean{
        if(e.action==MotionEvent.ACTION_UP){for(i in hit.indices)if(hit[i].contains(e.x,e.y)){onModule(nodes[i].key);return true}}
        return true
    }
}
