package com.codeit.mople.global.config;

import net.javacrumbs.shedlock.core.LockProvider;
import net.javacrumbs.shedlock.provider.redis.spring.RedisLockProvider;
import net.javacrumbs.shedlock.spring.annotation.EnableSchedulerLock;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;

@Configuration
@EnableSchedulerLock(defaultLockAtMostFor = "PT10M")
public class ShedLockConfig {

  @Bean
  public LockProvider lockProvider(
      RedisConnectionFactory connectionFactory,
      @Value("${spring.redis.namespace:mople:default}") String namespace) {
    //환경별로 락 키 이름 충돌 방지
    return new RedisLockProvider(connectionFactory, namespace + ":shedlock");
  }
}