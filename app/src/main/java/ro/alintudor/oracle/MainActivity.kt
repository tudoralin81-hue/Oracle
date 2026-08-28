package ro.alintudor.oracle

import android.app.Activity
import android.os.Bundle
import android.graphics.Color
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

    private val modules = mapOf(
        "portfolio" to Module("portfolio", "PORTFOLIO", "https://alintudor.ro/ai-stock-oracle-position-monitor/"),
        "alerts" to Module("alerts", "ALERTS", "https://alintudor.ro/ai-stock-oracle-sell-alerts/"),
        "news" to Module("news", "NEWS", "https://alintudor.ro/?s=stock"),
        "growth" to Module("growth", "GROWTH", "https://alintudor.ro/ai-stock-oracle-v56-daily-action/"),
        "knowledge" to Module("knowledge", "KNOWLEDGE", "https://alintudor.ro/knowledge/"),
        "analysis" to Module("analysis", "ANALYSIS", "https://alintudor.ro/ai-stock-oracle-analysis/"),
        "watchlist" to Module("watchlist", "WATCHLIST", "https://alintudor.ro/ai-stock-oracle-position-monitor/")
    )

    private val hubHtml = """
        <!doctype html>
        <html><head>
          <meta name="viewport" content="width=device-width, initial-scale=1, maximum-scale=1, user-scalable=no, viewport-fit=cover">
          <style>
            *{box-sizing:border-box;-webkit-tap-highlight-color:transparent}
            html,body{margin:0;width:100%;height:100%;background:#02040a;overflow:hidden}
            body{display:flex;align-items:center;justify-content:center}
            .hub{position:relative;width:100vw;height:100vw;max-width:100vh;max-height:100vh;line-height:0}
            .hub img{display:block;width:100%;height:100%;object-fit:contain;user-select:none;-webkit-user-drag:none}
            .hub:after{content:"";position:absolute;inset:5%;border-radius:50%;border:1px solid rgba(160,130,255,.10);box-shadow:0 0 35px rgba(140,110,255,.08),inset 0 0 35px rgba(140,110,255,.04);pointer-events:none;animation:orbitGlow 4s ease-in-out infinite}
            .node{position:absolute;display:block;z-index:5;border-radius:50%;touch-action:manipulation}
            .node:before{content:"";position:absolute;inset:-7%;border-radius:50%;border:1px solid rgba(145,120,255,.25);box-shadow:0 0 0 rgba(145,120,255,0);animation:pulse 2.8s ease-out infinite;pointer-events:none}
            .node:after{content:"";position:absolute;inset:10%;border-radius:50%;background:radial-gradient(circle,rgba(255,255,255,.06),transparent 62%);animation:nodeGlow 2.8s ease-in-out infinite;pointer-events:none}
            .portfolio{left:50%;top:8.6%;width:22%;height:22%;transform:translate(-50%,-50%)}
            .alerts{left:20.5%;top:22.9%;width:22%;height:22%;transform:translate(-50%,-50%)}
            .news{left:78.9%;top:22.9%;width:22%;height:22%;transform:translate(-50%,-50%)}
            .growth{left:10.7%;top:50%;width:22%;height:22%;transform:translate(-50%,-50%)}
            .knowledge{left:88.7%;top:50%;width:22%;height:22%;transform:translate(-50%,-50%)}
            .analysis{left:26.5%;top:81.4%;width:22%;height:22%;transform:translate(-50%,-50%)}
            .watchlist{left:73.5%;top:81.4%;width:22%;height:22%;transform:translate(-50%,-50%)}
            @keyframes pulse{0%{transform:scale(.88);opacity:.55}70%{transform:scale(1.16);opacity:0}100%{transform:scale(1.16);opacity:0}}
            @keyframes nodeGlow{0%,100%{opacity:.25}50%{opacity:.7}}
            @keyframes orbitGlow{0%,100%{opacity:.45;transform:scale(.985)}50%{opacity:1;transform:scale(1.01)}}
          </style>
        </head><body>
          <div class="hub">
            <img src="https://alintudor.ro/wp-content/uploads/2026/08/file_000000003a28820ab17a0721b9d4dca7.png" alt="AI Stock Oracle Hub">
            <a class="node portfolio" href="app://module/portfolio" aria-label="Portfolio"></a>
            <a class="node alerts" href="app://module/alerts" aria-label="Alerts"></a>
            <a class="node news" href="app://module/news" aria-label="News"></a>
            <a class="node growth" href="app://module/growth" aria-label="Growth"></a>
            <a class="node knowledge" href="app://module/knowledge" aria-label="Knowledge"></a>
            <a class="node analysis" href="app://module/analysis" aria-label="Analysis"></a>
            <a class="node watchlist" href="app://module/watchlist" aria-label="Watchlist"></a>
          </div>
        </body></html>
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
                    val key = uri.removePrefix("app://module/").substringBefore("?")
                    openModule(key)
                    return true
                }
                if (uri == "app://back") {
                    showHub()
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
        web.settings.userAgentString = web.settings.userAgentString + " AIStockOracleApp/1.3"
        setContentView(web)
        showHub()
    }

    private fun showHub() {
        currentModule = null
        web.loadDataWithBaseURL("https://alintudor.ro/", hubHtml, "text/html", "UTF-8", "https://alintudor.ro/")
    }

    private fun openModule(key: String) {
        val module = modules[key] ?: return
        currentModule = module.key
        web.loadUrl(module.url)
    }

    private fun injectModuleShell(key: String) {
        val title = modules[key]?.title ?: "ORACLE"
        val script = """
            (function(){
              try {
                var old=document.getElementById('oracle-app-shell');
                if(old) old.remove();
                var style=document.getElementById('oracle-app-style');
                if(!style){
                  style=document.createElement('style');style.id='oracle-app-style';
                  style.textContent=`html,body{background:#02040a!important;color:#eef2ff!important}body{padding-top:58px!important}.site-header,header.site-header,.site-footer,footer.site-footer,.entry-header,.wp-block-post-title,.entry-title{display:none!important}`;
                  document.head.appendChild(style);
                }
                var bar=document.createElement('div');bar.id='oracle-app-shell';
                bar.innerHTML='<button id="oracle-app-back" aria-label="Înapoi">‹</button><div class="oracle-app-title">ORACLE <span>•</span> ${title}</div><div class="oracle-app-spacer"></div>';
                bar.style.cssText='position:fixed;top:0;left:0;right:0;height:58px;z-index:2147483647;background:rgba(2,4,10,.96);backdrop-filter:blur(12px);display:flex;align-items:center;padding:0 14px;border-bottom:1px solid rgba(155,130,255,.18);box-shadow:0 4px 24px rgba(0,0,0,.35);font-family:Arial,sans-serif;';
                var b=bar.querySelector('#oracle-app-back');b.style.cssText='width:42px;height:42px;border:0;border-radius:14px;background:#0b1020;color:#eef2ff;font-size:34px;line-height:36px;padding:0;';
                var t=bar.querySelector('.oracle-app-title');t.style.cssText='margin-left:12px;font-weight:800;font-size:17px;letter-spacing:.4px;color:#eef2ff;white-space:nowrap;';
                var s=bar.querySelector('.oracle-app-title span');s.style.cssText='color:#8b6cff;margin:0 5px;';
                document.body.appendChild(bar);
                b.onclick=function(){window.location='app://back';};
                document.documentElement.scrollTop=0;
              }catch(e){}
            })();
        """.trimIndent()
        web.evaluateJavascript("javascript:$script", null)
    }

    override fun onBackPressed() {
        if (currentModule != null) {
            if (web.canGoBack()) {
                web.goBack()
            } else {
                showHub()
            }
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
