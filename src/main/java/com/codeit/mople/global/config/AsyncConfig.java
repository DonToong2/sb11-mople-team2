package com.codeit.mople.global.config;

import java.util.concurrent.Executor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.retry.annotation.EnableRetry;
import org.springframework.scheduling.annotation.AsyncConfigurer;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

@Slf4j
@EnableAsync
@EnableRetry
@Configuration
public class AsyncConfig implements AsyncConfigurer {

  @Bean
  @Override
  public Executor getAsyncExecutor() {
    ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
    executor.setCorePoolSize(2);
    executor.setMaxPoolSize(10);
    executor.setQueueCapacity(500);
    executor.setThreadNamePrefix("async-notification-");
    executor.setWaitForTasksToCompleteOnShutdown(true);
    executor.setAwaitTerminationSeconds(30);
    executor.initialize();
    return executor;
  }

  @Bean
  public Executor kafkaRelayExecutor() {
    ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor(); // 스레드 풀 객체 생성
    executor.setCorePoolSize(2);                                    // 스레드 수
    executor.setMaxPoolSize(4);                                     // 큐가 꽉 찼을때 추가로 늘릴수 있는 최대 스레드 수
    executor.setQueueCapacity(1000);                                // 작업 대기하는 큐 크기
    executor.setThreadNamePrefix("kafka-relay-");                   // 스레드 이름
    executor.setWaitForTasksToCompleteOnShutdown(true);    // 앱이 종료될 때 큐에 남은 작업/실행 작업을 강제로 끊지 않고 끝날때까지 기다림
    executor.setAwaitTerminationSeconds(30);               // 기다리다가 30초가 지나도 안끝나면 강제종료
    executor.initialize();                                 // 설정 마무
    return executor;
  }

  @Override
  public org.springframework.aop.interceptor.AsyncUncaughtExceptionHandler getAsyncUncaughtExceptionHandler() {
    return (ex, method, params) ->
        log.error("비동기 처리 최종 실패 - method: {}", method.getName(), ex);
  }
}
