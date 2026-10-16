package com.arkansasio.nudl.api

import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

/**
 * Creates a NUDL HTTP API client.
 *
 * Pass a trailing-slash base URL, for example:
 * - Emulator host bridge: http://10.0.2.2:8787/
 * - Physical device: http://192.168.1.10:8787/
 * - Production: https://api.example.com/
 */
object NudlApiClient {
    fun create(
        baseUrl: String,
        debugLogging: Boolean = false,
    ): NudlApiService {
        val normalizedBaseUrl = NudlApiConfig.normalizeBaseUrl(baseUrl)
        require(normalizedBaseUrl.startsWith("https://") || normalizedBaseUrl.startsWith("http://")) {
            "baseUrl must use http:// or https://"
        }

        val logging = HttpLoggingInterceptor().apply {
            level = if (debugLogging) HttpLoggingInterceptor.Level.BASIC
            else HttpLoggingInterceptor.Level.NONE
        }
        val httpClient = OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .addInterceptor(logging)
            .build()

        return Retrofit.Builder()
            .baseUrl(normalizedBaseUrl)
            .client(httpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(NudlApiService::class.java)
    }
}
