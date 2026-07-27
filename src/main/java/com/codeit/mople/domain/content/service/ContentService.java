package com.codeit.mople.domain.content.service;

import com.codeit.mople.domain.content.dto.ContentCreateRequest;
import com.codeit.mople.domain.content.dto.ContentPageResponse;
import com.codeit.mople.domain.content.dto.ContentResponse;
import java.util.UUID;
import org.springframework.web.multipart.MultipartFile;

public interface ContentService {

  ContentResponse createContent(UUID adminId, ContentCreateRequest request, MultipartFile thumbnail);
  ContentPageResponse getContents(int limit, String sortDirection, String sortBy);
}
