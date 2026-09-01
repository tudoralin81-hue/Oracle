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

/** B514 native Start UI only. Protected modules are untouched. */
class OracleMysticStartView(context: Context, private val onModule: (String) -> Unit) : View(context) {
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val path = Path()
    private val hits = mutableListOf<Pair<RectF, String>>()
    private val gold = Color.rgb(238,190,60)
    private val brightGold = Color.rgb(255,215,82)
    private val white = Color.rgb(244,239,226)
    private val muted = Color.rgb(151,143,126)
    private val green = Color.rgb(65,245,92)
    private val modules = listOf(
        Module("portfolio","PORTFOLIO","OVERVIEW",Color.rgb(220,60,255)),
        Module("watchlist","WATCHLIST","TRACK & FOCUS",Color.rgb(255,205,35)),
        Module("analysis","ANALYSIS","CHARTS & TOOLS",Color.rgb(25,220,255)),
        Module("growth","GROWTH","FUTURE SCAN",Color.rgb(115,255,45)),
        Module("alerts","ALERTS","STAY AHEAD",Color.rgb(255,55,40)),
        Module("news","NEWS","MARKET PULSE",Color.rgb(35,215,255)),
        Module("knowledge","KNOWLEDGE","LEARN & EVOLVE",Color.rgb(255,205,45)),
        Module("stock","STOCK","INTELLIGENCE",Color.rgb(220,65,255))
    )
    private data class Module(val key:String,val title:String,val subtitle:String,val color:Int)

    override fun onDraw(c: Canvas) {
        super.onDraw(c)
        val w=width.toFloat(); val h=height.toFloat()
        val wide=w/h>1.15f
        val sx=w/720f
        val sy=if(wide) min(sx*0.72f,h/1080f) else min(sx,h/1080f)
        val ox=(w-720f*sx)/2f
        val topInset=if(wide) 18f else 28f
        fun x(v:Float)=ox+v*sx
        fun y(v:Float)=topInset+v*sy
        fun s(v:Float)=v*((sx+sy)*0.5f)
        paint.style=Paint.Style.FILL; paint.color=Color.rgb(1,2,4); paint.alpha=255; c.drawRect(0f,0f,w,h,paint)

        val cx=x(360f); val eyeY=y(285f); val t=System.nanoTime()/1_000_000_000.0
        paint.style=Paint.Style.STROKE; paint.color=gold; paint.alpha=30; paint.strokeWidth=s(.55f)
        for(i in 0 until 14)c.drawCircle(cx,eyeY,s(105f+i*18f),paint)
        for(i in 0 until 32){val a=i*Math.PI/16.0;val dx=cos(a).toFloat();val dy=sin(a).toFloat();c.drawLine(cx+dx*s(90f),eyeY+dy*s(90f),cx+dx*s(335f),eyeY+dy*s(335f),paint)}

        drawCentered(c,"ORACLE",cx,y(105f),s(31f),brightGold,Typeface.SERIF,.18f)
        drawCentered(c,"STOCK INTELLIGENCE",cx,y(130f),s(9f),gold,Typeface.DEFAULT,.25f)
        drawSigil(c,cx,y(57f),s(22f),brightGold)
        drawAnimatedEye(c,cx,eyeY,s(132f),t)
        drawCentered(c,"SEE MORE.  KNOW FIRST.",cx,y(465f),s(10.5f),white,Typeface.DEFAULT,.27f)
        paint.style=Paint.Style.STROKE;paint.color=gold;paint.alpha=180;paint.strokeWidth=s(.7f);c.drawLine(x(215f),y(486f),x(505f),y(486f),paint);drawDiamond(c,cx,y(486f),s(4f),brightGold)

        hits.clear()
        val left=20f;val top=510f;val cw=164f;val ch=120f;val gap=12f
        for(i in modules.indices){val col=i%4;val row=i/4;val l=left+col*(cw+gap);val q=top+row*(ch+gap);val r=RectF(x(l),y(q),x(l+cw),y(q+ch));hits+=r to modules[i].key;drawCard(c,r,modules[i],sx,sy,t,i)}

        drawStatus(c,::x,::y,::s,t)
        drawCentered(c,"ORACLE",cx,y(955f),s(20f),gold,Typeface.SERIF,.34f)
        drawCentered(c,"SEE MORE.  KNOW FIRST.",cx,y(980f),s(8f),muted,Typeface.DEFAULT,.22f)
        paint.style=Paint.Style.STROKE;paint.color=gold;paint.alpha=145;paint.strokeWidth=s(.6f);c.drawLine(x(285f),y(1000f),x(435f),y(1000f),paint);drawDiamond(c,cx,y(1000f),s(3f),gold)
        drawCentered(c,"357AT2026",cx,y(1042f),s(11f),brightGold,Typeface.DEFAULT_BOLD,.16f)
        postInvalidateDelayed(32L)
    }

