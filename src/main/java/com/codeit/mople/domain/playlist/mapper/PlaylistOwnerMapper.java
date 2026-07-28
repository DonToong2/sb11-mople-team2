package com.codeit.mople.domain.playlist.mapper;

import com.codeit.mople.domain.playlist.dto.response.PlaylistOwnerResponse;
import com.codeit.mople.domain.user.entity.User;
import org.springframework.stereotype.Component;

@Component
public class PlaylistOwnerMapper {

  public PlaylistOwnerResponse toResponse(User user) {
    return new PlaylistOwnerResponse(
        user.getId(),
        user.getName(),
        user.getProfileImageUrl()
    );
  }

}
