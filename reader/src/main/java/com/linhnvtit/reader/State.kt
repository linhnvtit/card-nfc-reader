package com.linhnvtit.reader

import com.linhnvtit.reader.card_scheme.CardData

sealed class State {
    data object Idle: State()

    data object Scanning: State()

    data object Fetching: State()

    data class Success(
        val data: CardData
    ): State()

    data class Fail(
        val reason: String
    ): State()

    data object Canceled: State()
}