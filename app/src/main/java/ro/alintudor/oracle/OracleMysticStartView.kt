package ro.alintudor.oracle

import android.content.Context
import android.graphics.*
import android.view.MotionEvent
import android.view.View
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

/** Native B514 Start. No image/bitmap. Rectangular card composition inspired by the approved reference. */
class OracleMysticStartView(context: Context, private val onModule: (String) -> Unit) : View(context) {
    private val p=Paint(Paint.ANTI_ALIAS_FLAG); private val path=Path(); private val hit=ArrayList<Pair<RectF,String>>()
    private val gold=Color.rgb(238,190,60); private val white=Color.rgb(244,239,226)
    private data class N(val key:String,val title:String,val sub:String,val color:Int)
    private val ns=listOf(
        N("portfolio","PORTFOLIO","OVERVIEW",Color.rgb(205,65,255)),N("watchlist","WATCHLIST","TRACK & FOCUS",Color.rgb(250,202,55)),
        N("analysis","ANALYSIS","CHARTS & TOOLS",Color.rgb(35,215,255)),N("growth","GROWTH","FUTURE SCAN",Color.rgb(120,248,50)),
        N("alerts","ALERTS","STAY AHEAD",Color.rgb(255,68,45)),N("news","NEWS","MARKET PULSE",Color.rgb(38,212,255)),
        N("knowledge","KNOWLEDGE","LEARN & EVOLVE",Color.rgb(248,202,55)),N("stock","STOCK","INTELLIGENCE",Color.rgb(202,68,255)))
    override fun onDraw(c:Canvas){
        val w=width.toFloat(); val h=height.toFloat(); val s=min(w/720f,h/1180f).coerceAtLeast(.42f); val ox=(w-720*s)/2f
        fun X(v:Float)=ox+v*s; fun Y(v:Float)=v*s; fun R(v:Float)=v*s
        p.style=Paint.Style.FILL;p.color=Color.rgb(2,3,5);p.alpha=255;c.drawRect(0f,0f,w,h,p)
        val cx=X(360f); val cy=Y(292f)
        p.style=Paint.Style.STROKE;p.color=gold;p.alpha=55;p.strokeWidth=R(.55f)
        for(r in 130..315 step 18)c.drawCircle(cx,cy,R(r.toFloat()),p)
        for(i in 0 until 20){val a=i*Math.PI/10;c.drawLine(cx+cos(a).toFloat()*R(95f),cy+sin(a).toFloat()*R(95f),cx+cos(a).toFloat()*R(315f),cy+sin(a).toFloat()*R(315f),p)}
        button(c,X(58f),Y(54f),R(29f),false);button(c,X(662f),Y(54f),R(29f),true)
        text(c,"STOCK INTELLIGENCE",cx,Y(22f),R(9f),gold,Typeface.DEFAULT_BOLD,.08f)
        sigil(c,cx,Y(82f),R(30f));text(c,"ORACLE",cx,Y(138f),R(31f),Color.rgb(255,215,82),Typeface.SERIF,.17f);text(c,"STOCK INTELLIGENCE",cx,Y(163f),R(9.5f),gold,Typeface.DEFAULT,.25f)
        eye(c,cx,cy,R(142f),Color.rgb(255,215,82));p.style=Paint.Style.FILL;p.color=Color.BLACK;c.drawCircle(cx,cy,R(51f),p);p.style=Paint.Style.STROKE;p.color=Color.rgb(255,215,82);p.alpha=230;p.strokeWidth=R(2f);c.drawCircle(cx,cy,R(57f),p)
        p.strokeWidth=R(.7f);p.alpha=120;for(i in 0 until 24){val a=i*Math.PI/12;c.drawLine(cx+cos(a).toFloat()*R(65f),cy+sin(a).toFloat()*R(65f),cx+cos(a).toFloat()*R(145f),cy+sin(a).toFloat()*R(145f),p)}
        text(c,"SEE MORE.  KNOW FIRST.",cx,Y(500f),R(10.5f),white,Typeface.DEFAULT,.27f);p.color=gold;p.alpha=190;p.strokeWidth=R(.7f);c.drawLine(X(220f),Y(522f),X(500f),Y(522f),p);diamond(c,cx,Y(522f),R(4f),Color.rgb(255,215,82))
        hit.clear();val left=20f;val top=546f;val cw=164f;val ch=132f;val gap=12f
        ns.forEachIndexed{ i,n->val col=i%4;val row=i/4;val l=X(left+col*(cw+gap));val t=Y(top+row*(ch+gap));val q=RectF(l,t,l+R(cw),t+R(ch));hit.add(q to n.key);card(c,q,n,R)}
        val st=RectF(X(20f),Y(834f),X(700f),Y(922f));p.style=Paint.Style.FILL;p.color=Color.rgb(5,6,8);p.alpha=245;c.drawRoundRect(st,R(9f),R(9f),p);p.style=Paint.Style.STROKE;p.color=gold;p.alpha=180;p.strokeWidth=R(.8f);c.drawRoundRect(st,R(9f),R(9f),p)
        miniEye(c,X(72f),Y(878f),R(24f),Color.rgb(105,235,88));leftText(c,"ORACLE READY",X(112f),Y(872f),R(14f),white,Typeface.DEFAULT_BOLD);leftText(c,"Market Intelligence Active",X(112f),Y(895f),R(9f),Color.rgb(70,218,105),Typeface.DEFAULT)
        p.style=Paint.Style.STROKE;p.color=gold;p.alpha=190;p.strokeWidth=R(.9f);c.drawCircle(X(378f),Y(878f),R(24f),p);for(i in -2..2)c.drawLine(X(378f+i*6f),Y(866f-kotlin.math.abs(i)*2f),X(378f+i*6f),Y(890f+kotlin.math.abs(i)*2f),p);leftText(c,"LOCAL INTELLIGENCE",X(416f),Y(872f),R(13f),white,Typeface.DEFAULT_BOLD);leftText(c,"Synced & Protected",X(416f),Y(895f),R(9f),Color.rgb(130,200,125),Typeface.DEFAULT);p.style=Paint.Style.FILL;p.color=Color.rgb(65,228,95);p.alpha=255;c.drawCircle(X(675f),Y(880f),R(8f),p)
        text(c,"ORACLE",cx,Y(960f),R(20f),gold,Typeface.SERIF,.34f);text(c,"SEE MORE.  KNOW FIRST.",cx,Y(986f),R(8f),Color.rgb(145,136,118),Typeface.DEFAULT,.22f);p.style=Paint.Style.STROKE;p.color=gold;p.alpha=150;p.strokeWidth=R(.6f);c.drawLine(X(285f),Y(1007f),X(435f),Y(1007f),p);diamond(c,cx,Y(1007f),R(3f),gold);text(c,"357AT2026",cx,Y(1050f),R(11f),Color.rgb(255,215,82),Typeface.DEFAULT_BOLD,.16f)
    }
    private fun button(c:Canvas,x:Float,y:Float,r:Float,g:Boolean){p.style=Paint.Style.STROKE;p.color=gold;p.alpha=220;p.strokeWidth=r*.035f;c.drawRoundRect(RectF(x-r,y-r,x+r,y+r),r*.2f,r*.2f,p);p.strokeWidth=r*.075f;if(!g)for(i in -1..1)c.drawLine(x-r*.35f,y+i*r*.22f,x+r*.35f,y+i*r*.22f,p)else{c.drawCircle(x,y,r*.25f,p);c.drawCircle(x,y,r*.42f,p);for(i in 0 until 8){val a=i*Math.PI/4;c.drawLine(x+cos(a).toFloat()*r*.45f,y+sin(a).toFloat()*r*.45f,x+cos(a).toFloat()*r*.58f,y+sin(a).toFloat()*r*.58f,p)}}}
    private fun sigil(c:Canvas,x:Float,y:Float,r:Float){p.style=Paint.Style.STROKE;p.color=Color.rgb(255,215,82);p.alpha=240;p.strokeWidth=r*.06f;path.reset();path.moveTo(x-r*.75f,y);path.lineTo(x,y-r*.5f);path.lineTo(x+r*.75f,y);path.lineTo(x,y+r*.5f);path.close();c.drawPath(path,p);c.drawCircle(x,y,r*.18f,p);p.style=Paint.Style.FILL;c.drawCircle(x,y,r*.055f,p)}
    private fun eye(c:Canvas,x:Float,y:Float,r:Float,col:Int){p.style=Paint.Style.STROKE;p.color=col;p.alpha=235;p.strokeWidth=r*.018f;path.reset();path.moveTo(x-r,y);path.cubicTo(x-r*.58f,y-r*.56f,x+r*.58f,y-r*.56f,x+r,y);path.cubicTo(x+r*.58f,y+r*.56f,x-r*.58f,y+r*.56f,x-r,y);c.drawPath(path,p)}
    private fun card(c:Canvas,q:RectF,n:N,R:(Float)->Float){p.style=Paint.Style.FILL;p.color=Color.rgb(3,4,6);p.alpha=247;c.drawRoundRect(q,R(8f),R(8f),p);p.style=Paint.Style.STROKE;p.color=gold;p.alpha=205;p.strokeWidth=R(.8f);c.drawRoundRect(q,R(8f),R(8f),p);val x=q.centerX();val y=q.top+q.height()*.37f;val r=min(q.width(),q.height())*.25f;p.color=n.color;p.alpha=245;p.strokeWidth=R(1f);for(k in 0..2)c.drawCircle(x,y,r*(1-k*.22f),p);when(n.key){"watchlist"->eye(c,x,y,r*.78f,n.color);"portfolio"->c.drawRect(x-r*.45f,y-r*.35f,x+r*.45f,y+r*.38f,p);"analysis","growth"->{path.reset();path.moveTo(x-r*.6f,y+r*.35f);path.lineTo(x-r*.15f,y);path.lineTo(x+r*.05f,y+r*.15f);path.lineTo(x+r*.58f,y-r*.48f);c.drawPath(path,p)}"alerts"->c.drawArc(RectF(x-r*.45f,y-r*.43f,x+r*.45f,y+r*.43f),210f,120f,false,p);"news"->{c.drawRect(x-r*.46f,y-r*.43f,x+r*.46f,y+r*.43f,p);for(i in -1..1)c.drawLine(x-r*.27f,y+i*r*.18f,x+r*.27f,y+i*r*.18f,p)}"knowledge"->{c.drawRect(x-r*.48f,y-r*.43f,x,y+r*.43f,p);c.drawRect(x,y-r*.43f,x+r*.48f,y+r*.43f,p)}else->sigil(c,x,y,r*.75f)};text(c,n.title,x,q.top+q.height()*.71f,R(11.5f),white,Typeface.DEFAULT,.01f);text(c,n.sub,x,q.top+q.height()*.84f,R(7.4f),Color.rgb(150,142,125),Typeface.DEFAULT,.03f);p.color=n.color;p.alpha=220;p.strokeWidth=R(.6f);c.drawLine(x-R(20f),q.bottom-R(11f),x+R(20f),q.bottom-R(11f),p);diamond(c,x,q.bottom-R(11f),R(2.4f),n.color)}
    private fun miniEye(c:Canvas,x:Float,y:Float,r:Float,col:Int){eye(c,x,y,r,col);p.style=Paint.Style.STROKE;p.color=col;p.strokeWidth=r*.07f;c.drawCircle(x,y,r*.24f,p)}
    private fun diamond(c:Canvas,x:Float,y:Float,r:Float,col:Int){p.style=Paint.Style.STROKE;p.color=col;p.alpha=220;p.strokeWidth=.7f*resources.displayMetrics.density;path.reset();path.moveTo(x,y-r);path.lineTo(x+r,y);path.lineTo(x,y+r);path.lineTo(x-r,y);path.close();c.drawPath(path,p)}
    private fun text(c:Canvas,s:String,x:Float,y:Float,size:Float,col:Int,t:Typeface,sp:Float){p.style=Paint.Style.FILL;p.alpha=255;p.color=col;p.textSize=size;p.typeface=t;p.textAlign=Paint.Align.CENTER;p.letterSpacing=sp;c.drawText(s,x,y,p)}
    private fun leftText(c:Canvas,s:String,x:Float,y:Float,size:Float,col:Int,t:Typeface){p.style=Paint.Style.FILL;p.alpha=255;p.color=col;p.textSize=size;p.typeface=t;p.textAlign=Paint.Align.LEFT;p.letterSpacing=0f;c.drawText(s,x,y,p)}
    override fun onTouchEvent(e:MotionEvent):Boolean{if(e.action==MotionEvent.ACTION_UP)for((r,k)in hit)if(r.contains(e.x,e.y)&&k!="stock"){onModule(k);return true};return true}
}
