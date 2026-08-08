package com.codeit.mople.domain.auth.security;

import com.codeit.mople.domain.user.entity.AuthProvider;
import java.util.Map;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class OAuthAttributes {

  private String providerId;
  private String email;
  private String name;
  private String picture;

  public static OAuthAttributes of(AuthProvider provider, Map<String, Object> attributes) {
    return switch (provider) {
      case GOOGLE -> ofGoogle(attributes);
      case KAKAO -> ofKakao(attributes);
      default -> throw new IllegalArgumentException("지원하지 않는 provider 입니다: " + provider);
    };
  }

  private static OAuthAttributes ofGoogle(Map<String, Object> attributes) {
    return OAuthAttributes.builder()
        .providerId((String) attributes.get("sub"))
        .email((String) attributes.get("email"))
        .name((String) attributes.get("name"))
        .picture((String) attributes.get("picture"))
        .build();
  }

  private static OAuthAttributes ofKakao(Map<String, Object> attributes) {
    Map<String, Object> kakaoAccount = (Map<String, Object>) attributes.get("kakao_account");
    Map<String, Object> profile = (Map<String, Object>) kakaoAccount.get("profile");
    String providerId = String.valueOf(attributes.get("id"));
    String email = (String) kakaoAccount.get("email");

    return OAuthAttributes.builder()
        .providerId(providerId)
        .email(email != null ? email : "kakao_" + providerId + "@kakao.local") // 비즈앱 전환 문제로 이메일 미제공. 임시 이메일 등록
        .name((String) profile.get("nickname"))
        .picture((String) profile.get("picture_image_url"))
        .build();
  }
}
