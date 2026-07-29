package com.codeit.mople.global.storage;

import org.springframework.web.multipart.MultipartFile;

public interface FileStorageService {
  String upload(MultipartFile file);
}
