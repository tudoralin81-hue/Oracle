package ro.alintudor.oracle.core

import java.util.Locale
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt
import kotlin.math.roundToInt

/** Single-ticker analysis using the same 12 Growth factors and horizon weights. */
object OracleAnalysisEngine {
    val factorNames = listOf("News","Breakout","Trend","Momentum","Volume","Support / Resistance","Fundamentals","Bollinger","Ichimoku","Market / Sector","Risk / Reward","ADX")
    val factorKeys = listOf("news","breakout","trend","momentum","volume","support_resistance","fundamentals","bollinger","ichimoku","market_sector","risk_reward","adx")
    val shortWeights = intArrayOf(21,18,12,16,12,8,3,4,2,2,1,1)
    val mediumWeights = intArrayOf(12,12,16,12,9,9,9,5,6,5,4,1)
    val longWeights = intArrayOf(6,6,20,7,5,8,18,4,9,7,9,2)

    data class Result(
        val ticker:String,
        val price:Double,
        val shortScore:Int,
        val mediumScore:Int,
        val longScore:Int,
        val signal:String,
        val risk:String,
        val allocation:Double,
        val rsi:Double,
        val momentum5D:Double,
        val momentum20D:Double,
        val volumeRatio:Double,
        val sma50:Double?,
        val sma200:Double?,
        val adx:Double?,
        val atrPct:Double,
        val factors:List<Double>
    )

    fun analyze(raw:String):Result? {
        val ticker=raw.trim().uppercase(Locale.US)
        if(!ticker.matches(Regex("[A-Z][A-Z0-9.\\-]{0,9}"))) return null
        val d=OracleMarketData.fetchDaily(ticker,"1y").sortedByDescending{it.timestamp}
        if(d.size<60) return null
        val close=d.map{it.close}; val high=d.map{it.high}; val low=d.map{it.low}; val vol=d.map{it.volume}; val p=close.first()
        fun avg(n:Int)=if(close.size>=n)close.take(n).average() else null
        fun std(n:Int):Double?{if(close.size<n)return null;val a=close.take(n);val m=a.average();return sqrt(a.sumOf{(it-m)*(it-m)}/n)}
        fun mom(n:Int)=if(close.size>n)(p/close[n]-1)*100 else 0.0
        val s20=avg(20);val s50=avg(50);val s200=avg(200);val m5=mom(5);val m20=mom(20)
        val gains=close.dropLast(1).take(14).mapIndexed{i,x->max(0.0,x-close[i+1])}.average();val losses=close.dropLast(1).take(14).mapIndexed{i,x->max(0.0,close[i+1]-x)}.average();val rsi=if(losses==0.0)100.0 else 100-100/(1+gains/losses)
        val v20=if(vol.size>=20)vol.take(20).average() else 0.0;val vr=if(v20>0)vol[0]/v20 else 1.0
        val prior20=if(close.size>=21)close.drop(1).take(20).maxOrNull()?:p else p;val breakout=if(p>prior20&&vr>=1.25)100.0 else if(p>prior20)62.0 else if(p>=prior20*.97)48.0 else 25.0
        val lo=close.take(20).minOrNull()?:p;val hi=close.take(20).maxOrNull()?:p;val sr=if(hi>lo)(30+70*(p-lo)/(hi-lo)).coerceIn(0.0,100.0) else 50.0
        val mid=avg(20);val sd=std(20);val bbPos=if(mid!=null&&sd!=null&&sd>0)(p-(mid-2*sd))/(4*sd) else .5;val bbWidth=if(mid!=null&&mid>0)100*(4*(sd?:0.0))/mid else 0.0
        val atr=atr(high,low,close,14)?:p*.01;val atrPct=100*atr/p;val adx=adx(high,low,close,14)
        val ichi=if(close.size>=52){val t9=(high.take(9).max()+low.take(9).min())/2;val k26=(high.take(26).max()+low.take(26).min())/2;val a=(t9+k26)/2;val b=(high.take(52).max()+low.take(52).min())/2;p>max(a,b)&&t9>k26}else false
        val trend=(50.0+(if(s20!=null&&p>s20)16 else -16)+(if(s50!=null&&p>s50)17 else -17)+(if(s200!=null&&p>s200)17 else -17)).coerceIn(0.0,100.0)
        val momentum=(50+m5*2+m20*.65).coerceIn(0.0,100.0);val volume=(50+(vr-1)*45).coerceIn(0.0,100.0)
        val boll=(50+(bbPos-.5)*80+(if(bbWidth>0&&bbWidth<8)10 else 0)).coerceIn(0.0,100.0);val ichScore=if(ichi)90.0 else 30.0;val adxScore=(35+(adx?:0.0)*1.15).coerceIn(0.0,100.0);val rr=(70-atrPct*5+(if(breakout>=100)15 else 0)).coerceIn(0.0,100.0)
        // Analysis excludes news from the information set, so News is neutral 50 rather than fetched.
        val factors=listOf(50.0,breakout,trend,momentum,volume,sr,50.0,boll,ichScore,50.0,rr,adxScore)
        fun score(w:IntArray):Int{val raw=factors.indices.sumOf{factors[it]*(w[it]/100.0)}.roundToInt().coerceIn(0,100);return when{raw in 97..100->raw-3;raw in 92..96->raw-1;else->raw}}
        val ss=score(shortWeights);val ms=score(mediumWeights);val ls=score(longWeights)
        val risk=if(rsi>75||vr>2.5||m5>12||atrPct>7)"RIDICAT" else if(trend>=75)"MEDIU" else "RIDICAT"
        var alloc=when{trend>=90->8.0;trend>=85->7.0;trend>=80->6.0;trend>=75->5.0;trend>=70->4.0;else->2.0};if(risk=="RIDICAT")alloc=min(alloc,4.0);if(rsi>75)alloc=min(alloc,3.0);if(m5>12)alloc=min(alloc,3.0);if(atrPct>7)alloc=min(alloc,3.0)
        val signal=when{ss>=85->"STRONG BUY";ss>=75->"BUY";ss>=65->"HOLD";ss>=55->"WATCH";else->"AVOID"}
        return Result(ticker,p,ss,ms,ls,signal,risk,alloc,rsi,m5,m20,vr,s50,s200,adx,atrPct,factors)
    }

