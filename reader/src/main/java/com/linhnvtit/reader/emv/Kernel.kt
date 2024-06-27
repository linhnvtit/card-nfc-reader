package com.linhnvtit.reader.emv

import android.content.Context
import com.linhnvtit.reader.R
import com.linhnvtit.reader.card_scheme.CardScheme
import com.linhnvtit.reader.card_scheme.MasterCard
import com.linhnvtit.reader.card_scheme.Visa
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import java.io.BufferedReader
import java.io.InputStreamReader

class Kernel(
    context: Context
) {
    private val visaAids: HashSet<String>
    private var masterCardAids: HashSet<String>

    init {
        visaAids = getAids(context, R.raw.visa_aids)
        masterCardAids = getAids(context, R.raw.master_card_aids)
    }

    fun detectCardScheme(aid: String): CardScheme? {
        return when (aid) {
            in visaAids -> Visa(CoroutineScope(Dispatchers.Main + SupervisorJob()))
            in masterCardAids -> MasterCard(CoroutineScope(Dispatchers.Main + SupervisorJob()))
            else -> null
        }
    }

    private fun getAids(context: Context, resID: Int): HashSet<String> {
        val aids: HashSet<String> = hashSetOf()

        val inputStream = context.resources.openRawResource(resID)
        val inputStreamReader = InputStreamReader(inputStream)
        val bufferedReader = BufferedReader(inputStreamReader)

        var line: String?
        try {
            while (bufferedReader.readLine().also { line = it } != null) {
                aids.add(line!!)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            try {
                bufferedReader.close()
                inputStreamReader.close()
                inputStream.close()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        return aids
    }
}