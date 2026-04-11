package com.nazir.onlinesurveyservice.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

@ConfigurationProperties(prefix = "app")
@Getter
@Setter
public class AppProperties {

    private JwtProperties jwt = new JwtProperties();
    private PaginationProperties pagination = new PaginationProperties();
    private CorsProperties cors = new CorsProperties();

    @Getter
    @Setter
    public static class JwtProperties {
        private String secret;
        private long accessTokenExpirationMs  = 900_000L;      // 15 min
        private long refreshTokenExpirationMs = 604_800_000L;  // 7 days
    }

    @Getter
    @Setter
    public static class PaginationProperties {
        private int defaultPageSize = 20;
        private int maxPageSize     = 50;
    }

    @Getter
    @Setter
    public static class CorsProperties {
        private List<String> allowedOrigins = List.of("http://localhost:3000");
    }
}
