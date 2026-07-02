package com.example.onlyoffice.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "onlyoffice")
public class OnlyOfficeProperties {

    /** URL the browser uses to load api.js from the Document Server. */
    private String docserverUrl;

    /** URL the backend uses to reach the Document Server (conversion / command service). */
    private String docserverInnerUrl;

    /** Shared JWT secret; must match the Document Server's JWT_SECRET. */
    private String jwtSecret;

    /** HTTP header the Document Server expects the JWT in for backend requests. */
    private String jwtHeader = "Authorization";
}
