package com.linhnvtit.reader

import android.util.Log
import java.util.concurrent.atomic.AtomicBoolean

internal object NfcReaderLog {
    var enabled: AtomicBoolean = AtomicBoolean(false)

    fun d(message: String) {
        if (enabled.get()) {
            Thread.currentThread().stackTrace[3].let {
                Log.d(NFC_READER, "[${it.fileName}:${it.lineNumber}] [Debug]: $message")
            }
        }
    }

    fun e(message: String) {
        if (enabled.get()) {
            Thread.currentThread().stackTrace[3].let {
                Log.e(NFC_READER, "[${it.fileName}:${it.lineNumber}] [Error]: $message")
            }
        }
    }

    fun network(message: String) {
        if (enabled.get()) {
            Thread.currentThread().stackTrace[3].let {
                Log.i(NFC_READER, "[${it.fileName}:${it.lineNumber}] [Network]: $message")
            }
        }
    }
}