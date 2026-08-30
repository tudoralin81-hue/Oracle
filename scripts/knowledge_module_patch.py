from pathlib import Path

p = Path('app/src/main/java/ro/alintudor/oracle/MainActivity.kt')
s = p.read_text()

# Add Knowledge sync import.
needle = 'import ro.alintudor.oracle.core.OracleWatchlistStore\n'
if 'import ro.alintudor.oracle.core.OracleKnowledgeSync' not in s:
    if needle not in s:
        raise SystemExit('Knowledge import anchor not found')
    s = s.replace(needle, needle + 'import ro.alintudor.oracle.core.OracleKnowledgeSync\n', 1)

# Schedule the daily refresh on every app start. AlarmManager keeps one receiver alarm.
needle = '        runCatching { OracleBootstrap.ensure(repository); showHub() }.onFailure { showFatalError("Pornirea Oracle a eșuat",it) }'
replacement = '        OracleKnowledgeSync.scheduleDaily(this)\n        runCatching { OracleBootstrap.ensure(repository); showHub() }.onFailure { showFatalError("Pornirea Oracle a eșuat",it) }'
if needle not in s:
    raise SystemExit('onCreate anchor not found')
s = s.replace(needle, replacement, 1)

# Knowledge is a web-backed module. Do not route it through the local market refresh pipeline.
needle = '''    private fun openModule(key:String){
        currentModule=key
        runCatching{renderModule(key,false)}.onFailure{showModuleError(key,it)}
        if (key == "analysis") return
        Thread{val result=runCatching{OracleLocalProcessor.refresh(repository)};mainHandler.post{if(currentModule!=key||isFinishing)return@post;result.onSuccess{runCatching{renderModule(key,false)}.onFailure{showModuleError(key,it)}}.onFailure{e->Toast.makeText(this,"Refresh local eșuat: ${e.message?:e.javaClass.simpleName}",Toast.LENGTH_LONG).show()}}}.start()
    }
'''
replacement = '''    private fun openModule(key:String){
        currentModule=key
        runCatching{renderModule(key,false)}.onFailure{showModuleError(key,it)}
        if (key == "analysis") return
        if (key == "knowledge") {
            if (OracleKnowledgeSync.isStale(this)) {
                OracleKnowledgeSync.refreshAsync(this) { ok, error ->
                    if (currentModule != "knowledge" || isFinishing) return@refreshAsync
                    if (ok) runCatching { renderModule("knowledge", false) }.onFailure { showModuleError("knowledge", it) }
                    else if (error != null) Toast.makeText(this, "Knowledge refresh eșuat: $error", Toast.LENGTH_LONG).show()
                }
            }
            return
        }
        Thread{val result=runCatching{OracleLocalProcessor.refresh(repository)};mainHandler.post{if(currentModule!=key||isFinishing)return@post;result.onSuccess{runCatching{renderModule(key,false)}.onFailure{showModuleError(key,it)}}.onFailure{e->Toast.makeText(this,"Refresh local eșuat: ${e.message?:e.javaClass.simpleName}",Toast.LENGTH_LONG).show()}}}.start()
    }
'''
if needle not in s:
    raise SystemExit('openModule block not found')
s = s.replace(needle, replacement, 1)

# Replace the old knowledge renderer with the dedicated web-backed module.
needle = '            "knowledge"->OracleSimpleModule(host,title).render(actions=data.actions,knowledge=data.knowledge,positions=data.positions,history=data.history)'
replacement = '            "knowledge"->OracleKnowledgeModule(host).render(OracleKnowledgeSync.load(this)) { url -> openKnowledgeUrl(url) }'
if needle not in s:
    raise SystemExit('knowledge render branch not found')
s = s.replace(needle, replacement, 1)

# Add browser navigation helper before showModuleError.
needle = '    private fun showModuleError(key:String,error:Throwable)'
helper = '''    private fun openKnowledgeUrl(url:String){
        if (url.isBlank()) return
        runCatching {
            startActivity(android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse(url)))
        }.onFailure { Toast.makeText(this, "Nu se poate deschide articolul", Toast.LENGTH_SHORT).show() }
    }

    private fun showModuleError(key:String,error:Throwable)'''
if needle not in s:
    raise SystemExit('showModuleError anchor not found')
s = s.replace(needle, helper, 1)

p.write_text(s)
print('Knowledge module patch applied')
