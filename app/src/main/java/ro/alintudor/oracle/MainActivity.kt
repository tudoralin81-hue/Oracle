package ro.alintudor.oracle

import android.app.Activity
import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.webkit.WebViewClient.ERROR_HOST_LOOKUP
import android.widget.Toast

class MainActivity : Activity() {
    private lateinit var web: WebView
    private var currentModule: String? = null

    private data class Module(val key: String, val title: String, val url: String)

    private val modules = linkedMapOf(
        "portfolio" to Module("portfolio", "PORTFOLIO", "https://alintudor.ro/ai-stock-oracle-position-monitor/"),
        "alerts" to Module("alerts", "ALERTS", "https://alintudor.ro/ai-stock-oracle-sell-alerts/"),
        "news" to Module("news", "NEWS", "https://alintudor.ro/news/"),
        "growth" to Module("growth", "GROWTH", "https://alintudor.ro/oracle-v56-daily-action/"),
        "knowledge" to Module("knowledge", "KNOWLEDGE", "https://alintudor.ro/knowledge/"),
        "analysis" to Module("analysis", "ANALYSIS", "https://alintudor.ro/ai-stock-oracle-analysis/"),
        "watchlist" to Module("watchlist", "WATCHLIST", "https://alintudor.ro/ai-stock-oracle-position-monitor/")
    )

    private val hubHtml = """
        <!doctype html><html><head>
        <meta name="viewport" content="width=device-width, initial-scale=1, maximum-scale=1, user-scalable=no, viewport-fit=cover">
        <style>
          *{box-sizing:border-box;-webkit-tap-highlight-color:transparent}
          html,body{margin:0;width:100%;height:100%;background:#02040a;overflow:hidden}
          body{display:flex;align-items:center;justify-content:center}
          .hub{position:relative;width:min(100vw,100dvh);height:min(100vw,100dvh);max-width:2048px;max-height:2048px;line-height:0}
          .hub img{display:block;width:100%;height:100%;object-fit:contain;user-select:none;-webkit-user-drag:none}
          .hub:after{content:"";position:absolute;inset:5%;border-radius:50%;border:1px solid rgba(160,130,255,.12);box-shadow:0 0 35px rgba(140,110,255,.10),inset 0 0 35px rgba(140,110,255,.05);pointer-events:none;animation:orbitGlow 4s ease-in-out infinite}
          .node{position:absolute;display:block;z-index:5;border-radius:50%;touch-action:manipulation}
          .node:before{content:"";position:absolute;inset:-7%;border-radius:50%;border:1px solid rgba(145,120,255,.28);animation:pulse 2.8s ease-out infinite;pointer-events:none}
          .node:after{content:"";position:absolute;inset:10%;border-radius:50%;background:radial-gradient(circle,rgba(255,255,255,.08),transparent 62%);animation:nodeGlow 2.8s ease-in-out infinite;pointer-events:none}
          .portfolio{left:50%;top:8.6%;width:22%;height:22%;transform:translate(-50%,-50%)}
          .alerts{left:20.5%;top:22.9%;width:22%;height:22%;transform:translate(-50%,-50%)}
          .news{left:78.9%;top:22.9%;width:22%;height:22%;transform:translate(-50%,-50%)}
          .growth{left:10.7%;top:50%;width:22%;height:22%;transform:translate(-50%,-50%)}
          .knowledge{left:88.7%;top:50%;width:22%;height:22%;transform:translate(-50%,-50%)}
          .analysis{left:26.5%;top:81.4%;width:22%;height:22%;transform:translate(-50%,-50%)}
          .watchlist{left:73.5%;top:81.4%;width:22%;height:22%;transform:translate(-50%,-50%)}
          @keyframes pulse{0%{transform:scale(.88);opacity:.55}70%{transform:scale(1.16);opacity:0}100%{transform:scale(1.16);opacity:0}}
          @keyframes nodeGlow{0%,100%{opacity:.25}50%{opacity:.8}}
          @keyframes orbitGlow{0%,100%{opacity:.45;transform:scale(.985)}50%{opacity:1;transform:scale(1.01)}}
        </style></head><body>
        <div class="hub">
          <img src="https://alintudor.ro/wp-content/uploads/2026/08/file_000000003a28820ab17a0721b9d4dca7.png" alt="AI Stock Oracle Hub">
          <a class="node portfolio" href="app://module/portfolio"></a>
          <a class="node alerts" href="app://module/alerts"></a>
          <a class="node news" href="app://module/news"></a>
          <a class="node growth" href="app://module/growth"></a>
          <a class="node knowledge" href="app://module/knowledge"></a>
          <a class="node analysis" href="app://module/analysis"></a>
          <a class="node watchlist" href="app://module/watchlist"></a>
        </div></body></html>
    """.trimIndent()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = Color.rgb(2, 4, 10)
        window.navigationBarColor = Color.rgb(2, 4, 10)

