package com.bookmania.bookmania.Configuration;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "jwt")
@Getter
@Setter
public class JwtProperties {

    private String secretKey;
    private long expiration;
    private RefreshToken refreshToken = new RefreshToken();

    @Getter
    @Setter
    public static class RefreshToken {
        private long expiration;
    }
}