from pathlib import Path

path = Path('app/src/main/java/ro/alintudor/oracle/nativeui/OracleAnalysisModules.kt')
s = path.read_text(encoding='utf-8')
old = 'text = "${companyName(r.ticker)}   •   Sector: ${sector(r.ticker)}"'
new = 'text = "${companyName(r.ticker)}   •   Sector: ${OracleRealData.resolvedSector(r.ticker) ?: "Sector indisponibil"}"'
if old in s:
    s = s.replace(old, new, 1)
old_fn = '''    private fun sector(t: String) = when (t) {\n        "AAOI" -> "Technology / Optical Networking"\n        "NVDA", "AMD", "AVGO" -> "Semiconductors"; "AAPL", "MSFT", "GOOGL", "META", "AMZN", "NFLX" -> "Technology / Internet"; "TSLA" -> "Automotive / Energy"; else -> "Sector indisponibil"\n    }\n'''
s = s.replace(old_fn, '')
path.write_text(s, encoding='utf-8')
print('Analysis sector source patch applied')
