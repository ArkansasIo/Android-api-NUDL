package com.arkansasio.nudl.api

import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class NudlApiInstrumentedTest {
    private val service = NudlServiceFactory.create()

    @Test
    fun healthEndpointIsReachableFromAndroidEmulator() = runBlocking {
        val response = service.health()
        assertTrue(response.ok)
        assertEquals("nudl-api", response.service)
        assertEquals("0.3.0", response.version)
    }

    @Test
    fun runEndpointExecutesNudlThroughRetrofit() = runBlocking {
        val response = service.run(NudlRequest(source = "fn main() { let x = 2 + 3; print(x); return x; }"))
        assertTrue(response.ok)
        assertTrue(response.result != null)
    }

    @Test
    fun generateEndpointRespondsThroughRetrofit() = runBlocking {
        val response = service.generate(NudlRequest(source = "fn main() { print(\"hello\"); }", target = "TypeScript"))
        assertTrue(response.ok)
        assertTrue(response.result != null)
    }
}
