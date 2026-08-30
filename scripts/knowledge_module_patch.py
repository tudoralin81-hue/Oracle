from pathlib import Path

# Knowledge source is committed. This build step verifies the module and applies
# one small idempotent wiring change so the visible REFRESH button actually
# performs OracleKnowledgeSync.refreshAsync and redraws the module.
p = Path('app/src/main/java/ro/alintudor/oracle/MainActivity.kt')
s = p.read_text()
required = [
    'OracleKnowledgeSync',
    'OracleKnowledgeModule',
    '"knowledge"->OracleKnowledgeModule',
    'OracleKnowledgeSync.scheduleDaily(this)',
]
missing = [x for x in required if x not in s]
if missing:
    raise SystemExit('Knowledge source is incomplete: ' + ', '.join(missing))

old = '"knowledge"->OracleKnowledgeModule(host).render(OracleKnowledgeSync.load(this)) { url -> openKnowledgeUrl(url) }'
new = '''"knowledge"->OracleKnowledgeModule(host).render(
                OracleKnowledgeSync.load(this),
                { url -> openKnowledgeUrl(url) },
                {
                    OracleKnowledgeSync.refreshAsync(this) { ok, error ->
                        if (currentModule != "knowledge" || isFinishing) return@refreshAsync
                        if (ok) {
                            runCatching { renderModule("knowledge", false) }
                                .onFailure { showModuleError("knowledge", it) }
                        } else {
                            Toast.makeText(this, "Knowledge refresh eșuat: ${error ?: "eroare necunoscută"}", Toast.LENGTH_LONG).show()
                        }
                    }
                }
            )'''
if old in s:
    s = s.replace(old, new, 1)
p.write_text(s)

for path in [
    'app/src/main/java/ro/alintudor/oracle/core/OracleKnowledgeSync.kt',
    'app/src/main/java/ro/alintudor/oracle/nativeui/OracleKnowledgeModule.kt',
]:
    if not Path(path).exists():
        raise SystemExit('Missing Knowledge source file: ' + path)

print('Knowledge module patch verified/applied (idempotent)')
