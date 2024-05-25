package com.linhnvtit.reader.card_scheme

data class CardData(
    val brand: Brand = Brand.VISA,
    val cardNumber: String = "",
    val expireDate: String = ""
) {
    companion object {
        fun getCardBrand(scheme: CardScheme): Brand {
            return when (scheme) {
                is Visa -> Brand.VISA
                is MasterCard -> Brand.MASTERCARD
                else -> throw Exception("Card scheme not supported")
            }
        }
    }
}

enum class Brand {
    VISA,
    MASTERCARD
}