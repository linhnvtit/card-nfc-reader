package com.linhnvtit.reader.emv

import com.linhnvtit.reader.card_scheme.CardData
import com.linhnvtit.reader.card_scheme.CardScheme

sealed class EmvState {
    data object Idle: EmvState()

    data object SelectPPSE: EmvState()

    data class SelectAid(
        val tempEntries: List<EntryTag61>
    ): EmvState()

    data class GetProcessingOption(
        val tags: HashMap<String, String>,
        val cardScheme: CardScheme
    ): EmvState()

    data class ReadRecord(
        val tags: HashMap<String, String>,
        val cardScheme: CardScheme
    ): EmvState()

    data class Done(
        val cardData: CardData
    ): EmvState()

    data class Fail(
        val reason: String
    ): EmvState()
}