    private fun atr(h:List<Double>,l:List<Double>,c:List<Double>,n:Int):Double?{if(c.size<n+1)return null;val tr=(0 until c.size-1).map{i->maxOf(h[i]-l[i],abs(h[i]-c[i+1]),abs(l[i]-c[i+1]))};return tr.take(n).average()}
    private fun adx(h:List<Double>,l:List<Double>,c:List<Double>,n:Int):Double?{if(c.size<n*2+2)return null;val tr=mutableListOf<Double>();val pd=mutableListOf<Double>();val md=mutableListOf<Double>();for(i in 0 until c.size-1){val up=h[i]-h[i+1];val dn=l[i+1]-l[i];tr+=maxOf(h[i]-l[i],abs(h[i]-c[i+1]),abs(l[i]-c[i+1]));pd+=if(up>dn&&up>0)up else 0.0;md+=if(dn>up&&dn>0)dn else 0.0};var atrv=tr.take(n).average();var p=pd.take(n).average();var m=md.take(n).average();val dx=mutableListOf<Double>();for(i in n until tr.size){atrv=(atrv*(n-1)+tr[i])/n;p=(p*(n-1)+pd[i])/n;m=(m*(n-1)+md[i])/n;val pi=if(atrv>0)100*p/atrv else 0.0;val mi=if(atrv>0)100*m/atrv else 0.0;dx+=if(pi+mi>0)100*abs(pi-mi)/(pi+mi) else 0.0};return if(dx.size<n)dx.average() else dx.takeLast(n).average()}
}
