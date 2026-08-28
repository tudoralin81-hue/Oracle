package ro.alintudor.oracle

import android.app.Activity
import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast

/** AI Stock Oracle app shell: seven app-owned module containers. */
class MainActivity : Activity() {
    private lateinit var web: WebView
    private var currentModule: String? = null
    private data class Module(val key: String, val title: String, val url: String)

    private val modules = linkedMapOf(
        "portfolio" to Module("portfolio", "PORTFOLIO", "https://alintudor.ro/ai-stock-oracle-position-monitor/"),
        "alerts" to Module("alerts", "ALERTS", "https://alintudor.ro/ai-stock-oracle-sell-alerts/"),
        "news" to Module("news", "NEWS", "https://alintudor.ro/blog/"),
        "growth" to Module("growth", "GROWTH", "https://alintudor.ro/oracle-v56-daily-action/"),
        "knowledge" to Module("knowledge", "KNOWLEDGE", "https://alintudor.ro/knowledge/"),
        "analysis" to Module("analysis", "ANALYSIS", "https://alintudor.ro/ai-stock-oracle-analysis/"),
        "watchlist" to Module("watchlist", "WATCHLIST", "https://alintudor.ro/ai-stock-oracle-position-monitor/")
    )

    private val hubHtml = """
      <!doctype html><html><head><meta name="viewport" content="width=device-width,initial-scale=1,maximum-scale=1,user-scalable=no,viewport-fit=cover">
      <style>*{box-sizing:border-box;-webkit-tap-highlight-color:transparent}html,body{margin:0;width:100%;height:100%;background:#02040a;overflow:hidden}body{display:flex;align-items:center;justify-content:center}.hub{position:relative;width:min(100vw,100dvh);height:min(100vw,100dvh);max-width:2048px;max-height:2048px;line-height:0}.hub img{display:block;width:100%;height:100%;object-fit:contain;user-select:none;-webkit-user-drag:none}.hub:after{content:"";position:absolute;inset:5%;border-radius:50%;border:1px solid rgba(160,130,255,.12);box-shadow:0 0 35px rgba(140,110,255,.10),inset 0 0 35px rgba(140,110,255,.05);pointer-events:none;animation:orbitGlow 4s ease-in-out infinite}.node{position:absolute;display:block;z-index:5;border-radius:50%;touch-action:manipulation}.node:before{content:"";position:absolute;inset:-7%;border-radius:50%;border:1px solid rgba(145,120,255,.28);animation:pulse 2.8s ease-out infinite;pointer-events:none}.node:after{content:"";position:absolute;inset:10%;border-radius:50%;background:radial-gradient(circle,rgba(255,255,255,.08),transparent 62%);animation:nodeGlow 2.8s ease-in-out infinite;pointer-events:none}.portfolio{left:50%;top:8.6%;width:22%;height:22%;transform:translate(-50%,-50%)}.alerts{left:20.5%;top:22.9%;width:22%;height:22%;transform:translate(-50%,-50%)}.news{left:78.9%;top:22.9%;width:22%;height:22%;transform:translate(-50%,-50%)}.growth{left:10.7%;top:50%;width:22%;height:22%;transform:translate(-50%,-50%)}.knowledge{left:88.7%;top:50%;width:22%;height:22%;transform:translate(-50%,-50%)}.analysis{left:26.5%;top:81.4%;width:22%;height:22%;transform:translate(-50%,-50%)}.watchlist{left:73.5%;top:81.4%;width:22%;height:22%;transform:translate(-50%,-50%)}@keyframes pulse{0%{transform:scale(.88);opacity:.55}70%{transform:scale(1.16);opacity:0}100%{transform:scale(1.16);opacity:0}}@keyframes nodeGlow{0%,100%{opacity:.25}50%{opacity:.8}}@keyframes orbitGlow{0%,100%{opacity:.45;transform:scale(.985)}50%{opacity:1;transform:scale(1.01)}}</style></head><body><div class="hub">
      <img src="https://alintudor.ro/wp-content/uploads/2026/08/file_000000003a28820ab17a0721b9d4dca7.png" alt="AI Stock Oracle Hub">
      <a class="node portfolio" href="app://module/portfolio"></a><a class="node alerts" href="app://module/alerts"></a><a class="node news" href="app://module/news"></a><a class="node growth" href="app://module/growth"></a><a class="node knowledge" href="app://module/knowledge"></a><a class="node analysis" href="app://module/analysis"></a><a class="node watchlist" href="app://module/watchlist"></a>
      </div></body></html>
    """.trimIndent()

