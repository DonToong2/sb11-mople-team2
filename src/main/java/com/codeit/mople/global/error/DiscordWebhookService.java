package com.codeit.mople.global.error;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

@Slf4j
@Component
public class DiscordWebhookService {

  @Value("${discord.webhook.url}")
  private String webhookUrl;

  private final RestTemplate restTemplate;

  //동시성 제어를 위한 AtomicLong 사용 및 디스코드 웹훅 알림 쿨다운 시간 설정(1분)
  private final AtomicLong lastSentTime = new AtomicLong(0);
  private static final long COOLDOWN_TIME = 60000;

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

    long currentTime = System.currentTimeMillis();

    //원자적으로 쿨다운 통과 여부 검사
    //현재 시간과 마지막 전송 시간을 비교하여 쿨다운이 지났을 때만 업데이트
    boolean canSend = lastSentTime.updateAndGet(last ->
        (currentTime - last >= COOLDOWN_TIME) ? currentTime : last
    ) == currentTime;

    if (!canSend) {
      log.warn("디스코드 알림 쿨다운 중입니다. (에러: {})", e.getMessage());
      return;
    }

    String content = " [서버 장애 발생 알림] \n" +
        "- 요청 경로: `" + request.getMethod() + " " + request.getRequestURI() + "`\n" +
        "- 에러 메시지: `" + e.getMessage() + "`\n" +
        "- 에러 종류: `" + e.getClass().getSimpleName() + "`";

    Map<String, String> body = new HashMap<>();
    body.put("content", content);

    try {
      restTemplate.postForEntity(webhookUrl, body, String.class);
    } catch (Exception ex) {
      log.error("디스코드 알림 전송에 실패했습니다.", ex);
      //전송 실패 시 쿨다운을 초기화하여 다음 에러가 바로 알림을 보낼 수 있게 함
      lastSentTime.set(0);
    }
  }
}