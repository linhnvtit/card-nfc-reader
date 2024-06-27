package com.linhnvtit.reader.emv

import android.nfc.tech.IsoDep
import com.linhnvtit.reader.NfcReaderLog
import com.linhnvtit.reader.card_scheme.CardData
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex


class Emv(
    private val coroutineScope: CoroutineScope,
    private val kernel: Kernel,
) {
    private var stateChannel = Channel<EmvState>()

    private var mutex: Mutex = Mutex()

    /**
     * Only process VISA, Mastercard card for now
     */
    suspend fun readCard(
        isoDep: IsoDep,
        onSuccess: (CardData) -> Unit,
        onFailure: (String) -> Unit
    ) {
        if (!mutex.tryLock()) return
        NfcReaderLog.d("Start fetching data")

        coroutineScope.launch {
            try {
                isoDep.connect()
                initState()
            } catch (e: Exception) {
                NfcReaderLog.d("Read card failed")
                onFailure.invoke("Read card failed")
            }
        }

        coroutineScope.launch {
            try {
                while (true) {
                    NfcReaderLog.d("Loop")
                    when (val state = stateChannel.receive()) {
                        is EmvState.Idle -> {
                            NfcReaderLog.d("EmvState.Idle")
                        }

                        is EmvState.SelectPPSE -> {
                            NfcReaderLog.d("EmvState.SelectPPSE")

                            selectPPSE(isoDep)
                        }

                        is EmvState.SelectAid -> {
                            NfcReaderLog.d("EmvState.SelectAid")

                            selectAID(state, isoDep)
                        }

                        is EmvState.GetProcessingOption -> {
                            NfcReaderLog.d("EmvState.GetProcessingOption")

                            getProcessingOption(state, isoDep)
                        }

                        is EmvState.ReadRecord -> {
                            NfcReaderLog.d("EmvState.ReadRecord")

                            readRecord(state, isoDep)
                        }

                        is EmvState.Done -> {
                            NfcReaderLog.d("EmvState.Done")
                            onSuccess.invoke(state.cardData)
                            break
                        }

                        is EmvState.Fail -> {
                            NfcReaderLog.d("EmvState.Fail")
                            throw Exception(state.reason)
                        }
                    }
                }
            } catch (e: Exception) {
                NfcReaderLog.e("${e.message}")
                onFailure.invoke("${e.message}")
            } finally {
                finishProcess()
                mutex.unlock()
            }
        }
    }

    fun cancel() {
        coroutineScope.coroutineContext.cancelChildren()
    }

    private suspend fun initState() = coroutineScope.launch {
        stateChannel.send(EmvState.SelectPPSE)
    }

    private suspend fun finishProcess() = coroutineScope.launch {
        stateChannel.send(EmvState.Idle)
    }

    private suspend fun selectPPSE(isoDep: IsoDep) = coroutineScope.launch {
        selectPPSEExecute(isoDep).onSuccess { item ->
            NfcReaderLog.d("SelectPPSE success")

            stateChannel.send(
                EmvState.SelectAid(
                    tempEntries = item
                )
            )
        }.onFailure {
            NfcReaderLog.d("SelectPPSE fail ${it.message}")
            stateChannel.send(EmvState.Fail("SelectPPSE fail ${it.message}"))
        }
    }

    private suspend fun selectAID(stepData: EmvState.SelectAid, isoDep: IsoDep) = coroutineScope.launch {
        selectAIDExecute(stepData, isoDep).onSuccess { tags ->
            NfcReaderLog.d("SelectAID success")

            stateChannel.send(
                kernel.detectCardScheme(tags["84"]!!).let {
                    if (it != null) {
                        EmvState.GetProcessingOption(
                            tags = tags, cardScheme = it
                        )
                    } else {
                        EmvState.Fail(reason = "Unsupported card scheme")
                    }
                }
            )
        }.onFailure {
            NfcReaderLog.e("SelectAID fail ${it.message}")
            stateChannel.send(EmvState.Fail("SelectAID fail ${it.message}"))
        }
    }

    private suspend fun getProcessingOption(stepData: EmvState.GetProcessingOption, isoDep: IsoDep) = coroutineScope.launch {
        stepData.cardScheme.getProcessingOption(isoDep, stepData.tags).onSuccess {
            NfcReaderLog.d("GPO success")
            stateChannel.send(it)
        }.onFailure {
            NfcReaderLog.e("GPO fail ${it.message}")
            stateChannel.send(EmvState.Fail("GPO fail ${it.message}"))
        }
    }

    private suspend fun readRecord(stepData: EmvState.ReadRecord, isoDep: IsoDep) = coroutineScope.launch {
        stepData.cardScheme.readRecord(isoDep, stepData.tags).onSuccess {
            NfcReaderLog.d("ReadRecord success")
            stateChannel.send(it)
        }.onFailure {
            NfcReaderLog.e("Read Record fail ${it.message}")
            stateChannel.send(EmvState.Fail("Read Record fail ${it.message}"))
        }
    }

    private fun selectPPSEExecute(isoDep: IsoDep): Result<List<EntryTag61>> {
        return kotlin.runCatching {
            val command = APDU.genCommandSelectPPSE()
            NfcReaderLog.d("APDU Request: $command")
            val response = isoDep.transceive(command.decodeToByteArr())
            val hexRes = response.toHexString()
            NfcReaderLog.d("APDU Response: $hexRes")

            hexRes.substring(hexRes.length - 4).let {
                if (it.isRspCodeSuccess) {
                    return Result.success(APDU.decodeSelectPPSEResponse(hexRes.dropLast(4)))
                } else {
                    throw Exception("Response code: $it")
                }
            }
        }
    }

    private fun selectAIDExecute(stepData: EmvState.SelectAid, isoDep: IsoDep): Result<HashMap<String, String>> {
        return kotlin.runCatching {
            stepData.tempEntries.sortedBy { it.priorityT87.toInt() }.forEach {
                val command = APDU.genCommandSelectAID(it.aidT84)
                NfcReaderLog.d("APDU Request: $command")
                val response = isoDep.transceive(command.decodeToByteArr())
                val hexRes = response.toHexString()
                NfcReaderLog.d("APDU Response: $hexRes")

                hexRes.substring(hexRes.length - 4).let { rsp ->
                    if (rsp.isRspCodeSuccess) return@runCatching APDU.decodeSelectAIDResponse(hexRes.dropLast(4))
                }
            }
            throw Exception("Not found any matched AIDs")
        }
    }
}

