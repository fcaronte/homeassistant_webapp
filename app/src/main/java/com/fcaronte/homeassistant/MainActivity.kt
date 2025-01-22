package com.fcaronte.homeassistant

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.webkit.CookieManager
import android.webkit.WebResourceError
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.content.ContextCompat
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import androidx.appcompat.view.ContextThemeWrapper

class MainActivity : AppCompatActivity() {
    private lateinit var webView: WebView
    private lateinit var swipeRefreshLayout: SwipeRefreshLayout
    private var isBackPressedOnce = false
    private val prefsName = "HomeAssistantPrefs"
    private val prefURL = "url"

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        webView = findViewById(R.id.webView)

        webView.settings.javaScriptEnabled = true
        webView.settings.domStorageEnabled = true
        webView.clearCache(false)
        webView.setLayerType(View.LAYER_TYPE_HARDWARE, null)

        // Imposta il listener per il refresh
        swipeRefreshLayout = findViewById(R.id.swipeRefreshLayout)

        swipeRefreshLayout.setOnRefreshListener {
            webView.reload()
        }

        // Doppio swipe per uscire
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (isBackPressedOnce) {
                    finish()
                } else {
                    isBackPressedOnce = true
                    Toast.makeText(this@MainActivity, getString(R.string.please_click_back_again_to_exit), Toast.LENGTH_SHORT).show()
                    Handler(Looper.getMainLooper()).postDelayed({ isBackPressedOnce = false }, 2000)
                }
            }
        })


        // Imposta il tema dark se il sistema è impostato in modalità dark
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)

        // Abilita i cookie
        val cookieManager = CookieManager.getInstance()
        cookieManager.setAcceptCookie(true)
        cookieManager.setAcceptThirdPartyCookies(webView, true)

        webView.webViewClient = object : WebViewClient() {

            override fun onPageFinished(view: WebView, url: String) {
                super.onPageFinished(view, url)
                swipeRefreshLayout.isRefreshing = false
            }

            override fun onReceivedError(
                view: WebView,
                request: WebResourceRequest,
                error: WebResourceError
            ) {
                Toast.makeText(
                    this@MainActivity,
                    getString(R.string.error_loading_page, error.description),
                    Toast.LENGTH_LONG
                ).show()
            }
        }

        swipeRefreshLayout.setOnRefreshListener {
            webView.reload()
        }

        // Carica l'URL salvato e aprilo nel WebView
        loadSavedUrl()

        handleIntent(intent)

    }

    private fun loadSavedUrl() {
        val sharedPreferences = getSharedPreferences(prefsName, Context.MODE_PRIVATE)
        val savedUrl = sharedPreferences.getString(prefURL, null)

        if (savedUrl.isNullOrEmpty()) {
            findViewById<TextInputLayout>(R.id.textInputLayoutUrl).visibility = View.VISIBLE
            showUrlInputDialog(null)
        } else {
            webView.loadUrl(savedUrl)
        }
    }

    private fun showUrlInputDialog(currentUrl: String?) {
        val dialogView = layoutInflater.inflate(R.layout.url_input_dialog, null)
        val input =
            dialogView.findViewById<TextInputEditText>(R.id.editTextUrl)// Popola il campo di input con l'ultimo URL salvato, se esiste
        if (!currentUrl.isNullOrEmpty()) {
            input.setText(currentUrl)
        }
        val dialogContext = ContextThemeWrapper(this, R.style.RoundedTextInputLayout)
        val builder = AlertDialog.Builder(dialogContext)
        builder.setTitle(getString(R.string.inserisci_url_di_home_assistant))
        builder.setView(dialogView)

        val dialog = builder.create() // Crea il dialog qui
        dialog.window?.setBackgroundDrawable(
            ContextCompat.getDrawable(
                this,
                R.drawable.rounded_dialog_background
            )
        ) // Impostalo sfondo arrotondato qui

        // Imposta il listener per il pulsante"OK"
        dialog.setButton(AlertDialog.BUTTON_POSITIVE, "OK") { _, _ ->
            val url = input.text.toString()
            if (url.isNotEmpty()) {
                saveUrl(url)
                webView.loadUrl(url)
            } else {
                Toast.makeText(this, "L'URL non può essere vuoto", Toast.LENGTH_SHORT).show()
                showUrlInputDialog(url) // Richiama il dialog se l'URL è vuoto
            }
            dialog.dismiss() // Chiudi il dialog dopo aver gestito l'input
        }

        // Imposta il listener per il pulsante "Annulla"
        dialog.setButton(AlertDialog.BUTTON_NEGATIVE, "Annulla") { _, _ ->
            dialog.dismiss() // Chiudi il dialog quando l'utente annulla
            finish() // Chiudi l'app se l'utente annulla
        }

        dialog.show() // Mostra il dialog qui
    }

    private fun saveUrl(url: String) {
        val sharedPreferences = getSharedPreferences(prefsName, Context.MODE_PRIVATE)
        with(sharedPreferences.edit()) {
            putString(prefURL, url)
            apply()
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent) {
        if (intent.action == "android.intent.action.VIEW") {
            val sharedPreferences = getSharedPreferences(prefsName, Context.MODE_PRIVATE)
            val savedUrl = sharedPreferences.getString(prefURL, null)
            showUrlInputDialog(savedUrl)
        }
    }
}

