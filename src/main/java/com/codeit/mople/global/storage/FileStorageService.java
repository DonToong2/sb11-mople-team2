package com.codeit.mople.global.storage;

import org.springframework.web.multipart.MultipartFile;

public interface FileStorageService {

  //파일 업로드 후 URL 반환
  String upload(MultipartFile file);

  //S3 연동 시 필요해질 파일 삭제 기능
  void delete(String fileUrl);
}
