package com.linhnvtit.reader.card_scheme

import android.nfc.tech.IsoDep
import com.linhnvtit.reader.emv.EmvState

interface CardScheme {
    suspend fun getProcessingOption(
        isoDep: IsoDep,
        tags: HashMap<String, String>,
    ): Result<EmvState>

    suspend fun readRecord(
        isoDep: IsoDep,
        tags: HashMap<String, String>,
    ): Result<EmvState>
}

