package com.example.deeplviewer.activity

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.view.animation.AlphaAnimation
import androidx.appcompat.app.AppCompatActivity
import com.example.deeplviewer.R
import com.example.deeplviewer.webview.MyWebViewClient
import com.example.deeplviewer.webview.NestedScrollWebView
import com.example.deeplviewer.webview.WebAppInterface
import com.facebook.shimmer.ShimmerFrameLayout
import com.google.android.material.bottomsheet.BottomSheetDialog


class FloatingTextSelection : AppCompatActivity() {

    private val startUrl by lazy {
        val urlParam = getSharedPreferences("config", Context.MODE_PRIVATE).getString(
            "urlParam",
            DEFAULT_PARAM
        ) ?: DEFAULT_PARAM
        return@lazy "https://www.deepl.com/translator$urlParam"
    }

    companion object {
        private const val DEFAULT_PARAM = "#en/en/"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            val androidTranslateFloatingText =
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                    intent.getCharSequenceExtra(Intent.EXTRA_TEXT)
                } else {
                    null
                }

            val floatingText = (androidTranslateFloatingText
                ?: intent.getCharSequenceExtra(Intent.EXTRA_PROCESS_TEXT)) as String
            val config = getSharedPreferences("config", Context.MODE_PRIVATE)
            val usePopup = config.getBoolean(getString(R.string.key_switch_popup_mode), true)

            if (usePopup) {
                launchPopup(floatingText)
            } else {
                launchFullscreen(floatingText)
            }
        }
    }

    override fun onPause() {
        super.onPause()
        finish()
    }

    override fun finish() {
        super.finish()
        overridePendingTransition(0, 0)
    }

    private fun launchFullscreen(initialText: String) {
        val intent = Intent(this, MainActivity::class.java)
        intent.putExtra("FLOATING_TEXT", initialText)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_DOCUMENT)
        startActivity(intent)
        overridePendingTransition(0, 0)
        finish()
    }

    @SuppressLint("SetJavaScriptEnabled", "RestrictedApi", "VisibleForTests")
    private fun launchPopup(initialText: String) {
        val layout = layoutInflater.inflate(R.layout.popup_layout, null)
        val webView = layout.findViewById<NestedScrollWebView>(R.id.webview)

        webView.settings.javaScriptEnabled = true
        webView.settings.domStorageEnabled = true

        val webViewClient = MyWebViewClient(this)
        webView.webViewClient = webViewClient
        webView.addJavascriptInterface(WebAppInterface(this), "Android")

        val dialog = BottomSheetDialog(this)
        dialog.setContentView(layout)
        dialog.setOnDismissListener { finish() }
        dialog.behavior.disableShapeAnimations()
        dialog.show()

        webViewClient.loadFinishedListener = {
            // wait a bit, cause the WebView will change it's height multiple times caused by some lazy-loaded elements
            Handler(Looper.getMainLooper()).postDelayed({
                // Get the screen height and set the WebView height to 70% of the screen height
                val displayMetrics = resources.displayMetrics
                val screenHeight = displayMetrics.heightPixels
                val webViewHeight = (screenHeight * 0.7).toInt()

                val layoutParams = webView.layoutParams
                layoutParams.height = webViewHeight
                webView.layoutParams = layoutParams

                webView.measure(
                    View.MeasureSpec.makeMeasureSpec(webView.width, View.MeasureSpec.EXACTLY),
                    View.MeasureSpec.makeMeasureSpec(webViewHeight, View.MeasureSpec.EXACTLY)
                )
                webView.layout(0, 0, webView.width, webViewHeight)

                // Fade in the WebView and hide the shimmer effect
                val animation = AlphaAnimation(0.0F, 1.0F)
                animation.duration = 250
                webView.visibility = View.VISIBLE
                webView.startAnimation(animation)
                layout.findViewById<ShimmerFrameLayout>(R.id.shimmer_view_container).hideShimmer()
            }, 750)
        }

        webView.loadUrl(
            startUrl + Uri.encode(
                initialText.replace(
                    "/",
                    "\\/"
                )
            )
        )
    }
}
