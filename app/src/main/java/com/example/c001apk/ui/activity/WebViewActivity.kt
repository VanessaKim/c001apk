package com.example.c001apk.ui.activity

import android.annotation.SuppressLint
import android.content.ActivityNotFoundException
import android.content.Intent
import android.content.res.Configuration
import android.net.Uri
import android.os.Build.VERSION.SDK_INT
import android.os.Bundle
import android.util.Log
import android.view.KeyEvent
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.webkit.CookieManager
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.webkit.WebSettingsCompat
import androidx.webkit.WebViewFeature
import com.example.c001apk.R
import com.example.c001apk.databinding.ActivityWebViewBinding
import com.example.c001apk.util.ClipboardUtil.copyText
import com.example.c001apk.util.PrefManager
import com.google.android.material.snackbar.Snackbar
import java.net.URISyntaxException


class WebViewActivity : BaseActivity() {

    private lateinit var binding: ActivityWebViewBinding
    private lateinit var link: String

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityWebViewBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolBar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        link = intent.getStringExtra("url")!!
        loadUrlInWebView()

        if (SDK_INT >= 32) {
            if (WebViewFeature.isFeatureSupported(WebViewFeature.ALGORITHMIC_DARKENING)) {
                try {
                    WebSettingsCompat.setAlgorithmicDarkeningAllowed(
                        binding.webView.settings,
                        true
                    )
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        } else {
            if (WebViewFeature.isFeatureSupported(WebViewFeature.FORCE_DARK)) {
                val nightModeFlags =
                    resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK
                if (nightModeFlags == Configuration.UI_MODE_NIGHT_YES) {
                    WebSettingsCompat.setForceDark(
                        binding.webView.settings,
                        WebSettingsCompat.FORCE_DARK_ON
                    )
                }
            }
        }
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun loadUrlInWebView() {
        binding.webView.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            setSupportZoom(true)
            builtInZoomControls = true
            cacheMode = WebSettings.LOAD_NO_CACHE
            defaultTextEncodingName = "UTF-8"
            allowContentAccess = true
            useWideViewPort = true
            loadWithOverviewMode = true
            javaScriptCanOpenWindowsAutomatically = true
            loadsImagesAutomatically = true
            allowFileAccess = false
            //isAlgorithmicDarkeningAllowed = true
            //WebView.setWebContentsDebuggingEnabled(true)
            userAgentString = PrefManager.USER_AGENT
        }
        CookieManager.getInstance().apply {
            setAcceptThirdPartyCookies(binding.webView, false)
            removeAllCookies { }
            setCookie("m.coolapk.com", "DID=${PrefManager.SZLMID}")
            setCookie("m.coolapk.com", "forward=https://www.coolapk.com")
            setCookie("m.coolapk.com", "displayVersion=v14")
            setCookie("m.coolapk.com", "uid=${PrefManager.uid}")
            setCookie("m.coolapk.com", "username=${PrefManager.username}")
            setCookie("m.coolapk.com", "token=${PrefManager.token}")
        }
        binding.webView.apply {
            webViewClient = object : WebViewClient() {
                override fun shouldOverrideUrlLoading(
                    view: WebView?, request: WebResourceRequest?
                ): Boolean {
                    request?.let {
                        try {
                            //处理intent协议
                            if (request.url.toString().startsWith("intent://")) {
                                val intent: Intent
                                try {
                                    intent = Intent.parseUri(
                                        request.url.toString(), Intent.URI_INTENT_SCHEME
                                    )
                                    intent.addCategory("android.intent.category.BROWSABLE")
                                    intent.component = null
                                    intent.selector = null
                                    val resolves =
                                        context.packageManager.queryIntentActivities(intent, 0)
                                    if (resolves.size > 0) {
                                        startActivityIfNeeded(intent, -1)
                                    }
                                    return true
                                } catch (e: URISyntaxException) {
                                    e.printStackTrace()
                                }
                            }
                            // 处理自定义scheme协议
                            if (!request.url.toString().startsWith("http")) {
                                Snackbar.make(
                                    view!!,
                                    "当前网页将要打开外部链接，是否打开",
                                    Snackbar.LENGTH_SHORT
                                ).setAction("打开") {
                                    try {
                                        val intent = Intent(
                                            Intent.ACTION_VIEW,
                                            Uri.parse(request.url.toString())
                                        )
                                        intent.flags =
                                            (Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
                                        startActivity(intent)
                                    } catch (e: Exception) {
                                        Toast.makeText(
                                            this@WebViewActivity,
                                            "打开失败",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                        e.printStackTrace()
                                    }
                                }.show()
                                return true
                            }
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }
                    return super.shouldOverrideUrlLoading(view, request)
                }
            }
            webChromeClient = object : WebChromeClient() {
                override fun onProgressChanged(view: WebView?, newProgress: Int) {
                    if (newProgress == 100) {
                        binding.progressBar.visibility = View.GONE
                    } else {
                        binding.progressBar.visibility = View.VISIBLE
                        binding.progressBar.progress = newProgress
                    }
                }

                override fun onReceivedTitle(view: WebView, title: String) {
                    super.onReceivedTitle(view, title)
                    binding.toolBar.title = title
                }
            }
            loadUrl(link, mutableMapOf("X-Requested-With" to "com.coolapk.market"))
        }
    }


    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.webview_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> finish()

            R.id.refresh -> binding.webView.reload()

            R.id.copyLink -> {
                copyText(this, link)
            }

            R.id.openInBrowser -> {
                val uri = Uri.parse(link)
                val intent = Intent(Intent.ACTION_VIEW, uri)
                try {
                    startActivity(intent)
                } catch (e: ActivityNotFoundException) {
                    Toast.makeText(this, "打开失败", Toast.LENGTH_SHORT).show()
                    Log.w("error", "Activity was not found for intent, $intent")
                }
            }

            R.id.clearCache -> {
                binding.webView.clearHistory()
                binding.webView.clearCache(true)
                binding.webView.clearFormData()
                Toast.makeText(this, "清除缓存成功", Toast.LENGTH_SHORT).show()
            }

        }
        return true
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if (keyCode == KeyEvent.KEYCODE_BACK && binding.webView.canGoBack()) {
            binding.webView.goBack() //返回上个页面
            return true
        }
        return super.onKeyDown(keyCode, event) //退出H5界面
    }

}