    private fun drawAnimatedEye(c:Canvas,x:Float,y:Float,r:Float,time:Double){
        val p=(.5+.5*sin(time*1.7)).toFloat()
        paint.style=Paint.Style.STROKE
        path.reset();path.moveTo(x-r,y);path.cubicTo(x-r*.58f,y-r*.55f,x+r*.58f,y-r*.55f,x+r,y);path.cubicTo(x+r*.58f,y+r*.55f,x-r*.58f,y+r*.55f,x-r,y);paint.color=brightGold;paint.alpha=(175+75*p).toInt();paint.strokeWidth=r*.018f;c.drawPath(path,paint)
        paint.alpha=(45+85*p).toInt();paint.strokeWidth=r*.012f;c.drawCircle(x,y,r*(.48f+.035f*p),paint)
        paint.color=Color.rgb(55,255,85);paint.alpha=(135+120*p).toInt();paint.strokeWidth=r*.018f;c.drawCircle(x,y,r*(.30f+.025f*p),paint)
        paint.style=Paint.Style.FILL;paint.color=Color.rgb(2,12,5);paint.alpha=255;c.drawCircle(x,y,r*.28f,paint)
        paint.color=Color.rgb(70,255,95);paint.alpha=255;c.drawCircle(x,y,r*(.095f+.035f*p),paint)
        paint.style=Paint.Style.STROKE;paint.color=Color.rgb(255,110,35);paint.alpha=(75+90*p).toInt();paint.strokeWidth=r*.009f
        for(i in 0 until 28){val a=i*Math.PI/14.0;val inn=r*.40f;val out=r*(.56f+.055f*p);c.drawLine(x+cos(a).toFloat()*inn,y+sin(a).toFloat()*inn,x+cos(a).toFloat()*out,y+sin(a).toFloat()*out,paint)}
    }

    private fun drawCard(c:Canvas,r:RectF,m:Module,sx:Float,sy:Float,time:Double,index:Int){
        fun ss(v:Float)=v*((sx+sy)*.5f)
        val p=(.5+.5*sin(time*1.35+index*.48)).toFloat();val cx=r.centerX();val cy=r.top+r.height()*.37f;val ir=min(r.width(),r.height())*.25f
        paint.style=Paint.Style.FILL;paint.color=Color.rgb(2,4,7);paint.alpha=250;c.drawRoundRect(r,ss(9f),ss(9f),paint)
        paint.style=Paint.Style.STROKE;paint.color=m.color;paint.alpha=(150+95*p).toInt();paint.strokeWidth=ss(1.25f);c.drawRoundRect(r,ss(9f),ss(9f),paint)
        paint.alpha=(40+90*p).toInt();paint.strokeWidth=ss(1f);c.drawCircle(cx,cy,ir*(1.06f+.10f*p),paint)
        paint.alpha=(120+120*p).toInt();paint.strokeWidth=ss(1.1f);c.drawCircle(cx,cy,ir,paint);paint.alpha=90;c.drawCircle(cx,cy,ir*.78f,paint)
        paint.alpha=255;paint.strokeWidth=ss(1.9f)
        when(m.key){
            "watchlist"->drawEyeIcon(c,cx,cy,ir*.70f,m.color)
            "portfolio"->{c.drawRect(cx-ir*.45f,cy-ir*.35f,cx+ir*.45f,cy+ir*.35f,paint);c.drawCircle(cx+ir*.22f,cy+ir*.15f,ir*.17f,paint)}
            "analysis","growth"->{path.reset();path.moveTo(cx-ir*.60f,cy+ir*.35f);path.lineTo(cx-ir*.20f,cy);path.lineTo(cx+ir*.02f,cy+ir*.15f);path.lineTo(cx+ir*.58f,cy-ir*.48f);c.drawPath(path,paint)}
            "alerts"->{c.drawArc(RectF(cx-ir*.48f,cy-ir*.45f,cx+ir*.48f,cy+ir*.45f),210f,120f,false,paint);c.drawLine(cx-ir*.22f,cy+ir*.40f,cx+ir*.22f,cy+ir*.40f,paint)}
            "news"->{c.drawRect(cx-ir*.46f,cy-ir*.43f,cx+ir*.46f,cy+ir*.43f,paint);for(j in -1..1)c.drawLine(cx-ir*.28f,cy+j*ir*.18f,cx+ir*.28f,cy+j*ir*.18f,paint)}
            "knowledge"->{c.drawRect(cx-ir*.48f,cy-ir*.43f,cx,cy+ir*.43f,paint);c.drawRect(cx,cy-ir*.43f,cx+ir*.48f,cy+ir*.43f,paint)}
            else->drawSigil(c,cx,cy,ir*.70f,m.color)
        }
        drawCentered(c,m.title,cx,r.top+r.height()*.72f,ss(11.5f),white,Typeface.DEFAULT,.01f)
        drawCentered(c,m.subtitle,cx,r.top+r.height()*.87f,ss(7.8f),m.color,Typeface.DEFAULT,.02f)
        paint.color=m.color;paint.alpha=(130+110*p).toInt();paint.strokeWidth=ss(1f);paint.style=Paint.Style.STROKE;c.drawLine(cx-ss(35f),r.bottom-ss(13f),cx+ss(35f),r.bottom-ss(13f),paint);drawDiamond(c,cx,r.bottom-ss(13f),ss(3.5f),m.color)
    }

