package com.codeit.mople.global.storage;

import io.awspring.cloud.s3.ObjectMetadata;
import io.awspring.cloud.s3.S3Resource;
import io.awspring.cloud.s3.S3Template;
import java.io.IOException;
import java.io.InputStream;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

@Slf4j
@Service
@Profile({"dev", "prod"}) //dev 또는 prod 환경일 때만 활성화
@RequiredArgsConstructor
public class S3FileStorageService implements FileStorageService{

  private final S3Template s3Template;

  @Value("${cloud.aws.s3.bucket}")
  private String bucket;

  @Override
  public String upload(MultipartFile file) {
    //파일 유효성 검사
    if (file == null || file.isEmpty()) {
      throw new IllegalArgumentException("업로드할 파일이 비어 있습니다.");
    }

    //고유한 파일명 생성
    String originalFilename = file.getOriginalFilename();
    String extension = StringUtils.getFilenameExtension(originalFilename);
    String savedFilename = UUID.randomUUID() + "." + extension;

    try (InputStream inputStream = file.getInputStream()) {
      //AWS S3에 올릴 때 파일의 메타데이터(캐시 정책, MIME 타입) 설정
      ObjectMetadata metadata = ObjectMetadata.builder()
          .contentType(file.getContentType())
          .cacheControl("max-age=31536000") //브라우저가 1년(31536000초) 동안 캐시하도록 설정
          .build();

      //S3 업로드 실행(metadata 파라미터 추가)
      S3Resource resource = s3Template.upload(bucket, savedFilename, inputStream, metadata);

      //업로드된 파일의 최종 S3 URL 반환
      String fileUrl = resource.getURL().toString();
      log.info("S3 파일 업로드 성공: {}", fileUrl);
      return fileUrl;

    } catch (IOException e) {
      log.error("S3 파일 업로드 실패", e);
      throw new RuntimeException("S3 파일 업로드 중 오류가 발생했습니다.", e);
    }
  }

  @Override
  public void delete(String fileUrl) {
    if (!StringUtils.hasText(fileUrl)) {
      return;
    }

    try {
      //URL의 마지막 부분(파일명)만 추출해서 객체 삭제
      String objectKey = fileUrl.substring(fileUrl.lastIndexOf("/") + 1);
      s3Template.deleteObject(bucket, objectKey);
      log.info("S3 파일 삭제 성공: {}", objectKey);
    } catch (Exception e) {
      log.error("S3 파일 삭제 실패: {}", fileUrl, e);
    }
  }
}
