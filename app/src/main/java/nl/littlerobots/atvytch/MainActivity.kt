/**
 * Copyright 2024 Hugo Visser
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */
package nl.littlerobots.atvytch

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.KeyEvent
import android.webkit.WebView
import androidx.activity.ComponentActivity
import androidx.webkit.JavaScriptReplyProxy
import androidx.webkit.WebViewCompat

class MainActivity : ComponentActivity() {
    private var channelProxy: JavaScriptReplyProxy? = null

    @SuppressLint("SetJavaScriptEnabled", "RequiresFeature")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val webView = WebView(this)
        setContentView(webView)
        webView.settings.javaScriptEnabled = true
        webView.settings.domStorageEnabled = true
        webView.settings.allowContentAccess = false
        webView.settings.mediaPlaybackRequiresUserGesture = false
        WebViewCompat.addDocumentStartJavaScript(
            webView,
            """
                document.addEventListener("DOMContentLoaded", (event) => {
                  // use getList as a hook to hide the controls and unmute
                  const oldGetList = window.getList
                  window.getList = () => {
                    toggleMute();
                    toggleControl();
                    control.style.opacity = "0";
                    return oldGetList()
                  }
                });

                remote.onmessage = function(event) {
                    if (event.data == "next") {
                        switchChannel(1);
                    } else if (event.data == "prev") {
                        switchChannel(-1);
                    }
                }
                remote.postMessage("ready");
            """.trimIndent(),
            setOf("https://ytch.xyz")
        )
        WebViewCompat.addWebMessageListener(
            webView, "remote", setOf("https://ytch.xyz")
        ) { _, _, _, _, replyProxy -> channelProxy = replyProxy }
        webView.loadUrl("https://ytch.xyz")
    }

    @SuppressLint("RestrictedApi", "RequiresFeature")
    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        if (event.action == KeyEvent.ACTION_DOWN) {
            return when {
                event.repeatCount > 0 -> super.dispatchKeyEvent(event)
                event.keyCode == KeyEvent.KEYCODE_DPAD_DOWN -> {
                    channelProxy?.postMessage("prev")
                    true
                }

                event.keyCode == KeyEvent.KEYCODE_DPAD_UP -> {
                    channelProxy?.postMessage("next")
                    true
                }

                else -> super.dispatchKeyEvent(event)
            }
        }
        return super.dispatchKeyEvent(event)
    }
}
