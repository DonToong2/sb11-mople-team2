package com.codeit.mople.domain.content.dto;

import java.util.List;

public record ContentUpdateRequest(
    String title,
    String description,
    List<String> tags
) {

}
