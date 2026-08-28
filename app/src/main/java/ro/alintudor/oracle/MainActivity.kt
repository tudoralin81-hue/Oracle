package ro.alintudor.oracle

import android.app.Activity
import android.os.Bundle
import android.graphics.Color
import android.view.View
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient

class MainActivity : Activity() {
    private lateinit var web: WebView

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
            a{position:absolute;display:block;z-index:5;border-radius:50%;touch-action:manipulation}
            .portfolio{left:50%;top:8.6%;width:22%;height:22%;transform:translate(-50%,-50%)}
            .alerts{left:20.5%;top:22.9%;width:22%;height:22%;transform:translate(-50%,-50%)}
            .news{left:78.9%;top:22.9%;width:22%;height:22%;transform:translate(-50%,-50%)}
            .growth{left:10.7%;top:50%;width:22%;height:22%;transform:translate(-50%,-50%)}
            .knowledge{left:88.7%;top:50%;width:22%;height:22%;transform:translate(-50%,-50%)}
            .analysis{left:26.5%;top:81.4%;width:22%;height:22%;transform:translate(-50%,-50%)}
            .watchlist{left:73.5%;top:81.4%;width:22%;height:22%;transform:translate(-50%,-50%)}
          </style>
        </head><body>
          <div class="hub">
            <img src="https://alintudor.ro/wp-content/uploads/2026/08/file_000000003a28820ab17a0721b9d4dca7.png" alt="AI Stock Oracle Hub">
            <a class="portfolio" href="https://alintudor.ro/ai-stock-oracle-position-monitor/" aria-label="Portfolio"></a>
            <a class="alerts" href="https://alintudor.ro/ai-stock-oracle-sell-alerts/" aria-label="Alerts"></a>
            <a class="news" href="https://alintudor.ro/blog/" aria-label="News"></a>
            <a class="growth" href="https://alintudor.ro/ai-stock-oracle-v56-daily-action/" aria-label="Growth"></a>
            <a class="knowledge" href="https://alintudor.ro/" aria-label="Knowledge"></a>
            <a class="analysis" href="https://alintudor.ro/ai-stock-oracle-analysis/" aria-label="Analysis"></a>
            <a class="watchlist" href="https://alintudor.ro/ai-stock-oracle-position-monitor/" aria-label="Watchlist"></a>
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
            override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean = false
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
        }
        setContentView(web)
        web.loadDataWithBaseURL("https://alintudor.ro/", hubHtml, "text/html", "UTF-8", "https://alintudor.ro/")
    }

    @Deprecated("Deprecated in Android 13; retained for compatibility with the current minSdk")
    override fun onBackPressed() {
        if (web.canGoBack()) web.goBack() else super.onBackPressed()
    }

    override fun onDestroy() {
        web.stopLoading()
        web.destroy()
        super.onDestroy()
    }
}
