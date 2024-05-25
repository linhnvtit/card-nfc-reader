package com.linhnvtit.reader.activity

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.lifecycle.lifecycleScope
import com.linhnvtit.reader.NfcReader
import com.linhnvtit.reader.NfcReaderLog
import kotlinx.coroutines.launch

open class NfcReaderActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        lifecycleScope.launch {
            NfcReader.init(this@NfcReaderActivity)
            NfcReader.handleNewIntent(intent)
        }
    }

    override fun onPause() {
        super.onPause()
        NfcReaderLog.d("onPause")
        NfcReader.disableNfcScanning(this@NfcReaderActivity)
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        NfcReaderLog.d("onNewIntent")
        NfcReader.handleNewIntent(intent)
    }
}