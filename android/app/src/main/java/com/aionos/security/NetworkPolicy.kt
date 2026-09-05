package com.aionos.security

import java.net.URI

object NetworkPolicy {
    fun validateOllamaHost(value: String): Result<String> = runCatching {
        val normalized = value.trim().removeSuffix("/")
        val uri = URI(normalized)
        require(uri.scheme == "http" || uri.scheme == "https") { "Ollama host must use HTTP or HTTPS" }
        require(uri.userInfo == null) { "Credentials in Ollama URLs are not allowed" }
        require(!uri.host.isNullOrBlank()) { "Ollama host is missing a hostname" }
        normalized
    }
}
