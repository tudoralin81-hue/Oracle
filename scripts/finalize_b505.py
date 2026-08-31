from pathlib import Path
import re

gradle = Path('app/build.gradle')
s = gradle.read_text(encoding='utf-8')
s = re.sub(r'versionCode\s+\d+', 'versionCode 21', s, count=1)
s = re.sub(r"versionName\s+'[^']+'", "versionName 'V6g-FINAL-B505'", s, count=1)
gradle.write_text(s, encoding='utf-8')

header = Path('app/src/main/java/ro/alintudor/oracle/nativeui/OracleNativeModule.kt')
s = header.read_text(encoding='utf-8')
s = re.sub(r'text=\"BUILD \d+\"', 'text="BUILD 505 • FINAL"', s, count=1)
header.write_text(s, encoding='utf-8')

analysis = Path('app/src/main/java/ro/alintudor/oracle/nativeui/OracleAnalysisModules.kt')
s = analysis.read_text(encoding='utf-8')
s = s.replace('if (i == 0) null else if (i == 0) null else name to (r.rawValues.getOrNull(i) ?: "Valoare indisponibilă")', 'name to (r.rawValues.getOrNull(i) ?: "Valoare indisponibilă")', 1)
s = s.replace('if (i == 0) null else name to (r.rawValues.getOrNull(i) ?: "Valoare indisponibilă")', 'name to (r.rawValues.getOrNull(i) ?: "Valoare indisponibilă")', 1)
analysis.write_text(s, encoding='utf-8')

real = Path('app/src/main/java/ro/alintudor/oracle/core/OracleRealData.kt')
s = real.read_text(encoding='utf-8')
s = s.replace('val rg=summary?.revenueGrowth ?: ts?.revenueGrowth', 'val rg=ts?.revenueGrowth ?: summary?.revenueGrowth', 1)
s = s.replace('val cap=ts?.marketCap ?: summary?.marketCap ?: quote?.marketCap', 'val cap=summary?.marketCap ?: quote?.marketCap ?: ts?.marketCap', 1)
s = s.replace('"LIN","APD","APLD","SHW","FCX","NEM","NUE","DOW","DD","ALB"->"Materials"', '"LIN","APD","SHW","FCX","NEM","NUE","DOW","DD","ALB"->"Materials"', 1)
real.write_text(s, encoding='utf-8')

print('Oracle B505 finalization applied')
