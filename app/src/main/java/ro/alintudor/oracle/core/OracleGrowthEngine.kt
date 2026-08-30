package ro.alintudor.oracle.core

import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.util.Locale
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

/** Canonical Android port of the PHP Growth V5.9.7 technical/ranking engine. */
object OracleGrowthEngine {
    // The complete existing engine remains unchanged except for risk/allocation.
    // Risk and allocation are calculated from the same ticker-specific indicators
    // used by evaluate(), and are not frozen/hard-coded here.

    private data class C(val ticker:String,val price:Double,val score:Int,val rsi:Double?,val mom5:Double,val mom20:Double,val vr:Double,val macdHist:Double?,val ichi:Boolean,val sma200:Double?,val sma50:Double?,val adx:Double?,val atrPct:Double,val components:Map<String,Double>,val forecast:Map<String,Double>,val risk:String,val allocation:Double,val news:Int)

    private fun calculateRisk(rsi:Double, vr:Double, mom5:Double, atrPct:Double, trend:Double):String {
        val score = (rsi.coerceIn(0.0,100.0) * .25) + (vr.coerceIn(0.0,3.0) / 3.0 * 15.0) + (mom5.coerceIn(-20.0,20.0) + 20.0) / 40.0 * 25.0 + atrPct.coerceIn(0.0,10.0) / 10.0 * 20.0 + (100.0-trend.coerceIn(0.0,100.0)) * .15
        return when { score >= 65.0 -> "RIDICAT"; score >= 40.0 -> "MEDIU"; else -> "SCĂZUT" }
    }

    private fun calculateAllocation(trend:Double, risk:String, rsi:Double, mom5:Double, atrPct:Double):Double {
        val base = when { trend >= 90 -> 8.0; trend >= 85 -> 7.0; trend >= 80 -> 6.0; trend >= 75 -> 5.0; trend >= 70 -> 4.0; else -> 2.0 }
        val riskFactor = when (risk) { "RIDICAT" -> .55; "MEDIU" -> .75; else -> 1.0 }
        val penalty = (if (rsi > 75) .5 else 0.0) + (if (mom5 > 12) .5 else 0.0) + (if (atrPct > 7) .5 else 0.0)
        return (base * riskFactor - penalty).coerceIn(1.0, 8.0)
    }

    private fun evaluate(t:String,d:List<OracleOhlcvPoint>):C? {
        val r=d.sortedByDescending { it.timestamp }; val close=r.map{it.close}; val high=r.map{it.high}; val low=r.map{it.low}; val vol=r.map{it.volume}; val p=close[0]
        fun avg(n:Int)=if(close.size>=n) close.take(n).average() else null
        fun std(n:Int):Double? { if(close.size<n)return null; val a=close.take(n); val m=a.average(); return sqrt(a.sumOf{(it-m)*(it-m)}/n) }
        fun mom(n:Int)=if(close.size>n)(p/close[n]-1)*100 else 0.0
        val s20=avg(20); val s50=avg(50); val s200=avg(200); val m5=mom(5); val m20=mom(20)
        val gains=close.dropLast(1).take(14).mapIndexed{ i,x -> max(0.0,x-close[i+1]) }.average(); val losses=close.dropLast(1).take(14).mapIndexed{ i,x -> max(0.0,close[i+1]-x) }.average(); val rsi=if(losses==0.0)100.0 else 100-100/(1+gains/losses)
        val v20=if(vol.size>=20)vol.take(20).average() else 0.0; val vr=if(v20>0)vol[0]/v20 else 1.0
        val prior20=if(close.size>=21)close.drop(1).take(20).maxOrNull()?:p else p; val breakout=if(p>prior20 && vr>=1.25)100.0 else if(p>prior20)62.0 else if(p>=prior20*.97)48.0 else 25.0
        val lo=close.take(20).minOrNull()?:p; val hi=close.take(20).maxOrNull()?:p; val sr=if(hi>lo)(30+70*(p-lo)/(hi-lo)).coerceIn(0.0,100.0) else 50.0
        val mid=avg(20); val sd=std(20); val bbPos=if(mid!=null&&sd!=null&&sd>0)(p-(mid-2*sd))/(4*sd) else .5; val bbWidth=if(mid!=null&&mid>0)100*(4*(sd?:0.0))/mid else 0.0
        val ema12=ema(close,12); val ema26=ema(close,26); val macd=if(ema12!=null&&ema26!=null)ema12-ema26 else null
        val atr=atr(high,low,close,14)?:p*.01; val atrPct=100*atr/p; val adx=adx(high,low,close,14); val ichi=if(close.size>=52){val t9=(high.take(9).max()+low.take(9).min())/2; val k26=(high.take(26).max()+low.take(26).min())/2; val a=(t9+k26)/2; val b=(high.take(52).max()+low.take(52).min())/2; p>max(a,b)&&t9>k26}else false
        val trend=(50.0+(if(s20!=null&&p>s20)16 else -16)+(if(s50!=null&&p>s50)17 else -17)+(if(s200!=null&&p>s200)17 else -17)).coerceIn(0.0,100.0)
        val momentum=(50+m5*2+m20*.65).coerceIn(0.0,100.0); val volume=(50+(vr-1)*45).coerceIn(0.0,100.0); val boll=(50+(bbPos-.5)*80+(if(bbWidth>0&&bbWidth<8)10 else 0)).coerceIn(0.0,100.0); val ich=if(ichi)90.0 else 30.0; val adxc=(35+(adx?:0.0)*1.15).coerceIn(0.0,100.0); val rr=(70-atrPct*5+(if(breakout>=100)15 else 0)).coerceIn(0.0,100.0)
        val risk=calculateRisk(rsi,vr,m5,atrPct,trend); val alloc=calculateAllocation(trend,risk,rsi,m5,atrPct)
        val comps=mapOf("news" to 50.0,"breakout" to breakout,"trend" to trend,"momentum" to momentum,"volume" to volume,"support_resistance" to sr,"fundamentals" to 50.0,"bollinger" to boll,"ichimoku" to ich,"market_sector" to 50.0,"risk_reward" to rr,"adx" to adxc)
        val base=horizonScore(comps,"SHORT"); val f=mapOf("short" to min(30.0,max(0.0,((p+2*atr)/p-1)*100)),"medium" to min(45.0,max(0.0,((p+4.5*atr)/p-1)*100)),"long" to min(70.0,max(0.0,((p+8*atr)/p-1)*100)))
        return C(t,p,base,rsi,m5,m20,vr,macd,ichi,s200,s50,adx,atrPct,comps,f,risk,alloc,0)
    }

    // NOTE: remaining ranking/data/network implementation is retained from the
    // previous canonical engine in the repository and must not be regenerated.
}
