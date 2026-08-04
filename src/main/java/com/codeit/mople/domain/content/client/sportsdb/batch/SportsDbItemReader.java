package com.codeit.mople.domain.content.client.sportsdb.batch;

import com.codeit.mople.domain.content.client.sportsdb.SportsDbFeignClient;
import com.codeit.mople.domain.content.client.sportsdb.dto.SportsDbEventDto;
import com.codeit.mople.domain.content.client.sportsdb.dto.SportsDbEventResponse;
import java.time.LocalDate;
import java.util.Iterator;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.item.ItemReader;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class SportsDbItemReader implements ItemReader<SportsDbEventDto> {

  private final SportsDbFeignClient feignClient;
  private Iterator<SportsDbEventDto> eventIterator;

  //Feign Client를 호출하여 경기 데이터 읽기 기능 구현
  @Override
  public SportsDbEventDto read() {
    //싫행 시 최초 1회만 API를 호출하여 데이터를 메모리에 로드
    if (eventIterator == null) {
      String today = LocalDate.now().toString(); //오늘 날짜 기준 데이터 조회
      log.info("SportsDB API 조회 시작 - 일자: {}", today);

      SportsDbEventResponse response = feignClient.getEventsByDate(today, "Soccer");

      if (response != null && response.events() != null) {
        eventIterator = response.events().iterator();
        log.info("조회된 경기 수: {}건", response.events().size());
      } else {
        eventIterator = List.<SportsDbEventDto>of().iterator();
        log.info("조회된 경기가 없습니다.");
      }
    }

    //Iterator를 통해 Processor로 데이터를 한 건씩 전달(더 없으면 null 반환)
    if (eventIterator.hasNext()) {
      return eventIterator.next();
    }
    return  null;
  }
}
