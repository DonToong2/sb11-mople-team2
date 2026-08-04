package com.codeit.mople.domain.content.client;

import feign.RequestInterceptor;
import feign.Retryer;
import feign.codec.ErrorDecoder;
import org.springframework.context.annotation.Bean;

public class TmdbFeignConfig {

  private static final String LANGUAGE = "ko-KR";

  @Bean
  public ErrorDecoder tmdbErrorDecoder() {
    return new TmdbErrorDecoder();
  }

  @Bean
  public Retryer tmdbRetryer() {
    return new Retryer.Default(
        1000L,
        3000L,
        3);
  }

  @Bean
  public RequestInterceptor tmdbRequestInterceptor(TmdbProperties properties) {
    return template -> {
      template.header("Authorization", "Bearer " + properties.apiKey());
      template.query("language", LANGUAGE);
    };
  }
}

