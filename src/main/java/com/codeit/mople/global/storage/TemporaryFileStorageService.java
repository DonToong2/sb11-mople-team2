package com.codeit.mople.global.storage;

import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Slf4j
@Service
@Profile("local") //로컬 환경에서만 이 구현체를 스프링 빈 등록
public class TemporaryFileStorageService implements FileStorageService {

  @Override
  public String upload(MultipartFile file) {
    String dummyUrl = "https://placeholder.mople.com/" + UUID.randomUUID();
    log.info("로컬 모드: 더미 이미지 URL이 생성되었습니다. [{}]", dummyUrl);
    return dummyUrl;
  }

  @Override
  public void delete(String fileUrl) {
    log.info("로컬 모드: 더미 이미지 삭제가 호출되었습니다. [{}]", fileUrl);
  }
}
