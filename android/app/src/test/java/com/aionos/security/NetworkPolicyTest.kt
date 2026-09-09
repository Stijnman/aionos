package com.aionos.security

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class NetworkPolicyTest {
    @Test fun normalizesValidLanHost() {
        assertEquals(
            "http://ollama.local:11434",
            NetworkPolicy.validateOllamaHost(" http://ollama.local:11434/ ").getOrThrow()
        )
        assertEquals(
            "http://10.0.2.2:11434",
            NetworkPolicy.validateOllamaHost("http://10.0.2.2:11434").getOrThrow()
        )
    }

    @Test fun rejectsCredentialsAndUnsupportedSchemes() {
        assertTrue(NetworkPolicy.validateOllamaHost("https://user:pass@ollama.local").isFailure)
        assertTrue(NetworkPolicy.validateOllamaHost("file:///tmp/ollama").isFailure)
    }

    @Test fun rejectsPublicCleartextAndAcceptsHttps() {
        assertTrue(NetworkPolicy.validateOllamaHost("http://8.8.8.8:11434").isFailure)
        assertTrue(NetworkPolicy.validateOllamaHost("https://ollama.example.com:443").isSuccess)
    }

    @Test fun rejectsRawRfc1918CleartextAlignedWithNetworkSecurityConfig() {
        // Android NSC cannot whitelist CIDR; app policy matches cleartext domain-config.
        assertTrue(NetworkPolicy.validateOllamaHost("http://192.168.1.10:11434").isFailure)
        assertTrue(NetworkPolicy.validateOllamaHost("http://10.0.0.5:11434").isFailure)
        assertTrue(NetworkPolicy.isLocalNetworkHost("192.168.1.10"))
        assertFalse(NetworkPolicy.isCleartextAllowedHost("192.168.1.10"))
        // HTTPS to RFC1918 remains allowed
        assertTrue(NetworkPolicy.validateOllamaHost("https://192.168.1.10:11434").isSuccess)
    }

    @Test fun allowsLoopbackAndMdnsCleartext() {
        assertTrue(NetworkPolicy.validateOllamaHost("http://localhost:11434").isSuccess)
        assertTrue(NetworkPolicy.validateOllamaHost("http://127.0.0.1:11434").isSuccess)
        assertTrue(NetworkPolicy.validateOllamaHost("http://nas.local:11434").isSuccess)
    }
}
