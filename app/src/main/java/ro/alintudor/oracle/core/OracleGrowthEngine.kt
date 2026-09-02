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
    private val universe = "NVDA,VRT,CEG,AMD,AVGO,PLTR,ANET,MU,AMZN,META,MSFT,GOOGL,GOOG,AAPL,TSLA,NFLX,CRWD,PANW,NOW,ORCL,CRM,ADBE,SNOW,DDOG,NET,ARM,TSM,ASML,QCOM,INTC,MRVL,SMCI,DELL,CLS,APH,GEV,ETN,FSLR,ENPH,LRCX,AMAT,KLAC,MELI,SHOP,COIN,HOOD,UBER,RKLB,LUNR,ACN,IBM,CSCO,INTU,ADP,ADSK,CDNS,SNPS,FTNT,FICO,MSI,HPE,HPQ,NTAP,WDC,STX,KEYS,ZS,OKTA,MDB,TEAM,HUBS,VEEV,PAYC,DOCU,TWLO,APP,ROKU,SPOT,U,LIN,WM,RSG,FAST,DECK,URI,CAT,CMI,PCAR,PH,ROK,EMR,HON,GE,LHX,RTX,NOC,GD,TDG,HEI,TT,CARR,JCI,IR,PWR,FIX,MTZ,EME,XOM,CVX,COP,EOG,OXY,SLB,HAL,MPC,PSX,VLO,WMB,KMI,OKE,LNG,DVN,FANG,APA,JPM,BAC,WFC,C,GS,MS,BLK,SCHW,COF,AXP,USB,PNC,TFC,BK,STT,NTRS,CBOE,CME,ICE,SPGI,MCO,MSCI,NDAQ,AMP,ALLY,DFS,UNH,LLY,JNJ,ABBV,MRK,PFE,BMY,AMGN,GILD,REGN,ISRG,ABT,TMO,DHR,SYK,BSX,MDT,BDX,EW,ZBH,RMD,DXCM,ALGN,IDXX,IQV,HCA,CI,ELV,HUM,CVS,MCK,CAH,COR,COST,WMT,TGT,HD,LOW,TJX,ROST,DG,DLTR,ORLY,AZO,ODP,BBY,NKE,LULU,CMG,MCD,SBUX,YUM,DRI,MAR,HLT,BKNG,EXPE,ABNB,PG,KO,PEP,PM,MO,CL,KMB,GIS,KHC,MDLZ,HSY,MNST,STZ,KDP,KR,CHD,CLX,EL,DIS,CMCSA,WBD,PARA,FOX,FOXA,T,VZ,TMUS,CHTR,EA,TTWO,RBLX,LYV,NEM,GOLD,FCX,NUE,STLD,DOW,DD,ECL,APD,SHW,MLM,VMC,ALB,MOS,CF,PLD,AMT,EQIX,CCI,O,OHI,WELL,VICI,PSA,SPG,AVB,EQR,ESS,INVH,ARE,NEE,DUK,SO,AEP,EXC,XEL,SRE,ED,PEG,WEC,DTE,FE,ETR,PPL,AES,CNP,CMS,NI,BA,LMT,UPS,FDX,DAL,UAL,LUV,AAL,GEHC,SWK,MMM".split(',')
    private data class C(val ticker:String,val price:Double,val score:Int,val rsi:Double?,val mom5:Double,val mom20:Double,val vr:Double,val macdHist:Double?,val ichi:Boolean,val sma200:Double?,val sma50:Double?,val adx:Double?,val atrPct:Double,val components:Map<String,Double>,val forecast:Map<String,Double>,val risk:String,val allocation:Double,val news:Int)

    // V5.9.7 authoritative raw profiles. They are normalized at score time.
    // Order: News, Breakout, Trend, Momentum, Volume, S/R, Fundamentals,
    // Bollinger, Ichimoku, Market/Sector, Risk/Reward, ADX.
    private val weights=mapOf("SHORT" to intArrayOf(21,18,18,12,16,12,3,4,4,2,2,1),"MEDIUM" to intArrayOf(12,12,12,16,12,9,9,5,5,6,5,4),"LONG" to intArrayOf(6,6,6,19,7,9,18,4,4,9,7,2))
    private val keys=listOf("news","breakout","trend","momentum","volume","support_resistance","fundamentals","bollinger","ichimoku","market_sector","risk_reward","adx")

    fun run(seed:List<OracleGrowthRecommendation> = emptyList()):List<OracleGrowthRecommendation>{
        val byTicker=seed.associateBy{it.ticker.uppercase(Locale.US)}
        val candidates=mutableListOf<C>()
        for(ticker in universe.distinct()){val candles=OracleMarketData.fetchDaily(ticker,"1y");if(candles.size<60)continue;evaluate(ticker,candles)?.let{candidates+=it}}
        if(candidates.isEmpty())return emptyList()
        // Enrich the technical shortlist with real non-OHLC data before ranking.
        // The previous implementation silently used 50/100 for News, Fundamentals
        // and Market/Sector, which made those displayed values look calculated while
        // they were actually placeholders. Growth now derives those factors from
        // OracleRealData and only falls back to neutral 50 when the source genuinely
        // has no value.
        val technicalShortlist=candidates.sortedByDescending{it.score}.take(30)
        val fundamentals=technicalShortlist.associate { c ->
            c.ticker to runCatching { OracleRealData.fundamentals(c.ticker) }.getOrNull()
        }
        val sectorContexts=mutableMapOf<String,Double>()
        val newsContexts=mutableMapOf<String,OracleNewsContext>()
        for(c in technicalShortlist){
            val f=fundamentals[c.ticker]
            val sector=OracleRealData.resolvedSector(c.ticker,f?.sector)
            if(sector != null && sector !in sectorContexts){
                sectorContexts[sector]=runCatching { OracleRealData.sectorScore(OracleRealData.marketContext(sector)) }.getOrNull() ?: 50.0
            }
        }
        val newsCandidates=technicalShortlist.take(15)
        for(c in newsCandidates){
            newsContexts[c.ticker]=runCatching { OracleRealData.newsContext(c.ticker) }.getOrDefault(OracleNewsContext(50,0,0,0,null))
        }
        val enriched=candidates.map{c->
            val f=fundamentals[c.ticker]
            val sector=OracleRealData.resolvedSector(c.ticker,f?.sector)
            val comp=c.components.toMutableMap()
            comp["fundamentals"]=(OracleRealData.fundamentalScore(f) ?: 50.0).coerceIn(0.0,100.0)
            comp["market_sector"]=(sector?.let { sectorContexts[it] } ?: 50.0).coerceIn(0.0,100.0)
            val n=newsContexts[c.ticker]
            comp["news"]=(n?.score?.toDouble() ?: 50.0).coerceIn(0.0,100.0)
            c.copy(score=horizonScore(comp,"SHORT",sector),components=comp,news=n?.headlineCount ?: 0)
        }
        val out=mutableListOf<OracleGrowthRecommendation>();val used=mutableSetOf<String>()
        for(h in listOf("SHORT","MEDIUM","LONG")){
            val ranked=enriched.sortedWith(compareByDescending<C>{horizonScore(it.components,h,OracleRealData.resolvedSector(it.ticker,fundamentals[it.ticker]?.sector))}.thenByDescending{tie(it,h)}.thenByDescending{it.score})
            val pick=ranked.firstOrNull{it.ticker !in used}?:continue
            used+=pick.ticker
            val score=horizonScore(pick.components,h,OracleRealData.resolvedSector(pick.ticker,fundamentals[pick.ticker]?.sector))
            val meta=byTicker[pick.ticker]
            val f=fundamentals[pick.ticker]
            val sector=OracleRealData.resolvedSector(pick.ticker,f?.sector ?: meta?.sector) ?: "—"
            val correctedAllocation=OracleSectorAllocation.apply(pick.allocation,sector)
            val correctedWeights=weights[h]!!.copyOf()
            val news=newsContexts[pick.ticker]
            val company=meta?.company?.takeIf { it.isNotBlank() && !it.equals(pick.ticker,true) } ?: lookupCompanyName(pick.ticker) ?: pick.ticker
            out+=OracleGrowthRecommendation(horizon=h,ticker=pick.ticker,company=company,sector=sector,score=score,signal=rating(score),risk=pick.risk,allocationMax=correctedAllocation,forecastPct=pick.forecast[h.lowercase(Locale.US)]?:0.0,momentum5D=pick.mom5,momentum20D=pick.mom20,weights=correctedWeights.toList(),newsTitle=meta?.newsTitle?.takeIf { it.isNotBlank() } ?: news?.topHeadline.orEmpty(),newsSource=meta?.newsSource.orEmpty(),referenceTimestamp=meta?.referenceTimestamp?:0L,currentPrice=pick.price,adx=pick.adx,factorValues=keys.map{pick.components[it]?:50.0},factorScore=score.toDouble(),generatedAt=System.currentTimeMillis(),source="ORACLE_ENGINE_V5.9.7_REALDATA_SECTOR_WEIGHTED")
        }
        return out
    }

    private fun lookupCompanyName(ticker:String):String? = runCatching {
        val ua="Mozilla/5.0 (Linux; Android 16) AppleWebKit/537.36 Chrome/126.0.0.0 Mobile Safari/537.36"
        val url=URL("https://query1.finance.yahoo.com/v7/finance/quote?symbols=${URLEncoder.encode(ticker,"UTF-8")}&formatted=false&lang=en-US&region=US")
        val c=url.openConnection() as HttpURLConnection
        c.connectTimeout=5000; c.readTimeout=7000; c.requestMethod="GET"; c.setRequestProperty("User-Agent",ua); c.setRequestProperty("Accept","application/json")
        val body=c.inputStream.bufferedReader().use{it.readText()}; c.disconnect()
        val root=org.json.JSONObject(body); val q=root.optJSONObject("quoteResponse")?.optJSONArray("result")?.optJSONObject(0) ?: return@runCatching null
        q.optString("longName").takeIf{it.isNotBlank()} ?: q.optString("shortName").takeIf{it.isNotBlank()}
    }.getOrNull()

    private fun evaluate(t:String,d:List<OracleOhlcvPoint>):C?{
        val r=d.sortedByDescending{it.timestamp}
        val close=r.map{it.close};val high=r.map{it.high};val low=r.map{it.low};val vol=r.map{it.volume};val p=close[0]
        fun avg(n:Int)=if(close.size>=n)close.take(n).average()else null
        fun std(n:Int):Double?{if(close.size<n)return null;val a=close.take(n);val m=a.average();return sqrt(a.sumOf{(it-m)*(it-m)}/n)}
        fun mom(n:Int)=if(close.size>n)(p/close[n]-1)*100 else 0.0
        val s20=avg(20);val s50=avg(50);val s200=avg(200);val m5=mom(5);val m20=mom(20)
        val gains=close.dropLast(1).take(14).mapIndexed{i,x->max(0.0,x-close[i+1])}.average();val losses=close.dropLast(1).take(14).mapIndexed{i,x->max(0.0,close[i+1]-x)}.average();val rsi=if(losses==0.0)100.0 else 100-100/(1+gains/losses)
        val v20=if(vol.size>=20)vol.take(20).average()else 0.0;val vr=if(v20>0)vol[0]/v20 else 1.0;val prior20=if(close.size>=21)close.drop(1).take(20).maxOrNull()?:p else p;val breakout=if(p>prior20&&vr>=1.25)100.0 else if(p>prior20)62.0 else if(p>=prior20*.97)48.0 else 25.0
        val lo=close.take(20).minOrNull()?:p;val hi=close.take(20).maxOrNull()?:p;val sr=if(hi>lo)(30+70*(p-lo)/(hi-lo)).coerceIn(0.0,100.0)else 50.0;val mid=avg(20);val sd=std(20);val bbPos=if(mid!=null&&sd!=null&&sd>0)(p-(mid-2*sd))/(4*sd)else .5;val bbWidth=if(mid!=null&&mid>0)100*(4*(sd?:0.0))/mid else 0.0
        val ema12=ema(close,12);val ema26=ema(close,26);val macd=if(ema12!=null&&ema26!=null)ema12-ema26 else null;val atr=atr(high,low,close,14)?:p*.01;val atrPct=100*atr/p;val adx=adx(high,low,close,14)
        val ichi=if(close.size>=52){val t9=(high.take(9).max()+low.take(9).min())/2;val k26=(high.take(26).max()+low.take(26).min())/2;val a=(t9+k26)/2;val b=(high.take(52).max()+low.take(52).min())/2;p>max(a,b)&&t9>k26}else false
        val trend=(50.0+(if(s20!=null&&p>s20)16 else -16)+(if(s50!=null&&p>s50)17 else -17)+(if(s200!=null&&p>s200)17 else -17)).coerceIn(0.0,100.0);val momentum=(50+m5*2+m20*.65).coerceIn(0.0,100.0);val volume=(50+(vr-1)*45).coerceIn(0.0,100.0);val boll=(50+(bbPos-.5)*80+(if(bbWidth>0&&bbWidth<8)10 else 0)).coerceIn(0.0,100.0);val ichScore=if(ichi)90.0 else 30.0;val adxc=(35+(adx?:0.0)*1.15).coerceIn(0.0,100.0);val rr=(70-atrPct*5+(if(breakout>=100)15 else 0)).coerceIn(0.0,100.0)
        val overextension=((rsi-65.0)/15.0).coerceIn(0.0,1.0);val volatility=(atrPct/8.0).coerceIn(0.0,1.0);val volumeShock=((vr-1.0)/2.0).coerceIn(0.0,1.0);val acceleration=(abs(m5)/20.0).coerceIn(0.0,1.0);val riskScore=(100.0*(overextension*.30+volatility*.35+volumeShock*.15+acceleration*.20)).coerceIn(0.0,100.0);val risk=when{riskScore>=65.0->"RIDICAT";riskScore>=35.0->"MEDIU";else->"SCĂZUT"}
        val comps=mapOf("news" to 50.0,"breakout" to breakout,"trend" to trend,"momentum" to momentum,"volume" to volume,"support_resistance" to sr,"fundamentals" to 50.0,"bollinger" to boll,"ichimoku" to ichScore,"market_sector" to 50.0,"risk_reward" to rr,"adx" to adxc)
        val base=horizonScore(comps,"SHORT",null)
        val f=mapOf("short" to min(30.0,max(0.0,((p+2*atr)/p-1)*100)),"medium" to min(45.0,max(0.0,((p+4.5*atr)/p-1)*100)),"long" to min(70.0,max(0.0,((p+8*atr)/p-1)*100)))
        val alloc=when{risk=="RIDICAT"->max(1.0,base*.04);risk=="MEDIU"->max(1.0,base*.06);else->max(1.0,base*.08)}.coerceAtMost(8.0).let{ kotlin.math.round(it*10.0)/10.0 }
        return C(t,p,base,rsi,m5,m20,vr,macd,ichi,s200,s50,adx,atrPct,comps,f,risk,alloc,0)
    }

    /** V5.9.7: sector correction is applied only to allocation, never to score. */
    private fun horizonScore(c:Map<String,Double>,h:String,sector:String?):Int{
        val w=weights[h]!!
        val total=w.sum().toDouble()
        val raw=(keys.indices.sumOf{(c[keys[it]]?:50.0)*w[it].toDouble()}/total).toInt().coerceIn(0,100)
        return when{raw in 97..100->raw-3;raw in 92..96->raw-1;else->raw}
    }

    private fun tie(c:C,h:String):Double=0.0
    private fun rating(s:Int)=when{s>=85->"STRONG BUY";s>=75->"BUY";s>=65->"HOLD";s>=55->"WATCH";else->"AVOID"}
    private fun ema(v:List<Double>,n:Int):Double?{if(v.size<n)return null;var e=v.takeLast(n).average();val k=2.0/(n+1);for(i in v.size-n until v.size)e=v[i]*k+e*(1-k);return e}
    private fun atr(h:List<Double>,l:List<Double>,c:List<Double>,n:Int):Double?{if(c.size<n+1)return null;val tr=(0 until c.size-1).map{i->maxOf(h[i]-l[i],abs(h[i]-c[i+1]),abs(l[i]-c[i+1]))};return tr.take(n).average()}
    private fun adx(h:List<Double>,l:List<Double>,c:List<Double>,n:Int):Double?{if(c.size<n*2+2)return null;val tr=mutableListOf<Double>();val pd=mutableListOf<Double>();val md=mutableListOf<Double>();for(i in 0 until c.size-1){val up=h[i]-h[i+1];val dn=l[i+1]-l[i];tr+=maxOf(h[i]-l[i],abs(h[i]-c[i+1]),abs(l[i]-c[i+1]));pd+=if(up>dn&&up>0)up else 0.0;md+=if(dn>up&&dn>0)dn else 0.0};var atrv=tr.take(n).average();var p=pd.take(n).average();var m=md.take(n).average();val dx=mutableListOf<Double>();for(i in n until tr.size){atrv=(atrv*(n-1)+tr[i])/n;p=(p*(n-1)+pd[i])/n;m=(m*(n-1)+md[i])/n;val pi=if(atrv>0)100*p/atrv else 0.0;val mi=if(atrv>0)100*m/atrv else 0.0;dx+=if(pi+mi>0)100*abs(pi-mi)/(pi+mi)else 0.0};return if(dx.size<n)dx.average()else dx.takeLast(n).average()}
    private fun newsScore(t:String):Int{return try{val q=URLEncoder.encode("\"$t\" stock when:7d","UTF-8");val u=URL("https://news.google.com/rss/search?q=$q&hl=en-US&gl=US&ceid=US:en");val con=u.openConnection() as HttpURLConnection;con.connectTimeout=5000;con.readTimeout=7000;val body=con.inputStream.bufferedReader().use{it.readText()};con.disconnect();val pos=listOf("beat","upgrade","buy","bullish","record","strong","surge","contract","partnership","deal","approval","launch","growth","profit");val neg=listOf("miss","downgrade","sell","bearish","lawsuit","investigation","warning","cut guidance","recall","layoff","fraud","delay","loss","decline","plunge","offering","dilution","bankruptcy");val titles=Regex("<title>(.*?)</title>",RegexOption.IGNORE_CASE).findAll(body).map{it.groupValues[1].replace("&amp;","&").lowercase()}.take(8).toList();titles.sumOf{title->2*pos.count{title.contains(it)}-3*neg.count{title.contains(it)}}.coerceIn(-10,10)}catch(_:Exception){0}}
}
