package com.codeit.mople.global.config;

import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

@Slf4j
@Configuration
@ConditionalOnProperty(prefix = "kafka", name = "enabled", havingValue = "true")
public class KafkaConfig {

  public KafkaConfig(@Value("${spring.kafka.bootstrap-servers:}") String bootstrapServers) {
    Assert.state(StringUtils.hasText(bootstrapServers),
        "kafka.enabled=true 이지만 spring.kafka.bootstrap-servers 가 비어있습니다. KAFKA_BOOTSTRAP_SERVERS 를 설정하세요.");
    log.info("Kafka 이벤트 발행 활성화: bootstrapServers={}", bootstrapServers);
  }

  @Bean
  public Executor kafkaRelayExecutor() {
    ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor(); // 스레드 풀 객체 생성
    executor.setCorePoolSize(4);                                    // 스레드 수
    executor.setMaxPoolSize(4);                                     // 큐가 꽉 찼을때 추가로 늘릴수 있는 최대 스레드 수
    executor.setQueueCapacity(1000);                                // 작업 대기하는 큐 크기
    executor.setThreadNamePrefix("kafka-relay-");                   // 스레드 이름
    executor.setWaitForTasksToCompleteOnShutdown(true);    // 앱이 종료될 때 큐에 남은 작업/실행 작업을 강제로 끊지 않고 끝날때까지 기다림
    executor.setAwaitTerminationSeconds(30);               // 기다리다가 30초가 지나도 안끝나면 강제종료
    // 스레트 풀이 작업을 더 받을 수 없을 때 어떻게 할지
    executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
    executor.initialize();                                 // 설정 마무리
    return executor;
  }
}
