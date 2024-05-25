package com.linhnvtit.reader

import android.app.Activity
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.nfc.NfcAdapter
import android.nfc.tech.IsoDep
import com.linhnvtit.reader.emv.Emv
import com.linhnvtit.reader.emv.Kernel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex

object NfcReader {
    private val mutex: Mutex = Mutex()
    private var adapter: NfcAdapter? = null
    private var sourceFlow = MutableStateFlow<State>(State.Idle)

    private var coroutineScope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private lateinit var emv: Emv

    fun init(context: Context) {
        if (!mutex.tryLock()) return

        adapter = NfcAdapter.getDefaultAdapter(context)
        if (adapter == null) throw Exception("This device doesn't support NFC")
        if (adapter?.isEnabled == false) throw Exception("NFC disabled")

        emv = Emv(
            coroutineScope = coroutineScope,
            kernel = Kernel(context)
        )
        mutex.unlock()
    }

    fun enableLog() {
        NfcReaderLog.enabled.set(true)
    }

    fun disableLog() {
        NfcReaderLog.enabled.set(false)
    }

    fun enableNfcScanning(activity: Activity) {
        val pendingIntent = PendingIntent.getActivity(
            activity,
            0,
            Intent(
                activity,
                activity.javaClass
            ).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
        )

        val intentFilter: Array<IntentFilter> = arrayOf(
            IntentFilter(NfcAdapter.ACTION_NDEF_DISCOVERED),
            IntentFilter(NfcAdapter.ACTION_TECH_DISCOVERED),
            IntentFilter(NfcAdapter.ACTION_TAG_DISCOVERED)
        )

        val techList: Array<Array<String>> = arrayOf(
            arrayOf(IsoDep::class.java.name)
        )

        adapter?.enableForegroundDispatch(
            activity,
            pendingIntent,
            intentFilter,
            techList
        )

        coroutineScope.launch {
            sourceFlow.emit(State.Scanning)
        }
    }

    fun disableNfcScanning(activity: Activity) {
        adapter?.disableForegroundDispatch(activity)
    }

    fun handleNewIntent(intent: Intent?) {
        if (
            intent?.action != NfcAdapter.ACTION_TECH_DISCOVERED ||
            adapter == null
        ) {
            return
        }

        try {
            val isoDep = IsoDep.get(intent.getParcelableExtra(NfcAdapter.EXTRA_TAG))!!

            coroutineScope.launch {
                sourceFlow.emit(State.Fetching)

                emv.readCard(
                    isoDep = isoDep,
                    onSuccess = {
                        coroutineScope.launch {
                            sourceFlow.emit(State.Success(it))
                        }
                    },
                    onFailure = {
                        coroutineScope.launch {
                            sourceFlow.emit(State.Fail(it))
                        }
                    }
                )
            }
        } catch (e: Exception) {
            NfcReaderLog.d("${e.message}")
        }
    }

    suspend fun cancelReadingProcess() {
        if (!::emv.isInitialized) throw Exception("NFC process need to be initialized before cancel.")
        emv.cancel()
        sourceFlow.emit(State.Canceled)
    }

    fun getSourceStateFlow() = sourceFlow
}