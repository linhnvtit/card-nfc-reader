package com.linhnvtit.reader.card_scheme

import android.nfc.tech.IsoDep
import com.linhnvtit.reader.EMPTY
import com.linhnvtit.reader.NfcReaderLog
import com.linhnvtit.reader.emv.APDU
import com.linhnvtit.reader.emv.EmvState
import com.linhnvtit.reader.emv.Strings
import com.linhnvtit.reader.emv.decodeToByteArr
import com.linhnvtit.reader.emv.isRspCodeSuccess
import com.linhnvtit.reader.emv.toHexString
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.delay

open class BaseCardScheme(
    private val coroutineScope: CoroutineScope
) : CardScheme {
    override suspend fun getProcessingOption(
        isoDep: IsoDep,
        tags: HashMap<String, String>,
    ): Result<EmvState> = kotlin.runCatching {
        val command = APDU.genCommandGPO(tags["9F38"] ?: "")

        NfcReaderLog.d("APDU Request: $command")
        val response = isoDep.transceive(command.decodeToByteArr())
        val hexRes = response.toHexString()
        NfcReaderLog.d("APDU Response: $hexRes")

        hexRes.substring(hexRes.length - 4).let { rsp ->
            if (rsp.isRspCodeSuccess) return@runCatching EmvState.ReadRecord(
                tags = tags.apply { putAll(APDU.decodeGPOResponse(hexRes.dropLast(4))) }, cardScheme = this
            )
            else throw Exception(rsp)
        }
    }

    override suspend fun readRecord(
        isoDep: IsoDep,
        tags: HashMap<String, String>,
    ): Result<EmvState> = kotlin.runCatching {
        val aflTag = Strings(tags["94"]!!)
        val newTags = hashMapOf<String, String>()

        val listAFL = buildList {
            while (aflTag.isNotBlank()) {
                add(aflTag.squish(8))
            }
        }

        val fetchRecordJobs: ArrayList<Deferred<Unit>> = arrayListOf()
        listAFL.forEach {
            fetchRecordJobs.add(coroutineScope.async {
                val startInd = it.substring(2, 4).toInt()
                val endInd = it.substring(4, 6).toInt()
                val commands = arrayListOf<String>()

                (startInd..endInd).forEach { ind ->
                    commands.add(
                        APDU.genCommandReadRecord(
                            ind.toString().padStart(2, '0'),
                            it.substring(0, 2),
                        )
                    )
                }
                val fetchSubRecordJobs: ArrayList<Deferred<Unit>> = arrayListOf()
                commands.forEachIndexed { index, command ->
                    fetchSubRecordJobs.add(coroutineScope.async {
                        try {
                            delay(index * 50L)
                            NfcReaderLog.d("APDU Request: $command")
                            val response = isoDep.transceive(command.decodeToByteArr())
                            val hexRes = response.toHexString()
                            NfcReaderLog.d("APDU Response: $hexRes")

                            hexRes.substring(hexRes.length - 4).let { rsp ->
                                if (rsp.isRspCodeSuccess) newTags.putAll(APDU.decodeReadRecordResponse(hexRes.dropLast(4)))
                                else throw Exception(rsp)
                            }
                        } catch (e: Exception) {
                            NfcReaderLog.d("exception: $e")
                        }
                    })
                }

                fetchSubRecordJobs.awaitAll()
                Unit
            })
        }
        fetchRecordJobs.awaitAll()

        tags.putAll(newTags)

        return@runCatching EmvState.Done(
            cardData = CardData(
                brand = CardData.getCardBrand(this),
                cardNumber = tags["5A"] ?: throw Exception("require field 5A"),
                expireDate = tags["5F24"] ?: throw Exception("require field 5F24"),
                cardHolderName = tags["5F20"] ?: EMPTY,
            )
        )
    }
}