    private fun drawStatus(c:Canvas,x:(Float)->Float,y:(Float)->Float,s:(Float)->Float,time:Double){
        val p=(.5+.5*sin(time*.85)).toFloat();val r=RectF(x(20f),y(800f),x(700f),y(886f))
        paint.style=Paint.Style.FILL;paint.color=Color.rgb(3,6,8);paint.alpha=250;c.drawRoundRect(r,s(10f),s(10f),paint)
        paint.style=Paint.Style.STROKE;paint.color=green;paint.alpha=(120+110*p).toInt();paint.strokeWidth=s(1.1f);c.drawRoundRect(r,s(10f),s(10f),paint)
        drawMiniEye(c,x(72f),y(843f),s(24f),green,p)
        drawLeft(c,"ORACLE READY",x(112f),y(837f),s(14f),white,Typeface.DEFAULT_BOLD);drawLeft(c,"Market Intelligence Active",x(112f),y(861f),s(9f),green,Typeface.DEFAULT)
        paint.color=gold;paint.alpha=(150+90*p).toInt();paint.strokeWidth=s(.9f);c.drawCircle(x(378f),y(843f),s(24f+3f*p),paint)
        for(i in -2..2){val xx=x(378f+i*6f);val half=s(9f+abs(i)*2f);c.drawLine(xx,y(843f)-half,xx,y(843f)+half,paint)}
        drawLeft(c,"LOCAL INTELLIGENCE",x(416f),y(837f),s(13f),white,Typeface.DEFAULT_BOLD);drawLeft(c,"Synced & Protected",x(416f),y(861f),s(9f),green,Typeface.DEFAULT)
        paint.style=Paint.Style.FILL;paint.color=green;paint.alpha=255;c.drawCircle(x(675f),y(843f),s(7f+3f*p),paint)
    }

    private fun drawMiniEye(c:Canvas,x:Float,y:Float,r:Float,color:Int,p:Float){paint.style=Paint.Style.STROKE;paint.color=color;paint.alpha=(165+80*p).toInt();paint.strokeWidth=r*.10f;path.reset();path.moveTo(x-r,y);path.cubicTo(x-r*.55f,y-r*.55f,x+r*.55f,y-r*.55f,x+r,y);path.cubicTo(x+r*.55f,y+r*.55f,x-r*.55f,y+r*.55f,x-r,y);c.drawPath(path,paint);paint.style=Paint.Style.FILL;paint.alpha=255;c.drawCircle(x,y,r*(.18f+.05f*p),paint)}
    private fun drawEyeIcon(c:Canvas,x:Float,y:Float,r:Float,color:Int){paint.style=Paint.Style.STROKE;paint.color=color;paint.alpha=255;paint.strokeWidth=r*.09f;path.reset();path.moveTo(x-r,y);path.cubicTo(x-r*.55f,y-r*.55f,x+r*.55f,y-r*.55f,x+r,y);path.cubicTo(x+r*.55f,y+r*.55f,x-r*.55f,y+r*.55f,x-r,y);c.drawPath(path,paint);c.drawCircle(x,y,r*.23f,paint)}
    private fun drawSigil(c:Canvas,x:Float,y:Float,r:Float,color:Int){paint.style=Paint.Style.STROKE;paint.color=color;paint.alpha=245;paint.strokeWidth=r*.065f;path.reset();path.moveTo(x-r*.75f,y);path.lineTo(x,y-r*.5f);path.lineTo(x+r*.75f,y);path.lineTo(x,y+r*.5f);path.close();c.drawPath(path,paint);c.drawCircle(x,y,r*.18f,paint)}
    private fun drawDiamond(c:Canvas,x:Float,y:Float,r:Float,color:Int){paint.style=Paint.Style.STROKE;paint.color=color;paint.alpha=230;paint.strokeWidth=r*.32f;path.reset();path.moveTo(x,y-r);path.lineTo(x+r,y);path.lineTo(x,y+r);path.lineTo(x-r,y);path.close();c.drawPath(path,paint)}
    private fun drawCentered(c:Canvas,text:String,x:Float,y:Float,size:Float,color:Int,typeface:Typeface,spacing:Float){paint.style=Paint.Style.FILL;paint.color=color;paint.alpha=255;paint.typeface=typeface;paint.textSize=size;paint.textAlign=Paint.Align.CENTER;paint.letterSpacing=spacing;c.drawText(text,x,y,paint);paint.letterSpacing=0f}
    private fun drawLeft(c:Canvas,text:String,x:Float,y:Float,size:Float,color:Int,typeface:Typeface){paint.style=Paint.Style.FILL;paint.color=color;paint.alpha=255;paint.typeface=typeface;paint.textSize=size;paint.textAlign=Paint.Align.LEFT;c.drawText(text,x,y,paint)}

    override fun onTouchEvent(e:MotionEvent):Boolean{if(e.action==MotionEvent.ACTION_UP){for((r,key) in hits)if(r.contains(e.x,e.y)){onModule(key);performClick();return true}};return true}
    override fun performClick():Boolean{super.performClick();return true}
}