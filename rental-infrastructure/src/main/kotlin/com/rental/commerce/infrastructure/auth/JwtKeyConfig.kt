package com.rental.commerce.infrastructure.auth

import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.security.KeyPair
import java.security.KeyPairGenerator
import java.security.PrivateKey
import java.security.PublicKey

@Configuration
@EnableConfigurationProperties(JwtProperties::class)
class JwtKeyConfig {

    @Bean
    fun rsaKeyPair(): KeyPair {
        val keyPairGenerator = KeyPairGenerator.getInstance("RSA")
        keyPairGenerator.initialize(2048)
        return keyPairGenerator.generateKeyPair()
    }

    @Bean
    fun rsaPrivateKey(rsaKeyPair: KeyPair): PrivateKey {
        return rsaKeyPair.private
    }

    @Bean
    fun rsaPublicKey(rsaKeyPair: KeyPair): PublicKey {
        return rsaKeyPair.public
    }
}
