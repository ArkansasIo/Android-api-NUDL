package com.arkansasio.nudl.api

class NudlRepository(private val api: NudlApiService) {
    suspend fun health() = api.health()
    suspend fun lex(source: String, filename: String = "main.nudl") = api.lex(NudlRequest(source, filename))
    suspend fun parse(source: String, filename: String = "main.nudl") = api.parse(NudlRequest(source, filename))
    suspend fun analyze(source: String, filename: String = "main.nudl") = api.analyze(NudlRequest(source, filename))
    suspend fun run(source: String, filename: String = "main.nudl") = api.run(NudlRequest(source, filename))
    suspend fun generate(source: String, target: String, filename: String = "main.nudl") = api.generate(NudlRequest(source, filename, target))
}
