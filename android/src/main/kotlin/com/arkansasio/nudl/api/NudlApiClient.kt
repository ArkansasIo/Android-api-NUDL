package com.arkansasio.nudl.api

import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object NudlApiClient {
    fun create(baseUrl: String): NudlApiService {
        require(baseUrl.endsWith('/')) { "baseUrl must end with '/'" }
        val httpClient = OkHttpClient.Builder().build()
        return Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(httpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(NudlApiService::class.java)
    }
}
