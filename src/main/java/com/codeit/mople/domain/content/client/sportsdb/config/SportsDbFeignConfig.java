package com.codeit.mople.domain.content.client.sportsdb.config;

import feign.Logger;
import feign.Logger.Level;
import org.springframework.context.annotation.Bean;

public class SportsDbFeignConfig {

  //외부 API 통신 요청/응답의 모든 헤더와 바디를 로그로 출력
  @Bean
  public Logger.Level feignLoggerLevel() {
    return Level.FULL;
  }
}
