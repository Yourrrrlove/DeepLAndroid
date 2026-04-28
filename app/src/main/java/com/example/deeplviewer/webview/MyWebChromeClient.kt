package com.example.deeplviewer.webview

import android.graphics.Bitmap
import android.os.Message
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient

class MyWebChromeClient(
    private val mainWebView: WebView,
) : WebChromeClient() {
    override fun onCreateWindow(
        view: WebView,
        isDialog: Boolean,
        isUserGesture: Boolean,
        resultMsg: Message
    ): Boolean {
        val tempWebView = WebView(view.context)
        tempWebView.webViewClient = object : WebViewClient() {
            private var handled = false

            private fun handleUrl(url: String) {
                if (handled || url.isEmpty() || url == "about:blank") return
                handled = true
                mainWebView.loadUrl(url)
                mainWebView.post { tempWebView.destroy() }
            }

            override fun shouldOverrideUrlLoading(
                view: WebView,
                request: WebResourceRequest
            ): Boolean {
                handleUrl(request.url.toString())
                return true
            }

            override fun onPageStarted(view: WebView, url: String, favicon: Bitmap?) {
                handleUrl(url)
            }
        }
        val transport = resultMsg.obj as WebView.WebViewTransport
        transport.webView = tempWebView
        resultMsg.sendToTarget()
        return true
    }

    companion object {
        val DEEPL_INTERNAL_REGEX = Regex("^https://www\\.deepl\\.com/.*/(translator|write).*$")
    }
}
