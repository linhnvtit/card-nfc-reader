package com.linhnvtit.reader.emv

object Tags {
    private var templateTags: List<String> = listOf(
        "6F",
        "A5",
        "BF0C",
        "61",
        "70",
        "77",
    )

    private var asciiValueTag: List<String> = listOf(
        "5F20",
        "8A",
        "50"
    )

    fun getTagValue(tag: String, value: String): Pair<String, String> {
        return when {
            isASCIIValueTag(tag) ->
                Pair(
                    tag,
                    value.chunked(2)
                        .map { it.toInt(16).toByte() }
                        .toByteArray()
                        .toString(Charsets.ISO_8859_1)
                )

            else ->
                Pair(tag, value)
        }
    }


    fun isValidTag(tag: String): Boolean {
        tag.toInt(16).let {
            if (
                (tag.length == 2 && (it and 0b00011111) == 0b00011111) ||
                (tag.length > 2 && (it and 0b10000000) == 0b10000000)
            )
                return false
        }

        return true
    }

    fun isTemplateTag(tag: String): Boolean = tag in templateTags

    private fun isASCIIValueTag(tag: String) = tag.uppercase() in asciiValueTag
}