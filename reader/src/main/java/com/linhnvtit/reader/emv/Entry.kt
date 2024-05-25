package com.linhnvtit.reader.emv

data class EntryTag61(
    val aidT84: String,
    val labelT50: String,
    val priorityT87: String,
    val kernelIdentifierT9F2A: String = "",
    val extendedSelectionT9F29: String = "",
    val proprietaryDataT9F0A: String = ""
)
