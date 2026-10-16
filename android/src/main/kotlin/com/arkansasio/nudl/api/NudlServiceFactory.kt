package com.arkansasio.nudl.api

object NudlServiceFactory {
    fun create(baseUrl: String = NudlApiConfig.EMULATOR_BASE_URL): NudlApiService =
        NudlApiClient.create(NudlApiConfig.normalizeBaseUrl(baseUrl))
}
