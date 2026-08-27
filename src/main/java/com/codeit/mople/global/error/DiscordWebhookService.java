package com.codeit.mople.global.error;

import java.time.Duration;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import jakarta.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Component
public class DiscordWebhookService {

  @Value("${discord.webhook.url}")
  private String webhookUrl;

  private final RestTemplate restTemplate;

  //디스코드 웹훅 알림 쿨다운 시간 설정(1분)
  private long lastSentTime = 0;
  private static final long COOLDOWN_TIME = 60000;

  //연결(2초) 및 응답(3초) 타임아웃을 설정
  public DiscordWebhookService(RestTemplateBuilder restTemplateBuilder) {
    this.restTemplate = restTemplateBuilder
        .connectTimeout(Duration.ofSeconds(2))
        .readTimeout(Duration.ofSeconds(3))
        .build();
  }

  public void sendErrorAlert(Exception e, HttpServletRequest request) {
    if (webhookUrl == null || webhookUrl.isEmpty()) {
      return;
    }

    //1분 이내에 동일한 연속 에러 알림 요청 시 발송 생략
    long currentTime = System.currentTimeMillis();
    if (currentTime - lastSentTime < COOLDOWN_TIME) {
      log.warn("디스코드 알림 쿨다운 중입니다. (에러: {})", e.getMessage());
      return;
    }
    lastSentTime = currentTime;

    //디스코드로 전송할 메시지 내용 구성
    String content = " [서버 장애 발생 알림] \n" +
        "- 요청 경로: `" + request.getMethod() + " " + request.getRequestURI() + "`\n" +
        "- 에러 메시지: `" + e.getMessage() + "`\n" +
        "- 에러 종류: `" + e.getClass().getSimpleName() + "`";

    Map<String, String> body = new HashMap<>();
    body.put("content", content);

    try {
      //디스코드로 메시지 전송
      restTemplate.postForEntity(webhookUrl, body, String.class);
    } catch (Exception ex) {
      log.error("디스코드 알림 전송에 실패했습니다.", ex);
    }
  }
}