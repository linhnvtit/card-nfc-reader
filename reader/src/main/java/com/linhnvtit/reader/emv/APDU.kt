package com.linhnvtit.reader.emv


object APDU {
    fun genCommandSelectPPSE(): String = "00A404000E325041592E5359532E444446303100"

    fun genCommandSelectAID(
        data: String
    ): String {
        // CLA + INS + P1 + P2
        val p1 = "00A40400"
        // Lc
        val p2 = Integer.toHexString(data.length / 2).toString().padStart(2, '0')
        // Le
        val p4 = "00"

        return p1 + p2 + data + p4
    }

    fun genCommandGPO(
        pdol: String
    ): String {
        val unpredictableNumberT9F37 = Integer.toHexString((0..Int.MAX_VALUE).random())
            .padStart(8, (Char.MIN_VALUE..Char.MAX_VALUE).random())
            .substring(0, 8)

        val sample = hashMapOf(
            "9F66" to "21000000",
            "9F02" to "000000000001",
            "9F03" to "000000000000",
            "9F1A" to "0704",
            "95" to "0000000000",
            "5F2A" to "0840",
            "9A" to "230731",
            "9C" to "00",
            "9F35" to "22",
            "9F4E" to "0000000000000000000000000000000000000000000000000000000000000000",
            "9F1D" to "0F80000000000000",
            "9F5C" to "0000000000100000",
            "9F40" to "50A0000000",
            "9F37" to unpredictableNumberT9F37
        )

        val pdolTags = arrayListOf<String>()
        val pdolStr = Strings(pdol)

        while (pdolStr.isNotBlank()) {
            var tag = pdolStr.squish(2)
            while (!Tags.isValidTag(tag)) {
                tag += pdolStr.squish(2)
            }

            // length
            pdolStr.squish(2).toInt(16)

            pdolTags.add(tag)
        }

        val data = buildString {
            pdolTags.forEach {
                append(sample[it])
            }
        }

        // CLA + INS + P1 + P2
        val p1 = "80A80000"

        // 83 Length
        val p3 = Integer.toHexString(data.length / 2).padStart(2, '0').uppercase()

        // Lc
        val p2 = Integer.toHexString(data.length / 2 + 2).padStart(2, '0').uppercase()

        // Le
        val p4 = "00"

        return p1 + p2 + "83" + p3 + data + p4
    }

    fun genCommandReadRecord(
        recordNumber: String,
        sfi: String,
    ): String {
        // CLA + INS
        val p1 = "00B2"

        // P2
        val p3 = sfi.toInt(16).shr(3).shl(3).or(4).toString(16).padStart(2, '0').uppercase()

        // Le
        val p4 = "00"

        return p1 + recordNumber + p3 + p4
    }

    fun decodeSelectPPSEResponse(data: String): List<EntryTag61> {
        val templates = listOf(
            "6F",
            "A5",
            "BF0C",
        )

        val tlvData = Strings(data)

        return buildList {
            while (tlvData.isNotBlank()) {
                var tag = tlvData.squish(2)
                while (!Tags.isValidTag(tag)) {
                    tag += tlvData.squish(2)
                }

                val length = tlvData.squish(2).toInt(16)

                if (tag in templates)
                    continue

                val value = tlvData.squish(length * 2)

                if (tag == "61") {
                    decodeTLV(value).let {
                        requireNotNull(it["4F"]) { "Require field 4F" }
//                        requireNotNull(it["50"]) { "Require field 50" }
//                        requireNotNull(it["87"])

                        add(
                            EntryTag61(
                                aidT84 = it["4F"]!!,
                                labelT50 = it["50"] ?: "",
                                priorityT87 = it["87"] ?: "",
                                kernelIdentifierT9F2A = it["9F2A"] ?: "",
                                extendedSelectionT9F29 = it["9F29"] ?: "",
                                proprietaryDataT9F0A = it["9F0A"] ?: "",
                            )
                        )
                    }
                }
            }
        }
    }

    fun decodeSelectAIDResponse(data: String): HashMap<String, String> {
        val hash = decodeTLV(data)

        // validate response
        requireNotNull(hash["84"]) { "Require field 84" }
        requireNotNull(hash["50"]) { "Require field 50" }

        return hash
    }

    fun decodeGPOResponse(data: String): HashMap<String, String> {
        val hash: HashMap<String, String> = decodeTLV(data)

        // validate response
        requireNotNull(hash["82"]) { "Require field 82" }
        requireNotNull(hash["94"]) { "Require field 94" }
//        requireNotNull(hash["57"])
//        requireNotNull(hash["9F10"])
//        requireNotNull(hash["9F26"])
//        requireNotNull(hash["9F27"])
//        requireNotNull(hash["9F36"])
//        requireNotNull(hash["9F4B"])

        return hash
    }

    fun decodeReadRecordResponse(
        data: String
    ): HashMap<String, String> {
        return decodeTLV(data)
    }

    private fun decodeTLV(data: String): HashMap<String, String> {
        try {
            val tlvData = Strings(data)
            val hashMap = hashMapOf<String, String>()

            while (tlvData.isNotBlank()) {
                var tag = tlvData.squish(2)
                while (!Tags.isValidTag(tag)) {
                    tag += tlvData.squish(2)
                }

                val length = tlvData.squish(2).let {
                    if (it.toInt(16) <= 0x7F) it else tlvData.squish(2)
                }
                    .toInt(16)

                if (Tags.isTemplateTag(tag))
                    continue

                val value = tlvData.squish(length * 2)

                hashMap[tag] = value
            }

            return hashMap
        } catch (e: Exception) {
            return hashMapOf()
        }
    }
}