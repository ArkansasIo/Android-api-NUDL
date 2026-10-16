package com.arkansasio.nudl.api

import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST

interface NudlApiService {
    @GET("api/v1/health")
    suspend fun health(): HealthResponse

    @POST("api/v1/nudl/lex")
    suspend fun lex(@Body request: NudlRequest): NudlResponse<Any>

    @POST("api/v1/nudl/parse")
    suspend fun parse(@Body request: NudlRequest): NudlResponse<Any>

    @POST("api/v1/nudl/analyze")
    suspend fun analyze(@Body request: NudlRequest): NudlResponse<Any>

    @POST("api/v1/nudl/run")
    suspend fun run(@Body request: NudlRequest): NudlResponse<Any>

    @POST("api/v1/nudl/generate")
    suspend fun generate(@Body request: NudlRequest): NudlResponse<Any>
}
