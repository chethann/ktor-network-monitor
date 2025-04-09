package io.github.chethann.network.monitor.utils

import android.content.Context

lateinit var appContext: Context

fun initializeClipboardUtils(context: Context) {
    appContext = context.applicationContext
}

actual fun copyToClipboard(text: String) {
    val clipboard = appContext.getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
    val clip = android.content.ClipData.newPlainText("Copied Text", text)
    clipboard.setPrimaryClip(clip)
}