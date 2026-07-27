package com.codeit.mople.global.storage;

import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

/**
 * TODO: Phase5 — AWS S3 연동으로 교체 예정
 * 지금은 실제 파일 저장 없이, 더미 URL만 생성해서 반환
 */
@Service
public class TemporaryFileStorageService implements FileStorageService {

  @Override
  public String upload(MultipartFile file) {
    return "https://placeholder.mople.com/" + UUID.randomUUID() + "-" + file.getOriginalFilename();
  }
}
