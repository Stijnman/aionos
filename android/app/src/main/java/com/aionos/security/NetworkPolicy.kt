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
            // Must match res/xml/network_security_config.xml cleartext domain-config.
            // Android NSC cannot express RFC1918 CIDR; raw private-IP HTTP is rejected here
            // so callers use localhost / 10.0.2.2 / *.local or HTTPS instead.
            require(isCleartextAllowedHost(host)) {
                "Cleartext Ollama endpoints must be localhost, 10.0.2.2, or a .local name; use HTTPS for other hosts"
            }
        }
        normalized
    }

    /** Hosts permitted for HTTP cleartext by network_security_config.xml. */
    fun isCleartextAllowedHost(host: String): Boolean {
        val h = host.lowercase()
        return h == "localhost" || h == "127.0.0.1" || h == "::1" || h == "10.0.2.2" || h.endsWith(".local")
    }

    /** RFC1918 / loopback / .local — used for documentation and HTTPS LAN checks. */
    fun isLocalNetworkHost(host: String): Boolean {
        if (isCleartextAllowedHost(host)) return true
        val octets = host.split('.')
        if (octets.size != 4 || octets.any { it.toIntOrNull() == null }) return false
        val first = octets[0].toInt()
        val second = octets[1].toInt()
        return first == 10 || first == 192 && second == 168 || first == 172 && second in 16..31
    }
}
