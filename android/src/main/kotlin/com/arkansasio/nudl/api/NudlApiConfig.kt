package com.arkansasio.nudl.api

object NudlApiConfig {
    const val EMULATOR_BASE_URL = "http://10.0.2.2:8787/"

    fun normalizeBaseUrl(value: String): String =
        if (value.endsWith('/')) value else "$value/"
}
