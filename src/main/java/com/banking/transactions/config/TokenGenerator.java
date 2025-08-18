package com.banking.transactions.config;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.temporal.ChronoUnit;


@Component
@RequiredArgsConstructor
@Slf4j
public class TokenGenerator {

    private final JwtEncoder jwtEncoder;

    @PostConstruct
    public void generateToken() {
        String scope = "USER";

        JwtClaimsSet claim = JwtClaimsSet.builder()
                .issuer("ebanking.com")
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plus(10, ChronoUnit.MINUTES))
                .claim("pid", "P-0123456789")
                .claim("roles", scope)
                .build();
        String token = jwtEncoder.encode(JwtEncoderParameters.from(claim)).getTokenValue();
        log.info("Generated JWT token: {}", token);
    }
}
