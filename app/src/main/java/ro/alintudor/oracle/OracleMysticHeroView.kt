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
    private data class Node(val key:String,val label:String,val sub:String,val color:Int)
    private val nodes = listOf(
        Node("portfolio","PORTFOLIO","OVERVIEW",Color.rgb(205,70,255)),
        Node("watchlist","WATCHLIST","TRACK & FOCUS",goldBright),
        Node("analysis","ANALYSIS","CHARTS & TOOLS",Color.rgb(34,218,255)),
        Node("growth","GROWTH","FUTURE SCAN",Color.rgb(118,255,42)),
        Node("alerts","ALERTS","STAY AHEAD",Color.rgb(255,65,42)),
        Node("news","NEWS","MARKET PULSE",Color.rgb(38,220,255)),
        Node("knowledge","KNOWLEDGE","LEARN & EVOLVE",goldBright),
        Node("","STOCK","INTELLIGENCE",Color.rgb(205,70,255))
    )
    override fun onDraw(c:Canvas){
        super.onDraw(c)
        val w=width.toFloat();val h=height.toFloat();val s=min(w/720f,h/1150f).coerceAtLeast(.42f);val ox=(w-720f*s)/2f
        fun X(v:Float)=ox+v*s; fun Y(v:Float)=v*s
        p.style=Paint.Style.FILL;p.shader=LinearGradient(0f,0f,w,h,Color.rgb(1,2,4),Color.rgb(8,5,3),Shader.TileMode.CLAMP);c.drawRect(0f,0f,w,h,p);p.shader=null
        p.style=Paint.Style.STROKE;p.strokeWidth=R(.65f);p.color=gold;p.alpha=38
        val cx=X(360f);val cy=Y(360f);val rr=R(325f)
        for(i in 1..11)c.drawCircle(cx,cy,rr*i/11f,p)
        for(i in 0 until 24){val a=i*Math.PI/12.0;val ca=cos(a).toFloat();val sa=sin(a).toFloat();c.drawLine(cx+ca*rr*.18f,cy+sa*rr*.18f,cx+ca*rr,cy+sa*rr,p)}
        p.style=Paint.Style.FILL;p.alpha=70;p.color=goldBright
        for(i in 0 until 28){val a=(i*47%360)*Math.PI/180.0;val rad=rr*(.45f+(i%5)*.095f);c.drawCircle(cx+cos(a).toFloat()*rad,cy+sin(a).toFloat()*rad,R(if(i%3==0)1.5f else .75f),p)}
        drawButton(c,X(54f),Y(52f),R(58f),"menu");drawButton(c,X(666f),Y(52f),R(58f),"gear")
        drawSigil(c,cx,Y(126f),R(54f));text(c,"ORACLE",cx,Y(204f),R(32f),goldBright,Typeface.SERIF,0f);text(c,"STOCK INTELLIGENCE",cx,Y(232f),R(11f),goldBright,Typeface.DEFAULT_BOLD,.22f)
        drawEye(c,cx,Y(352f),R(208f),goldBright,3f);p.style=Paint.Style.FILL;p.color=Color.rgb(1,2,3);p.alpha=255;c.drawCircle(cx,Y(352f),R(48f),p);p.style=Paint.Style.STROKE;p.color=goldBright;p.alpha=180;p.strokeWidth=R(2f);c.drawCircle(cx,Y(352f),R(51f),p)
        p.strokeWidth=R(.8f);p.alpha=110;for(i in 0 until 20){val a=i*Math.PI/10.0;val ca=cos(a).toFloat();val sa=sin(a).toFloat();c.drawLine(cx+ca*R(58f),Y(352f)+sa*R(58f),cx+ca*R(190f),Y(352f)+sa*R(190f),p)}
        p.alpha=190;c.drawLine(cx,Y(42f),cx,Y(548f),p)
        text(c,"SEE MORE.  KNOW FIRST.",cx,Y(566f),R(11f),ivory,Typeface.DEFAULT,.32f);p.style=Paint.Style.STROKE;p.strokeWidth=R(.8f);p.color=gold;p.alpha=190;c.drawLine(X(220f),Y(590f),X(500f),Y(590f),p);drawDiamond(c,cx,Y(590f),R(5f),goldBright)
        cards.clear();val left=20f;val top=628f;val cw=164f;val ch=145f;val gap=12f
        for(i in nodes.indices){val col=i%4;val row=i/4;val l=X(left+col*(cw+gap));val t=Y(top+row*(ch+gap));val rect=RectF(l,t,l+R(cw),t+R(ch));if(nodes[i].key.isNotEmpty())cards.add(rect to nodes[i].key);drawModuleCard(c,rect,nodes[i])}
        val sy=Y(958f);val sr=RectF(X(20f),sy,X(700f),sy+R(92f));p.style=Paint.Style.FILL;p.color=Color.rgb(5,7,9);p.alpha=235;c.drawRoundRect(sr,R(10f),R(10f),p);p.style=Paint.Style.STROKE;p.color=gold;p.alpha=170;p.strokeWidth=R(.9f);c.drawRoundRect(sr,R(10f),R(10f),p)
        drawMiniEye(c,X(76f),Y(1004f),R(25f),Color.rgb(103,238,88));textLeft(c,"ORACLE READY",X(112f),Y(998f),R(15f),ivory,Typeface.DEFAULT_BOLD,0f);textLeft(c,"Market Intelligence Active",X(112f),Y(1021f),R(9.5f),Color.rgb(75,218,108),Typeface.DEFAULT,.02f)
        p.style=Paint.Style.STROKE;p.color=goldBright;p.alpha=190;p.strokeWidth=R(1f);c.drawCircle(X(378f),Y(1004f),R(25f),p);for(i in -2..2)c.drawLine(X(378f+i*6f),Y(992f-abs(i)*2f),X(378f+i*6f),Y(1016f+abs(i)*2f),p);textLeft(c,"LOCAL INTELLIGENCE",X(416f),Y(998f),R(14f),ivory,Typeface.DEFAULT_BOLD,0f);textLeft(c,"Synced & Protected",X(416f),Y(1021f),R(9.5f),Color.rgb(122,202,117),Typeface.DEFAULT,.02f);p.style=Paint.Style.FILL;p.color=Color.rgb(63,224,91);p.alpha=255;c.drawCircle(X(675f),Y(1006f),R(8f),p)
    }
    private fun drawButton(c:Canvas,cx:Float,cy:Float,r:Float,type:String){p.style=Paint.Style.STROKE;p.color=gold;p.alpha=215;p.strokeWidth=r*.018f;c.drawRoundRect(RectF(cx-r,cy-r,cx+r,cy+r),r*.18f,r*.18f,p);p.strokeWidth=r*.035f;if(type=="menu")for(i in -1..1)c.drawLine(cx-r*.34f,cy+i*r*.23f,cx+r*.34f,cy+i*r*.23f,p)else{c.drawCircle(cx,cy,r*.25f,p);c.drawCircle(cx,cy,r*.42f,p);for(i in 0 until 8){val a=i*Math.PI/4;c.drawLine(cx+cos(a).toFloat()*r*.43f,cy+sin(a).toFloat()*r*.43f,cx+cos(a).toFloat()*r*.54f,cy+sin(a).toFloat()*r*.54f,p)}}}
    private fun drawSigil(c:Canvas,cx:Float,cy:Float,r:Float){p.style=Paint.Style.STROKE;p.color=goldBright;p.alpha=240;p.strokeWidth=r*.035f;path.reset();path.moveTo(cx-r*.62f,cy);path.lineTo(cx,cy-r*.42f);path.lineTo(cx+r*.62f,cy);path.lineTo(cx,cy+r*.42f);path.close();c.drawPath(path,p);c.drawCircle(cx,cy,r*.17f,p);p.style=Paint.Style.FILL;c.drawCircle(cx,cy,r*.055f,p);p.style=Paint.Style.STROKE;p.strokeWidth=r*.018f;c.drawLine(cx,cy-r*.75f,cx,cy-r*.42f,p);c.drawCircle(cx,cy-r*.82f,r*.045f,p)}
    private fun drawModuleCard(c:Canvas,r:RectF,n:Node){p.style=Paint.Style.FILL;p.color=Color.rgb(3,5,7);p.alpha=242;c.drawRoundRect(r,R(8f),R(8f),p);p.style=Paint.Style.STROKE;p.color=gold;p.alpha=210;p.strokeWidth=R(.9f);c.drawRoundRect(r,R(8f),R(8f),p);p.alpha=90;p.strokeWidth=R(.55f);c.drawLine(r.left+R(8f),r.top+R(25f),r.left+R(8f),r.top+R(8f),p);c.drawLine(r.left+R(8f),r.top+R(8f),r.left+R(25f),r.top+R(8f),p);c.drawLine(r.right-R(8f),r.bottom-R(25f),r.right-R(8f),r.bottom-R(8f),p);c.drawLine(r.right-R(8f),r.bottom-R(8f),r.right-R(25f),r.bottom-R(8f),p);val ix=r.centerX();val iy=r.top+r.height()*.37f;val ir=min(r.width(),r.height())*.23f;p.style=Paint.Style.STROKE;p.color=n.color;p.alpha=245;p.strokeWidth=R(1.15f);for(k in 0..2)c.drawCircle(ix,iy,ir*(1f-k*.23f),p);drawIcon(c,ix,iy,ir,n.key.ifEmpty{"stock"},n.color);text(c,n.label,ix,r.top+r.height()*.72f,R(12f),ivory,Typeface.DEFAULT,.01f);text(c,n.sub,ix,r.top+r.height()*.85f,R(8.3f),muted,Typeface.DEFAULT,.04f);p.style=Paint.Style.STROKE;p.color=n.color;p.alpha=230;p.strokeWidth=R(.75f);c.drawLine(ix-R(22f),r.bottom-R(12f),ix+R(22f),r.bottom-R(12f),p);drawDiamond(c,ix,r.bottom-R(12f),R(2.6f),n.color)}
    private fun drawIcon(c:Canvas,cx:Float,cy:Float,r:Float,key:String,color:Int){p.style=Paint.Style.STROKE;p.color=color;p.alpha=245;p.strokeWidth=R(2f);when(key){"portfolio"->{c.drawRect(cx-r*.45f,cy-r*.35f,cx+r*.45f,cy+r*.4f,p);c.drawCircle(cx+r*.23f,cy+r*.18f,r*.18f,p)};"watchlist"->drawMiniEye(c,cx,cy,r*.78f,color);"analysis"->{path.reset();path.moveTo(cx-r*.6f,cy+r*.36f);path.lineTo(cx-r*.22f,cy);path.lineTo(cx+r*.03f,cy+r*.15f);path.lineTo(cx+r*.58f,cy-r*.5f);c.drawPath(path,p)};"growth"->{path.reset();path.moveTo(cx-r*.58f,cy+r*.42f);path.lineTo(cx-r*.18f,cy+r*.08f);path.lineTo(cx+r*.08f,cy+r*.2f);path.lineTo(cx+r*.58f,cy-r*.48f);c.drawPath(path,p)};"alerts"->{c.drawArc(RectF(cx-r*.43f,cy-r*.42f,cx+r*.43f,cy+r*.45f),210f,120f,false,p);c.drawLine(cx-r*.58f,cy+r*.48f,cx+r*.58f,cy+r*.48f,p);c.drawCircle(cx,cy+r*.53f,r*.07f,p)};"news"->{c.drawRect(cx-r*.46f,cy-r*.43f,cx+r*.46f,cy+r*.43f,p);for(i in -1..1)c.drawLine(cx-r*.28f,cy+i*r*.18f,cx+r*.27f,cy+i*r*.18f,p)};"knowledge"->{c.drawRect(cx-r*.5f,cy-r*.45f,cx,cy+r*.43f,p);c.drawRect(cx,cy-r*.45f,cx+r*.5f,cy+r*.43f,p)};else->drawSigil(c,cx,cy,r*.8f)}}
    private fun drawEye(c:Canvas,cx:Float,cy:Float,r:Float,color:Int,stroke:Float){p.style=Paint.Style.STROKE;p.color=color;p.alpha=245;p.strokeWidth=R(stroke);path.reset();path.moveTo(cx-r,cy);path.cubicTo(cx-r*.52f,cy-r*.55f,cx+r*.52f,cy-r*.55f,cx+r,cy);path.cubicTo(cx+r*.52f,cy+r*.55f,cx-r*.52f,cy+r*.55f,cx-r,cy);c.drawPath(path,p);c.drawCircle(cx,cy,r*.23f,p);p.style=Paint.Style.FILL;c.drawCircle(cx,cy,r*.08f,p)}
    private fun drawMiniEye(c:Canvas,cx:Float,cy:Float,r:Float,color:Int)=drawEye(c,cx,cy,r,color,1.25f)
    private fun drawDiamond(c:Canvas,cx:Float,cy:Float,r:Float,color:Int){p.style=Paint.Style.STROKE;p.color=color;p.alpha=225;p.strokeWidth=R(.75f);path.reset();path.moveTo(cx,cy-r);path.lineTo(cx+r,cy);path.lineTo(cx,cy+r);path.lineTo(cx-r,cy);path.close();c.drawPath(path,p)}
    private fun text(c:Canvas,s:String,cx:Float,y:Float,size:Float,color:Int,typeface:Typeface,spacing:Float){p.style=Paint.Style.FILL;p.color=color;p.alpha=255;p.textAlign=Paint.Align.CENTER;p.typeface=typeface;p.textSize=size;p.letterSpacing=spacing;c.drawText(s,cx,y,p);p.letterSpacing=0f}
    private fun textLeft(c:Canvas,s:String,x:Float,y:Float,size:Float,color:Int,typeface:Typeface,spacing:Float){p.style=Paint.Style.FILL;p.color=color;p.alpha=255;p.textAlign=Paint.Align.LEFT;p.typeface=typeface;p.textSize=size;p.letterSpacing=spacing;c.drawText(s,x,y,p);p.letterSpacing=0f}
    private fun R(v:Float)=v*(min(width/720f,height/1150f).coerceAtLeast(.42f))
    override fun onTouchEvent(e:MotionEvent):Boolean{if(e.action==MotionEvent.ACTION_UP){for((rect,key) in cards)if(rect.contains(e.x,e.y)){onModule(key);return true}};return true}
}

// B514_START_REFERENCE_COMPOSITION_NATIVE_VECTOR_FINAL