    private fun moduleHtml(module: Module): String = """
      <!doctype html><html><head><meta name="viewport" content="width=device-width,initial-scale=1,maximum-scale=1,user-scalable=no,viewport-fit=cover">
      <style>*{box-sizing:border-box;-webkit-tap-highlight-color:transparent}html,body{margin:0;width:100%;height:100%;background:#02040a;color:#eef2ff;font-family:Arial,sans-serif;overflow:hidden}.bar{height:64px;display:flex;align-items:center;gap:10px;padding:0 12px;background:rgba(7,10,20,.98);border-bottom:1px solid rgba(155,130,255,.18);box-shadow:0 4px 24px rgba(0,0,0,.45);position:relative;z-index:20}button{border:0;border-radius:14px;background:#0d1323;color:#eef2ff;width:44px;height:44px;font-size:25px}.title{flex:1;text-align:center;font-size:17px;font-weight:800;letter-spacing:.6px}.dot{color:#8d72ff;margin:0 5px}.body{position:absolute;left:0;right:0;top:64px;bottom:0;overflow:hidden}iframe{border:0;width:100%;height:100%;background:#02040a}.loading{position:absolute;inset:0;display:flex;align-items:center;justify-content:center;background:#02040a;z-index:10;transition:opacity .35s}.spinner{width:42px;height:42px;border:3px solid rgba(255,255,255,.15);border-top-color:#8d72ff;border-radius:50%;animation:spin .9s linear infinite}@keyframes spin{to{transform:rotate(360deg)}}</style></head><body>
      <div class="bar"><button onclick="location.href='app://home'" aria-label="Acasă">⌂</button><div class="title">ORACLE <span class="dot">•</span> ${module.title}</div><button onclick="document.getElementById('frame').contentWindow.location.reload()" aria-label="Actualizează">↻</button></div>
      <div class="body"><div class="loading" id="loading"><div class="spinner"></div></div><iframe id="frame" src="${module.url}" allow="fullscreen; autoplay"></iframe></div>
      <script>document.getElementById('frame').addEventListener('load',function(){setTimeout(function(){document.getElementById('loading').style.opacity='0';setTimeout(function(){document.getElementById('loading').style.display='none'},350)},120)});</script></body></html>
    """.trimIndent()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = Color.rgb(2, 4, 10)
        window.navigationBarColor = Color.rgb(2, 4, 10)
        web = WebView(this)
        web.setBackgroundColor(Color.rgb(2, 4, 10))
        web.overScrollMode = View.OVER_SCROLL_NEVER
        web.settings.apply { javaScriptEnabled = true; domStorageEnabled = true; databaseEnabled = true; cacheMode = WebSettings.LOAD_DEFAULT; builtInZoomControls = false; displayZoomControls = false; useWideViewPort = true; loadWithOverviewMode = false; mediaPlaybackRequiresUserGesture = false }
        web.settings.userAgentString += " AIStockOracleApp/2.1"
        web.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
                val uri = request.url.toString()
                if (uri == "app://home") { showHub(); return true }
                if (uri.startsWith("app://module/")) { openModule(uri.removePrefix("app://module/").substringBefore("?")); return true }
                return false
            }
            override fun onReceivedError(view: WebView, request: WebResourceRequest, error: android.webkit.WebResourceError) { if (request.isForMainFrame) Toast.makeText(this@MainActivity, "Nu există conexiune la internet.", Toast.LENGTH_SHORT).show() }
        }
        setContentView(web)
        showHub()
    }

    private fun showHub() { currentModule = null; web.loadDataWithBaseURL("https://alintudor.ro/", hubHtml, "text/html", "UTF-8", "https://alintudor.ro/") }
    private fun openModule(key: String) { val module = modules[key] ?: return; currentModule = key; web.loadDataWithBaseURL("https://alintudor.ro/", moduleHtml(module), "text/html", "UTF-8", "https://alintudor.ro/") }
    override fun onBackPressed() { if (currentModule != null) showHub() else super.onBackPressed() }
    override fun onDestroy() { web.stopLoading(); web.destroy(); super.onDestroy() }
}
