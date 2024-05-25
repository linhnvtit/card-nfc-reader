package com.linhnvtit.reader.emv

fun String.formatDateTime() =
    "${this.substring(4, 6)}/${this.substring(2, 4)}/${this.substring(0, 2)}"

val String.isRspCodeSuccess
    get() = this == "9000"

fun String.formatExpireDate(): String {
    return this.substring(2, 4) + "/" + this.substring(0, 2)
}

fun String.formatCardNumber(): String {
    return this.chunked(4).joinToString(" ")
}

fun String.decodeToByteArr(): ByteArray {
    check(length % 2 == 0) { "Must have an even length" }

    return chunked(2)
        .map { it.toInt(16).toByte() }
        .toByteArray()
}

fun String.decodeToByte(): Byte = toInt(16).toByte()

fun ByteArray.toHexString(): String {
    val result = StringBuilder(size * 2)
    forEach { byte ->
        result.append(String.format("%02x", byte))
    }
    return result.toString().uppercase()
}