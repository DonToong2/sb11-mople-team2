package com.codeit.mople.global.config;

import com.codeit.mople.domain.content.repository.ContentRepository;
import com.codeit.mople.domain.content.repository.search.ContentDocument;
import com.codeit.mople.domain.content.repository.search.ContentSearchRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@RequiredArgsConstructor
public class SearchConfig {

  private final ContentRepository contentRepository;
  private final ContentSearchRepository contentSearchRepository;

  @Bean
  public CommandLineRunner indexContents() {
    return args -> {
      var documents = contentRepository.findAll().stream()
          .map(content -> new ContentDocument(
              content.getId(),
              content.getTitle()
          ))
          .toList();

      contentSearchRepository.saveAll(documents);
    };
  }
}