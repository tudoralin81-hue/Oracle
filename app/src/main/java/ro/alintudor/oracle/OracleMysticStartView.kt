package ro.alintudor.oracle

import android.content.Context
import android.graphics.*
import android.view.MotionEvent
import android.view.View
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

/** B514 Start: native Canvas UI, no image asset. */
class OracleMysticStartView(context: Context, private val onModule: (String) -> Unit) : View(context) {
    private val p = Paint(Paint.ANTI_ALIAS_FLAG)
    private val path = Path()
    private val hits = ArrayList<Pair<RectF, String>>()
    private val gold = Color.rgb(238,190,60)
    private val bright = Color.rgb(255,215,82)
    private val white = Color.rgb(244,239,226)
    private data class Node(val key:String,val title:String,val sub:String,val color:Int)
    private val nodes = listOf(
        Node("portfolio","PORTFOLIO","OVERVIEW",Color.rgb(205,65,255)),
        Node("watchlist","WATCHLIST","TRACK & FOCUS",Color.rgb(250,202,55)),
        Node("analysis","ANALYSIS","CHARTS & TOOLS",Color.rgb(35,215,255)),
        Node("growth","GROWTH","FUTURE SCAN",Color.rgb(120,248,50)),
        Node("alerts","ALERTS","STAY AHEAD",Color.rgb(255,68,45)),
        Node("news","NEWS","MARKET PULSE",Color.rgb(38,212,255)),
        Node("knowledge","KNOWLEDGE","LEARN & EVOLVE",Color.rgb(248,202,55)),
        Node("stock","STOCK","INTELLIGENCE",Color.rgb(202,68,255))
    )
    override fun onDraw(c:Canvas){
        super.onDraw(c)
        val w=width.toFloat(); val h=height.toFloat(); val scale=min(w/720f,h/1180f).coerceAtLeast(0.42f); val ox=(w-720f*scale)/2f
        fun x(v:Float)=ox+v*scale
        fun y(v:Float)=v*scale
        fun rr(v:Float)=v*scale
        p.style=Paint.Style.FILL; p.alpha=255; p.color=Color.rgb(2,3,5); c.drawRect(0f,0f,w,h,p)
        val cx=x(360f); val cy=y(292f)
        p.style=Paint.Style.STROKE; p.color=gold; p.alpha=52; p.strokeWidth=rr(.55f)
        for(i in 0 until 11){ val rad=130f+i*19f; c.drawCircle(cx,cy,rr(rad),p) }
        for(i in 0 until 20){ val a=i*Math.PI/10.0; c.drawLine(cx+cos(a).toFloat()*rr(92f),cy+sin(a).toFloat()*rr(92f),cx+cos(a).toFloat()*rr(315f),cy+sin(a).toFloat()*rr(315f),p) }
        button(c,x(58f),y(54f),rr(29f),false); button(c,x(662f),y(54f),rr(29f),true)
        text(c,"STOCK INTELLIGENCE",cx,y(22f),rr(9f),gold,Typeface.DEFAULT_BOLD,.08f)
        sigil(c,cx,y(82f),rr(30f)); text(c,"ORACLE",cx,y(138f),rr(31f),bright,Typeface.SERIF,.17f); text(c,"STOCK INTELLIGENCE",cx,y(163f),rr(9.5f),gold,Typeface.DEFAULT,.25f)
        eye(c,cx,cy,rr(142f),bright)
        p.style=Paint.Style.FILL;p.color=Color.BLACK;p.alpha=255;c.drawCircle(cx,cy,rr(51f),p)
        p.style=Paint.Style.STROKE;p.color=bright;p.alpha=230;p.strokeWidth=rr(2f);c.drawCircle(cx,cy,rr(57f),p)
        p.strokeWidth=rr(.7f);p.alpha=120;for(i in 0 until 24){val a=i*Math.PI/12.0;c.drawLine(cx+cos(a).toFloat()*rr(65f),cy+sin(a).toFloat()*rr(65f),cx+cos(a).toFloat()*rr(145f),cy+sin(a).toFloat()*rr(145f),p)}
        text(c,"SEE MORE.  KNOW FIRST.",cx,y(500f),rr(10.5f),white,Typeface.DEFAULT,.27f);p.color=gold;p.alpha=190;p.strokeWidth=rr(.7f);c.drawLine(x(220f),y(522f),x(500f),y(522f),p);diamond(c,cx,y(522f),rr(4f),bright)
        hits.clear(); val left=20f; val top=546f; val cw=164f; val ch=132f; val gap=12f
        for(i in nodes.indices){val col=i%4;val row=i/4;val l=x(left+col*(cw+gap));val t=y(top+row*(ch+gap));val q=RectF(l,t,l+rr(cw),t+rr(ch));hits.add(q to nodes[i].key);drawCard(c,q,nodes[i],scale)}
        val st=RectF(x(20f),y(834f),x(700f),y(922f));p.style=Paint.Style.FILL;p.color=Color.rgb(5,6,8);p.alpha=245;c.drawRoundRect(st,rr(9f),rr(9f),p);p.style=Paint.Style.STROKE;p.color=gold;p.alpha=180;p.strokeWidth=rr(.8f);c.drawRoundRect(st,rr(9f),rr(9f),p)
        miniEye(c,x(72f),y(878f),rr(24f),Color.rgb(105,235,88));leftText(c,"ORACLE READY",x(112f),y(872f),rr(14f),white,Typeface.DEFAULT_BOLD);leftText(c,"Market Intelligence Active",x(112f),y(895f),rr(9f),Color.rgb(70,218,105),Typeface.DEFAULT)
        p.style=Paint.Style.STROKE;p.color=gold;p.alpha=190;p.strokeWidth=rr(.9f);c.drawCircle(x(378f),y(878f),rr(24f),p);for(i in -2..2)c.drawLine(x(378f+i*6f),y(866f-kotlin.math.abs(i)*2f),x(378f+i*6f),y(890f+kotlin.math.abs(i)*2f),p);leftText(c,"LOCAL INTELLIGENCE",x(416f),y(872f),rr(13f),white,Typeface.DEFAULT_BOLD);leftText(c,"Synced & Protected",x(416f),y(895f),rr(9f),Color.rgb(130,200,125),Typeface.DEFAULT);p.style=Paint.Style.FILL;p.color=Color.rgb(65,228,95);p.alpha=255;c.drawCircle(x(675f),y(880f),rr(8f),p)
        text(c,"ORACLE",cx,y(960f),rr(20f),gold,Typeface.SERIF,.34f);text(c,"SEE MORE.  KNOW FIRST.",cx,y(986f),rr(8f),Color.rgb(145,136,118),Typeface.DEFAULT,.22f);p.style=Paint.Style.STROKE;p.color=gold;p.alpha=150;p.strokeWidth=rr(.6f);c.drawLine(x(285f),y(1007f),x(435f),y(1007f),p);diamond(c,cx,y(1007f),rr(3f),gold);text(c,"357AT2026",cx,y(1050f),rr(11f),bright,Typeface.DEFAULT_BOLD,.16f)
    }
    private fun button(c:Canvas,x:Float,y:Float,r:Float,gear:Boolean){p.style=Paint.Style.STROKE;p.color=gold;p.alpha=220;p.strokeWidth=r*.035f;c.drawRoundRect(RectF(x-r,y-r,x+r,y+r),r*.2f,r*.2f,p);p.strokeWidth=r*.075f;if(!gear){for(i in -1..1)c.drawLine(x-r*.35f,y+i*r*.22f,x+r*.35f,y+i*r*.22f,p)}else{c.drawCircle(x,y,r*.25f,p);c.drawCircle(x,y,r*.42f,p);for(i in 0 until 8){val a=i*Math.PI/4;c.drawLine(x+cos(a).toFloat()*r*.45f,y+sin(a).toFloat()*r*.45f,x+cos(a).toFloat()*r*.58f,y+sin(a).toFloat()*r*.58f,p)}}}
    private fun sigil(c:Canvas,x:Float,y:Float,r:Float){p.style=Paint.Style.STROKE;p.color=bright;p.alpha=240;p.strokeWidth=r*.06f;path.reset();path.moveTo(x-r*.75f,y);path.lineTo(x,y-r*.5f);path.lineTo(x+r*.75f,y);path.lineTo(x,y+r*.5f);path.close();c.drawPath(path,p);c.drawCircle(x,y,r*.18f,p);p.style=Paint.Style.FILL;c.drawCircle(x,y,r*.055f,p)}
    private fun eye(c:Canvas,x:Float,y:Float,r:Float,col:Int){p.style=Paint.Style.STROKE;p.color=col;p.alpha=235;p.strokeWidth=r*.018f;path.reset();path.moveTo(x-r,y);path.cubicTo(x-r*.58f,y-r*.56f,x+r*.58f,y-r*.56f,x+r,y);path.cubicTo(x+r*.58f,y+r*.56f,x-r*.58f,y+r*.56f,x-r,y);c.drawPath(path,p)}
    private fun drawCard(c:Canvas,q:RectF,n:Node,s:Float){fun r(v:Float)=v*s;p.style=Paint.Style.FILL;p.color=Color.rgb(3,4,6);p.alpha=247;c.drawRoundRect(q,r(8f),r(8f),p);p.style=Paint.Style.STROKE;p.color=gold;p.alpha=205;p.strokeWidth=r(.8f);c.drawRoundRect(q,r(8f),r(8f),p);val x=q.centerX();val y=q.top+q.height()*.37f;val z=min(q.width(),q.height())*.25f;p.color=n.color;p.alpha=245;p.strokeWidth=r(1f);for(k in 0..2)c.drawCircle(x,y,z*(1-k*.22f),p);when(n.key){"watchlist"->eye(c,x,y,z*.78f,n.color);"portfolio"->c.drawRect(x-z*.45f,y-z*.38f,x+z*.45f,y+z*.38f,p);"analysis","growth"->{path.reset();path.moveTo(x-z*.6f,y+z*.35f);path.lineTo(x-z*.18f,y);path.lineTo(x+z*.05f,y+z*.16f);path.lineTo(x+z*.58f,y-z*.48f);c.drawPath(path,p)};"alerts"->c.drawArc(RectF(x-z*.45f,y-z*.43f,x+z*.45f,y+z*.43f),210f,120f,false,p);"news"->{c.drawRect(x-z*.46f,y-z*.43f,x+z*.46f,y+z*.43f,p);for(i in -1..1)c.drawLine(x-z*.27f,y+i*z*.18f,x+z*.27f,y+i*z*.18f,p)};"knowledge"->{c.drawRect(x-z*.48f,y-z*.43f,x,y+z*.43f,p);c.drawRect(x,y-z*.43f,x+z*.48f,y+z*.43f,p)};else->sigil(c,x,y,z*.75f)};text(c,n.title,x,q.top+q.height()*.71f,r(11.5f),white,Typeface.DEFAULT,.01f);text(c,n.sub,x,q.top+q.height()*.84f,r(7.5f),Color.rgb(150,142,125),Typeface.DEFAULT,.03f);p.color=n.color;p.alpha=220;p.strokeWidth=r(.6f);c.drawLine(x-r(20f),q.bottom-r(11f),x+r(20f),q.bottom-r(11f),p);diamond(c,x,q.bottom-r(11f),r(2.4f),n.color)}
    private fun miniEye(c:Canvas,x:Float,y:Float,r:Float,col:Int){eye(c,x,y,r,col);p.style=Paint.Style.STROKE;p.color=col;p.strokeWidth=r*.07f;c.drawCircle(x,y,r*.24f,p)}
    private fun diamond(c:Canvas,x:Float,y:Float,r:Float,col:Int){p.style=Paint.Style.STROKE;p.color=col;p.alpha=220;p.strokeWidth=.7f*resources.displayMetrics.density;path.reset();path.moveTo(x,y-r);path.lineTo(x+r,y);path.lineTo(x,y+r);path.lineTo(x-r,y);path.close();c.drawPath(path,p)}
    private fun text(c:Canvas,s:String,x:Float,y:Float,size:Float,col:Int,t:Typeface,sp:Float){p.style=Paint.Style.FILL;p.alpha=255;p.color=col;p.textSize=size;p.typeface=t;p.textAlign=Paint.Align.CENTER;p.letterSpacing=sp;c.drawText(s,x,y,p)}
    private fun leftText(c:Canvas,s:String,x:Float,y:Float,size:Float,col:Int,t:Typeface){p.style=Paint.Style.FILL;p.alpha=255;p.color=col;p.textSize=size;p.typeface=t;p.textAlign=Paint.Align.LEFT;p.letterSpacing=0f;c.drawText(s,x,y,p)}
    override fun onTouchEvent(e:MotionEvent):Boolean{if(e.action==MotionEvent.ACTION_UP){for((r,k) in hits){if(r.contains(e.x,e.y)&&k!="stock"){onModule(k);return true}}};return true}
}
