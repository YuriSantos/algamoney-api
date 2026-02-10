package com.example.algamoney.api.config;

import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.proc.SecurityContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.io.ClassPathResource;

import java.io.File;
import java.security.KeyStore;

@Configuration
@Profile("oauth-security")
public class JwtKeyStoreConfig {

    @Bean
    public JWKSet jwkSet() throws Exception {
        File file = new ClassPathResource("keystore/algamoney.jks").getFile();

        KeyStore keyStore = KeyStore.Builder.newInstance(file,
                new KeyStore.PasswordProtection("123456".toCharArray())
        ).getKeyStore();

        RSAKey rsaKey = RSAKey.load(
                keyStore,
                "algamoney",
                "123456".toCharArray()
        );

        return new JWKSet(rsaKey);
    }

    @Bean
    public JWKSource<SecurityContext> jwkSource(JWKSet jwkSet) {
        return (jwkSelector, securityContext) -> jwkSelector.select(jwkSet);
    }
}
