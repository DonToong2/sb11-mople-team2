package com.codeit.mople.domain.content.client.sportsdb.config;

import feign.Logger;
import feign.Logger.Level;
import feign.RequestInterceptor;
import feign.codec.ErrorDecoder;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.context.annotation.Bean;

public class SportsDbFeignConfig {

  @Bean
  public Logger.Level feignLoggerLevel() {
    return Level.NONE; //API 키(민감정보)가 로그에 출력되지 않도록 로깅 비활성화
  }

  //API 호출 횟수 카운터
  @Bean
  public RequestInterceptor sportsDbRequestInterceptor(MeterRegistry meterRegistry) {
    Counter sportsDbCallCounter = Counter.builder("mople.external.api.call.count")
        .tag("provider", "sportsdb")
        .description("SportsDB API 호출 횟수")
        .register(meterRegistry);

    return template -> sportsDbCallCounter.increment();
  }

  //SportsDB 에러 횟수 카운터
  @Bean
  public ErrorDecoder sportsDbErrorDecoder(MeterRegistry meterRegistry) {
    Counter errorCounter = Counter.builder("mople.external.api.error.count")
        .tag("provider", "sportsdb") //태그를 sportsdb로 지정
        .description("SportsDB API 호출 실패 횟수")
        .register(meterRegistry);

    return (methodKey, response) -> {
      //에러 응답(4xx, 5xx)이 오면 카운터 1 증가
      errorCounter.increment();

      return new ErrorDecoder.Default().decode(methodKey, response);
    };
  }
}