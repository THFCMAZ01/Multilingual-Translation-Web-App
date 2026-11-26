package com.thfcmaz01.linguaswift.data

import android.text.Html
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

object MyMemoryApi {
    suspend fun translate(text: String, source: String, target: String): String =
        withContext(Dispatchers.IO) {
            val q = URLEncoder.encode(text, "UTF-8")
            val pair = URLEncoder.encode(source + "|" + target, "UTF-8")
            val url = URL("https://api.mymemory.translated.net/get?q=" + q + "&langpair=" + pair)
            val connection = url.openConnection() as HttpURLConnection
            try {
                connection.connectTimeout = 15000
                connection.readTimeout = 20000
                val status = connection.responseCode
                val stream = if (status in 200..299) connection.inputStream else connection.errorStream
                val body = stream?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }.orEmpty()
                if (status !in 200..299) throw IOException("Translation service returned HTTP " + status)
                val json = JSONObject(body)
                if (json.optInt("responseStatus", -1) != 200) {
                    throw IOException(json.optString("responseDetails").ifBlank { "Translation failed." })
                }
                val result = json.optJSONObject("responseData")?.optString("translatedText").orEmpty()
                if (result.isBlank()) throw IOException("The translation service returned an empty result.")
                Html.fromHtml(result, Html.FROM_HTML_MODE_LEGACY).toString().trim()
            } finally {
                connection.disconnect()
            }
        }
}
