package com.codeit.mople.domain.content.client;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "external-api.tmdb")
public record TmdbProperties(
    String baseUrl,
    String apiKey,
    String imageBaseUrl
) {

}
