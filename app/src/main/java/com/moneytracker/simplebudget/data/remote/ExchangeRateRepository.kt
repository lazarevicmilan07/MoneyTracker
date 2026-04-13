package com.moneytracker.simplebudget.data.remote

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.double
import java.net.HttpURLConnection
import java.net.URL
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ExchangeRateRepository @Inject constructor() {

    private val json = Json { ignoreUnknownKeys = true }

    suspend fun fetchRate(from: String, to: String): Result<Double> = withContext(Dispatchers.IO) {
        runCatching {
            val conn = URL("https://api.exchangerate-api.com/v4/latest/$from").openConnection() as HttpURLConnection
            conn.connectTimeout = 8_000
            conn.readTimeout = 8_000
            conn.requestMethod = "GET"
            try {
                if (conn.responseCode != HttpURLConnection.HTTP_OK) {
                    throw Exception("HTTP ${conn.responseCode}")
                }
                val body = conn.inputStream.bufferedReader().readText()
                json.parseToJsonElement(body)
                    .jsonObject["rates"]
                    ?.jsonObject?.get(to)
                    ?.jsonPrimitive?.double
                    ?: throw Exception("$to not found in response")
            } finally {
                conn.disconnect()
            }
        }
    }
}
