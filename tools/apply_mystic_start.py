from pathlib import Path

path = Path("app/src/main/java/ro/alintudor/oracle/MainActivity.kt")
text = path.read_text(encoding="utf-8")
marker = "private class OracleHeroView"
head, sep, _ = text.partition(marker)
if not sep:
    raise SystemExit("OracleHeroView marker not found; refusing to modify MainActivity")

new_class = r'''private class OracleHeroView(context:android.content.Context,private val onModule:(String)->Unit):View(context){
    private val p=Paint(Paint.ANTI_ALIAS_FLAG)
    private val stars=Array(95){i -> floatArrayOf(((i*137+41)%1000)/1000f,((i*271+73)%1000)/1000f,((i%4)+1)*.7f)}
    private val nodes=listOf(
        Node("portfolio","PORTFOLIO",Color.rgb(188,76,255),.50f,.16f),
        Node("alerts","ALERTS",Color.rgb(255,82,48),.17f,.31f),
        Node("news","NEWS",Color.rgb(25,215,255),.83f,.31f),
        Node("growth","GROWTH",Color.rgb(140,245,45),.13f,.55f),
        Node("knowledge","KNOWLEDGE",Color.rgb(255,205,48),.87f,.55f),
        Node("analysis","ANALYSIS",Color.rgb(40,210,255),.30f,.79f),
        Node("watchlist","WATCHLIST",Color.rgb(255,214,48),.70f,.79f))
    private data class Node(val key:String,val label:String,val color:Int,val x:Float,val y:Float)
    private var phase=0f

    init { isClickable=true; setLayerType(View.LAYER_TYPE_SOFTWARE,null) }

    override fun onDraw(c:Canvas){
        val w=width.toFloat(); val h=height.toFloat(); val d=resources.displayMetrics.density
        val cx=w*.5f; val cy=h*.485f; val base=minOf(w,h); val r=base*.205f; val nr=base*.095f
        c.drawColor(Color.rgb(1,3,8))

        // Deep-space / nebula atmosphere.
        p.style=Paint.Style.FILL
        p.shader=LinearGradient(0f,0f,w,h,Color.rgb(1,3,10),Color.rgb(8,4,15),Shader.TileMode.CLAMP)
        c.drawRect(0f,0f,w,h,p); p.shader=null
        for((i,s) in stars.withIndex()){
            val twinkle=(0.45f+0.55f*((kotlin.math.sin(phase*.8f+i)*.5f)+.5f))
            p.color=Color.argb((35+55*twinkle).toInt(),255,220,130)
            c.drawCircle(s[0]*w,s[1]*h,s[2]*d,p)
        }
        // Misty arcs.
        p.style=Paint.Style.STROKE; p.strokeWidth=1f*d
        for(i in 0..4){
            p.color=Color.argb(20,90,90,255)
            val rr=base*(.43f+i*.095f)
            c.drawArc(cx-rr,cy-rr,cx+rr,cy+rr,188f+i*17f,145f,false,p)
        }

        // Sacred orbital geometry.
        for(i in 1..7){
            val rr=r*(1f+i*.48f)
            p.strokeWidth=if(i==1)1.7f*d else .75f*d
            p.color=Color.argb(if(i<3)105 else 52,255,190,55)
            c.drawCircle(cx,cy,rr,p)
        }
        for(i in 0 until 8){
            val a=(Math.PI*2*i/8.0)-Math.PI/2
            val ex=cx+kotlin.math.cos(a).toFloat()*base*.48f
            val ey=cy+kotlin.math.sin(a).toFloat()*base*.48f
            p.color=Color.argb(65,255,205,70); p.strokeWidth=.7f*d
            c.drawLine(cx,cy,ex,ey,p)
        }
        drawRunes(c,cx,cy,r*1.30f,d)

        // Node connections.
        for(n in nodes){
            val x=w*n.x; val y=h*n.y
            p.color=Color.argb(75,255,205,70); p.strokeWidth=.8f*d
            c.drawLine(cx,cy,x,y,p)
            drawNode(c,x,y,nr,n,d)
        }

        // Central oracle portal.
        p.style=Paint.Style.FILL
        p.shader=LinearGradient(cx-r,cy-r,cx+r,cy+r,Color.rgb(255,232,120),Color.rgb(201,122,20),Shader.TileMode.CLAMP)
        c.drawCircle(cx,cy,r*1.16f,p); p.shader=null
        p.color=Color.rgb(5,7,13); c.drawCircle(cx,cy,r*1.02f,p)
        p.style=Paint.Style.STROKE; p.strokeWidth=1.4f*d; p.color=Color.argb(205,255,213,75); c.drawCircle(cx,cy,r*.90f,p)
        p.strokeWidth=.8f*d; p.color=Color.argb(100,255,195,65); c.drawCircle(cx,cy,r*.72f,p)
        // Eye symbol.
        val eye=Path(); eye.moveTo(cx-r*.20f,cy-r*.22f); eye.cubicTo(cx-r*.07f,cy-r*.38f,cx+r*.07f,cy-r*.38f,cx+r*.20f,cy-r*.22f); eye.cubicTo(cx+r*.07f,cy-r*.05f,cx-r*.07f,cy-r*.05f,cx-r*.20f,cy-r*.22f); p.color=Color.rgb(255,211,60); p.strokeWidth=1.8f*d; c.drawPath(eye,p); c.drawCircle(cx,cy-r*.22f,r*.045f,p)
        // ORACLE typography.
        p.textAlign=Paint.Align.CENTER; p.typeface=Typeface.create(Typeface.SERIF,Typeface.NORMAL); p.color=Color.WHITE; p.textSize=r*.27f; c.drawText("ORACLE",cx,cy+r*.12f,p)
        p.typeface=Typeface.create(Typeface.SERIF,Typeface.BOLD); p.color=Color.rgb(255,207,65); p.textSize=r*.095f; c.drawText("STOCK INTELLIGENCE",cx,cy+r*.31f,p)
        // Market line.
        val chart=Path(); chart.moveTo(cx-r*.62f,cy+r*.56f)
        val pts=arrayOf(.00f to .04f,.10f to -.01f,.20f to .12f,.30f to .02f,.42f to .19f,.52f to .11f,.64f to .29f,.74f to .22f,.84f to .43f,.93f to .36f,1f to .57f)
        for((xx,yy) in pts) chart.lineTo(cx-r*.62f+r*1.24f*xx,cy+r*.56f-r*.35f*yy)
        p.color=Color.rgb(255,202,45); p.strokeWidth=1.5f*d; c.drawPath(chart,p)

        drawHeader(c,w,base,d)
        phase += .018f
        postInvalidateDelayed(55)
    }

    private fun drawHeader(c:Canvas,w:Float,base:Float,d:Float){
        p.style=Paint.Style.STROKE; p.strokeWidth=1.3f*d; p.color=Color.rgb(128,99,35)
        c.drawRoundRect(4*d,10*d,48*d,54*d,10*d,10*d,p); c.drawRoundRect(w-48*d,10*d,w-4*d,54*d,10*d,10*d,p)
        p.strokeWidth=2.2f*d
        for(i in 0..2){val yy=(25+i*7)*d;c.drawLine(15*d,yy,37*d,yy,p)}
        c.drawArc(w-37*d,18*d,w-15*d,40*d,-55f,285f,false,p); c.drawLine(w-15*d,18*d,w-15*d,26*d,p)
        p.style=Paint.Style.FILL; p.textAlign=Paint.Align.CENTER; p.typeface=Typeface.create(Typeface.SERIF,Typeface.BOLD); p.textSize=base*.034f; p.color=Color.WHITE; c.drawText("ORACLE",w*.5f,base*.055f,p)
        p.textSize=base*.017f; p.color=Color.rgb(177,150,85); c.drawText("STOCK INTELLIGENCE",w*.5f,base*.080f,p)
    }

    private fun drawRunes(c:Canvas,cx:Float,cy:Float,rr:Float,d:Float){
        p.style=Paint.Style.FILL; p.textAlign=Paint.Align.CENTER; p.typeface=Typeface.SERIF; p.textSize=9f*d; p.color=Color.argb(105,255,205,80)
        val glyphs="✦ ◇ ᛉ ᚦ ✧ ᛟ ◈ ᚱ ✦ ᛏ ◇ ᛃ ✧ ᚲ ◈ ᛞ"
        for(i in glyphs.indices step 2){
            val a=(Math.PI*2*i/glyphs.length)-Math.PI/2; val x=cx+kotlin.math.cos(a).toFloat()*rr; val y=cy+kotlin.math.sin(a).toFloat()*rr
            c.save(); c.rotate((a*180/Math.PI).toFloat()+90f,x,y); c.drawText(glyphs[i].toString(),x,y,p); c.restore()
        }
    }

    private fun drawNode(c:Canvas,x:Float,y:Float,rad:Float,n:Node,d:Float){
        p.style=Paint.Style.FILL; p.color=Color.argb(238,3,7,15); c.drawCircle(x,y,rad*1.06f,p)
        p.style=Paint.Style.STROKE
        for(i in 3 downTo 1){p.strokeWidth=(i*.75f)*d;p.color=Color.argb(18*i,n.color shr 16 and 255,n.color shr 8 and 255,n.color and 255);c.drawCircle(x,y,rad*(1f+i*.035f),p)}
        p.strokeWidth=2.0f*d; p.color=n.color; c.drawCircle(x,y,rad,p)
        p.style=Paint.Style.FILL; p.color=n.color; c.drawCircle(x,y-rad*.72f,rad*.035f,p)
        drawIcon(c,x,y-rad*.25f,rad*.28f,n.key,n.color,d)
        p.textAlign=Paint.Align.CENTER; p.typeface=Typeface.DEFAULT_BOLD; p.textSize=rad*.25f; p.color=n.color; c.drawText(n.label,x,y+rad*.17f,p)
        p.textSize=rad*.105f; p.color=Color.rgb(235,235,240)
        val desc=when(n.key){"portfolio"->"Performanță și poziții";"alerts"->"Semnale și evenimente";"news"->"Știri financiare";"growth"->"Acțiuni cu potențial";"knowledge"->"Idei și documentație";"analysis"->"Analiză detaliată";else->"Acțiuni favorite"}
        c.drawText(desc,x,y+rad*.42f,p); p.textSize=rad*.28f; c.drawText("›",x,y+rad*.73f,p)
    }

    private fun drawIcon(c:Canvas,x:Float,y:Float,s:Float,key:String,color:Int,d:Float){
        p.style=Paint.Style.STROKE; p.strokeWidth=1.8f*d; p.strokeCap=Paint.Cap.ROUND; p.strokeJoin=Paint.Join.ROUND; p.color=color
        when(key){
            "portfolio"->{c.drawCircle(x,y,s*.62f,p);c.drawLine(x,y,x,y-s*.62f,p);c.drawLine(x,y,x+s*.48f,y+s*.28f,p)}
            "alerts"->{c.drawArc(x-s*.48f,y-s*.35f,x+s*.48f,y+s*.42f,205f,130f,false,p);c.drawLine(x-s*.58f,y+s*.42f,x+s*.58f,y+s*.42f,p);c.drawCircle(x,y+s*.62f,s*.07f,p)}
            "news"->{c.drawRect(x-s*.58f,y-s*.55f,x+s*.58f,y+s*.55f,p);c.drawLine(x-s*.35f,y-s*.20f,x+s*.35f,y-s*.20f,p);c.drawLine(x-s*.35f,y,x+s*.35f,y,p);c.drawLine(x-s*.35f,y+s*.20f,x+s*.18f,y+s*.20f,p)}
            "growth"->{val q=Path();q.moveTo(x-s*.58f,y+s*.18f);q.lineTo(x-s*.10f,y-s*.28f);q.lineTo(x+s*.08f,y-s*.05f);q.lineTo(x+s*.58f,y-s*.52f);c.drawPath(q,p);c.drawLine(x+s*.30f,y-s*.52f,x+s*.58f,y-s*.52f,p);c.drawLine(x+s*.58f,y-s*.52f,x+s*.58f,y-s*.25f,p)}
            "knowledge"->{c.drawRect(x-s*.58f,y-s*.52f,x-.03f,y+s*.52f,p);c.drawRect(x+.03f,y-s*.52f,x+s*.58f,y+s*.52f,p);c.drawLine(x,y-s*.52f,x,y+s*.52f,p)}
            "analysis"->{c.drawLine(x-s*.58f,y+s*.45f,x-s*.58f,y-s*.48f,p);c.drawLine(x-s*.58f,y+s*.45f,x+s*.58f,y+s*.45f,p);val q=Path();q.moveTo(x-s*.48f,y+s*.20f);q.lineTo(x-s*.15f,y-s*.10f);q.lineTo(x+s*.08f,y+s*.04f);q.lineTo(x+s*.48f,y-s*.40f);c.drawPath(q,p)}
            "watchlist"->{c.drawOval(x-s*.65f,y-s*.35f,x+s*.65f,y+s*.35f,p);c.drawCircle(x,y,s*.16f,p)}
        }
        p.strokeCap=Paint.Cap.BUTT
    }

    override fun onTouchEvent(e:MotionEvent):Boolean{
        if(e.action!=MotionEvent.ACTION_UP)return true
        val w=width.toFloat(); val h=height.toFloat(); val hit=minOf(w,h)*.125f
        for(n in nodes){val dx=e.x-w*n.x;val dy=e.y-h*n.y;if(dx*dx+dy*dy<=hit*hit){onModule(n.key);performClick();return true}}
        return true
    }
    override fun performClick():Boolean{super.performClick();return true}
}'''
path.write_text(head + new_class + "\n",encoding="utf-8")
print("Mystic Start applied")