        web = WebView(this)
        web.setBackgroundColor(Color.rgb(2, 4, 10))
        web.overScrollMode = View.OVER_SCROLL_NEVER
        web.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
                val uri = request.url.toString()
                if (uri.startsWith("app://module/")) {
                    openModule(uri.removePrefix("app://module/").substringBefore("?"))
                    return true
                }
                if (uri == "app://home") {
                    showHub()
                    return true
                }
                val moduleKey = moduleKeyForUrl(uri)
                if (moduleKey != null) {
                    openModule(moduleKey)
                    return true
                }
                return false
            }

            override fun onPageFinished(view: WebView, url: String) {
                super.onPageFinished(view, url)
                if (currentModule != null && url.startsWith("https://alintudor.ro/")) {
                    injectModuleShell(currentModule!!)
                }
            }

            override fun onReceivedError(view: WebView, request: WebResourceRequest, error: android.webkit.WebResourceError) {
                super.onReceivedError(view, request, error)
                if (request.isForMainFrame && error.errorCode == ERROR_HOST_LOOKUP) {
                    Toast.makeText(this@MainActivity, "Nu există conexiune la internet.", Toast.LENGTH_SHORT).show()
                }
            }
        }

        web.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            databaseEnabled = true
            cacheMode = WebSettings.LOAD_DEFAULT
            builtInZoomControls = false
            displayZoomControls = false
            useWideViewPort = true
            loadWithOverviewMode = false
            mediaPlaybackRequiresUserGesture = false
        }
        web.settings.userAgentString = web.settings.userAgentString + " AIStockOracleApp/2.0"
        setContentView(web)
        showHub()
    }

    private fun moduleKeyForUrl(url: String): String? {
        val clean = url.substringBefore("?").trimEnd('/')
        return modules.values.firstOrNull { it.url.trimEnd('/') == clean }?.key
    }

    private fun showHub() {
        currentModule = null
        web.loadDataWithBaseURL("https://alintudor.ro/", hubHtml, "text/html", "UTF-8", "https://alintudor.ro/")
    }

    private fun openModule(key: String) {
        val module = modules[key] ?: return
        currentModule = module.key
        web.clearHistory()
        web.loadUrl(module.url)
    }

    private fun injectModuleShell(key: String) {
        val title = modules[key]?.title ?: "ORACLE"
        val script = """
          (function(){
            try{
              var old=document.getElementById('oracle-app-shell'); if(old) old.remove();
              var style=document.getElementById('oracle-app-style');
              if(!style){
                style=document.createElement('style'); style.id='oracle-app-style';
                style.textContent=`html,body{background:#02040a!important;color:#eef2ff!important}body{padding-top:66px!important}.site-header,header.site-header,.site-footer,footer.site-footer,.entry-header,.wp-block-post-title,.entry-title{display:none!important}img{max-width:100%}`;
                document.head.appendChild(style);
              }
              var bar=document.createElement('div'); bar.id='oracle-app-shell';
              bar.innerHTML='<button id="oracle-app-home" aria-label="Acasă">⌂</button><div class="oracle-app-title">ORACLE <span>•</span> ${title}</div><button id="oracle-app-refresh" aria-label="Actualizează">↻</button>';
              bar.style.cssText='position:fixed;top:0;left:0;right:0;height:66px;z-index:2147483647;background:rgba(2,4,10,.97);backdrop-filter:blur(14px);display:flex;align-items:center;padding:0 14px;border-bottom:1px solid rgba(155,130,255,.18);box-shadow:0 4px 24px rgba(0,0,0,.4);font-family:Arial,sans-serif;';
              var h=bar.querySelector('#oracle-app-home'); h.style.cssText='width:44px;height:44px;border:0;border-radius:14px;background:#0b1020;color:#eef2ff;font-size:25px;line-height:44px;padding:0;';
              var r=bar.querySelector('#oracle-app-refresh'); r.style.cssText='width:44px;height:44px;border:0;border-radius:14px;background:#0b1020;color:#bbaeff;font-size:29px;line-height:44px;padding:0;';
              var t=bar.querySelector('.oracle-app-title'); t.style.cssText='flex:1;text-align:center;font-weight:800;font-size:18px;letter-spacing:.5px;color:#eef2ff;white-space:nowrap;';
              var s=bar.querySelector('.oracle-app-title span'); s.style.cssText='color:#8b6cff;margin:0 5px;';
              document.body.appendChild(bar);
              h.onclick=function(){window.location='app://home';};
              r.onclick=function(){location.reload();};
              document.documentElement.scrollTop=0;
            }catch(e){}
          })();
        """.trimIndent()
        web.evaluateJavascript("javascript:$script", null)
    }

    override fun onBackPressed() {
        if (currentModule != null) {
            showHub()
        } else {
            super.onBackPressed()
        }
    }

    override fun onDestroy() {
        web.stopLoading()
        web.destroy()
        super.onDestroy()
    }
}
