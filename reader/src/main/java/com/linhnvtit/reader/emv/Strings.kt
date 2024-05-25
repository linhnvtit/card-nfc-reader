package com.linhnvtit.reader.emv

data class Strings(
    private var _value: String
) {
    fun squish(num: Int): String {
        val droppedPart = _value.take(num)
        _value = _value.drop(num)
        return droppedPart
    }

    fun isNotBlank(): Boolean = _value.isNotBlank()
}