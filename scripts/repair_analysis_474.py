from pathlib import Path

# REPAIR_ANALYSIS_474_V1
# Repairs source-of-truth data/display only; does not alter Oracle weights.

ui = Path('app/src/main/java/ro/alintudor/oracle/nativeui/OracleAnalysisModules.kt')
s = ui.read_text(encoding='utf-8')
s = s.replace(
    'if (i == 0) null else name to (r.rawValues.getOrNull(i) ?: "Valoare indisponibilă")',
    'name to (r.rawValues.getOrNull(i) ?: "Valoare indisponibilă")',
    1,
)
ui.write_text(s, encoding='utf-8')

p = Path('app/src/main/java/ro/alintudor/oracle/core/OracleRealData.kt')
s = p.read_text(encoding='utf-8')

s = s.replace(
    '"quarterlyOrdinarySharesNumber","annualOrdinarySharesNumber",',
    '"quarterlyOrdinarySharesNumber","annualOrdinarySharesNumber",\n            "quarterlyCurrentAssets","quarterlyCurrentLiabilities","quarterlyInventory","quarterlyStockholdersEquity",',
    1,
)

old = '''        val debt=latest(root,"annualTotalDebt")
        val equity=latest(root,"annualStockholdersEquity")
        val ttmRevenue=latest(root,"trailingTotalRevenue") ?: revenue.first
        val ttmIncome=latest(root,"trailingNetIncome") ?: income.first
        val ttmOperating=latest(root,"trailingOperatingIncome") ?: operating.first
        val rg=if(revenue.first!=null && revenue.second!=null && revenue.second!=0.0) revenue.first!!/revenue.second!!-1.0 else null
        val eg=if(income.first!=null && income.second!=null && income.second!=0.0) income.first!!/income.second!!-1.0 else null
        val pm=if(ttmRevenue!=null && ttmRevenue!=0.0 && ttmIncome!=null) ttmIncome/ttmRevenue else null
        val om=if(ttmRevenue!=null && ttmRevenue!=0.0 && ttmOperating!=null) ttmOperating/ttmRevenue else null
        val roe=if(equity!=null && equity!=0.0 && ttmIncome!=null) ttmIncome/equity else null
        val de=if(equity!=null && equity!=0.0 && debt!=null) debt/equity else null
        val shares=latest(root,"quarterlyOrdinarySharesNumber") ?: latest(root,"annualOrdinarySharesNumber")
        val price=runCatching { OracleMarketData.fetchDaily(ticker,"5d").maxByOrNull{it.timestamp}?.close }.getOrNull()
        val marketCap=if(shares!=null && price!=null && shares>0.0 && price>0.0) shares*price else null
        val sector=resolvedSector(ticker)
        val industry=knownIndustry(ticker)
        return OracleFundamentals(sector,industry,null,null,rg,eg,pm,om,roe,de,marketCap,null,null,null,null,"")'''
new = '''        val debt=latest(root,"annualTotalDebt") ?: latest(root,"quarterlyTotalDebt")
        val equity=latest(root,"quarterlyStockholdersEquity") ?: latest(root,"annualStockholdersEquity")
        val currentAssets=latest(root,"quarterlyCurrentAssets")
        val currentLiabilities=latest(root,"quarterlyCurrentLiabilities")
        val inventory=latest(root,"quarterlyInventory") ?: 0.0
        val ttmRevenue=latest(root,"trailingTotalRevenue") ?: revenue.first
        val ttmIncome=latest(root,"trailingNetIncome") ?: income.first
        val ttmOperating=latest(root,"trailingOperatingIncome") ?: operating.first
        val ttmEps=latest(root,"trailingDilutedEPS")
        val rg=if(revenue.first!=null && revenue.second!=null && revenue.second!=0.0) revenue.first!!/revenue.second!!-1.0 else null
        val eg=if(income.first!=null && income.second!=null && income.second!! > 0.0 && income.first!! > 0.0) income.first!!/income.second!!-1.0 else null
        val pm=if(ttmRevenue!=null && ttmRevenue!=0.0 && ttmIncome!=null) ttmIncome/ttmRevenue else null
        val om=if(ttmRevenue!=null && ttmRevenue!=0.0 && ttmOperating!=null) ttmOperating/ttmRevenue else null
        val roe=if(equity!=null && equity!=0.0 && ttmIncome!=null) ttmIncome/equity else null
        val de=if(equity!=null && equity!=0.0 && debt!=null) debt/equity else null
        val shares=latest(root,"quarterlyOrdinarySharesNumber") ?: latest(root,"annualOrdinarySharesNumber")
        val price=runCatching { OracleMarketData.fetchDaily(ticker,"5d").maxByOrNull{it.timestamp}?.close }.getOrNull()
        val marketCap=if(shares!=null && price!=null && shares>0.0 && price>0.0) shares*price else null
        val pe=if(price!=null && ttmEps!=null && ttmEps>0.0) price/ttmEps else null
        val pb=if(marketCap!=null && equity!=null && equity>0.0) marketCap/equity else null
        val cr=if(currentAssets!=null && currentLiabilities!=null && currentLiabilities>0.0) currentAssets/currentLiabilities else null
        val qr=if(currentAssets!=null && currentLiabilities!=null && currentLiabilities>0.0) (currentAssets-inventory)/currentLiabilities else null
        val beta=computedBeta(ticker)
        val sector=resolvedSector(ticker)
        val industry=knownIndustry(ticker)
        return OracleFundamentals(sector,industry,pe,null,rg,eg,pm,om,roe,de,marketCap,pb,cr,qr,beta,"")'''
if old in s:
    s = s.replace(old, new, 1)

marker = '    private fun latestTwo(root:JSONObject,key:String):Pair<Double?,Double?> {'
if 'private fun computedBeta' not in s and marker in s:
    helper = '''    private fun computedBeta(ticker:String):Double? {
        return runCatching {
            val a=OracleMarketData.fetchDaily(ticker,"1y").sortedByDescending{it.timestamp}.map{it.close}
            val b=OracleMarketData.fetchDaily("SPY","1y").sortedByDescending{it.timestamp}.map{it.close}
            val n=minOf(a.size,b.size)-1
            if(n<30) return@runCatching null
            val ar=(0 until n).map{i->a[i]/a[i+1]-1.0}
            val br=(0 until n).map{i->b[i]/b[i+1]-1.0}
            val am=ar.average(); val bm=br.average()
            val cov=ar.indices.sumOf{i->(ar[i]-am)*(br[i]-bm)}/n
            val vari=br.sumOf{(it-bm)*(it-bm)}/n
            if(vari>0.0) cov/vari else null
        }.getOrNull()
    }

'''
    s = s.replace(marker, helper + marker, 1)

s = s.replace(
    '"LIN","APD","APLD","SHW","FCX","NEM","NUE","DOW","DD","ALB"->"Materials"',
    '"LIN","APD","SHW","FCX","NEM","NUE","DOW","DD","ALB"->"Materials"',
    1,
)
p.write_text(s, encoding='utf-8')

print('Analysis 474 repair applied')
