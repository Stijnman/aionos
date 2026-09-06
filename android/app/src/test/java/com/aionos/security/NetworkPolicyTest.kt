package com.aionos.security

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class NetworkPolicyTest {
    @Test fun normalizesValidLanHost() {
        assertEquals("http://192.168.1.10:11434", NetworkPolicy.validateOllamaHost(" http://192.168.1.10:11434/ ").getOrThrow())
    }

    @Test fun rejectsCredentialsAndUnsupportedSchemes() {
        assertTrue(NetworkPolicy.validateOllamaHost("https://user:pass@ollama.local").isFailure)
        assertTrue(NetworkPolicy.validateOllamaHost("file:///tmp/ollama").isFailure)
    }

    @Test fun rejectsPublicCleartextAndAcceptsHttps() {
        assertTrue(NetworkPolicy.validateOllamaHost("http://8.8.8.8:11434").isFailure)
        assertTrue(NetworkPolicy.validateOllamaHost("https://ollama.example.com:443").isSuccess)
    }
}
