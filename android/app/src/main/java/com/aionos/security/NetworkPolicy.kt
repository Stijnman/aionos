package com.aionos.security

import java.net.URI

object NetworkPolicy {
    fun validateOllamaHost(value: String): Result<String> = runCatching {
        val normalized = value.trim().removeSuffix("/")
        val uri = URI(normalized)
        val scheme = uri.scheme?.lowercase()
        require(scheme == "http" || scheme == "https") { "Ollama host must use HTTP or HTTPS" }
        require(uri.userInfo == null) { "Credentials in Ollama URLs are not allowed" }
        val host = uri.host?.lowercase().orEmpty()
        require(host.isNotBlank()) { "Ollama host is missing a hostname" }
        require(uri.query == null && uri.fragment == null) { "Ollama host cannot contain a query or fragment" }
        require(uri.port == -1 || uri.port in 1..65535) { "Ollama port is invalid" }
        if (scheme == "http") {
            require(isLocalHost(host)) { "Cleartext Ollama endpoints must be local-network addresses" }
        }
        normalized
    }

    private fun isLocalHost(host: String): Boolean {
        if (host == "localhost" || host == "127.0.0.1" || host == "::1" || host.endsWith(".local")) return true
        val octets = host.split('.')
        if (octets.size != 4 || octets.any { it.toIntOrNull() == null }) return false
        val first = octets[0].toInt()
        val second = octets[1].toInt()
        return first == 10 || first == 192 && second == 168 || first == 172 && second in 16..31
    }
}
