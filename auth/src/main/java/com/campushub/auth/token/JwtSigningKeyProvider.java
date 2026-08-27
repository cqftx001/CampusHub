package com.campushub.auth.token;

import com.campushub.auth.config.JwtProperties;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Base64;
import java.util.Objects;

@Component
public class JwtSigningKeyProvider {

    private final SecretKey signingKey;

    public JwtSigningKeyProvider(
            JwtProperties  jwtProperties
    ){
        Objects.requireNonNull(jwtProperties);

        this.signingKey = createSigningKey(jwtProperties.secret());
    }


    public SecretKey signingKey() {
        return signingKey;
    }

    // --- helper ---
    private SecretKey createSigningKey(String encodedSecret) {
        try{
            byte[] secret = Base64.getDecoder().decode(encodedSecret);

            return Keys.hmacShaKeyFor(secret);
        } catch (IllegalArgumentException | io.jsonwebtoken.security.SecurityException exception){
            throw new IllegalArgumentException("JWT secret must be a valid Base64-encoded key of at least 256 bits.", exception);
        }
    }
}
