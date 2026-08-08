package com.codeit.mople.domain.auth.security;

import com.codeit.mople.domain.auth.exception.AuthErrorCode;
import com.codeit.mople.domain.user.entity.AuthProvider;
import com.codeit.mople.domain.user.entity.User;
import com.codeit.mople.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CustomOAuth2UserService extends DefaultOAuth2UserService {

  private final UserRepository userRepository;

  public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
    OAuth2User oAuth2User = super.loadUser(userRequest);
    String registrationId = userRequest.getClientRegistration().getRegistrationId();
    return toCustomOAuth2User(oAuth2User, registrationId);
  }

  CustomOAuth2User toCustomOAuth2User(OAuth2User oAuth2User, String registrationId) {
    AuthProvider provider = AuthProvider.valueOf(registrationId.toUpperCase());
    OAuthAttributes attributes = OAuthAttributes.of(provider, oAuth2User.getAttributes());

    User user = userRepository.findByProviderAndProviderId(provider, attributes.getProviderId())
        .orElseGet(() -> createOAuthUser(attributes, provider));

    if(user.isLocked()) {
      throw new OAuth2AuthenticationException(new OAuth2Error(
          AuthErrorCode.LOCKED_ACCOUNT.getCode(), AuthErrorCode.LOCKED_ACCOUNT.getMessage(), null));
    }

    return new CustomOAuth2User(oAuth2User, user.getId());
  }

  private User createOAuthUser(OAuthAttributes attributes, AuthProvider provider) {
    try {
      return userRepository.save(
          User.createOAuthUser(attributes.getEmail(), attributes.getName(), attributes.getPicture(), provider, attributes.getProviderId()));
    } catch (DataIntegrityViolationException e) {
      return userRepository.findByProviderAndProviderId(provider, attributes.getProviderId())
          .orElseThrow(() -> e);
    }
  }
}
