package com.codeit.mople.domain.auth.service;

import com.codeit.mople.domain.auth.dto.request.ResetPasswordRequest;
import com.codeit.mople.domain.auth.dto.request.SignInRequest;
import com.codeit.mople.domain.auth.dto.response.AuthTokens;
import com.codeit.mople.domain.auth.exception.AuthErrorCode;
import com.codeit.mople.domain.auth.exception.AuthException;
import com.codeit.mople.domain.user.dto.response.UserDto;
import com.codeit.mople.domain.user.entity.User;
import com.codeit.mople.domain.user.repository.UserRepository;
import com.codeit.mople.global.jwt.JwtProvider;
import io.jsonwebtoken.JwtException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {

  private static final String TEMPORARY_PASSWORD = "temporary1!!";
  private static final long TEMPORARY_PASSWORD_EXPIRATION_MINUTES = 3L;

  private final UserRepository userRepository;
  private final PasswordEncoder passwordEncoder;
  private final JwtProvider jwtProvider;

  @Transactional
  public AuthTokens signIn(SignInRequest request) {
    User user = userRepository.findByEmail(request.username())
        .orElseThrow(() -> new AuthException(AuthErrorCode.INVALID_CREDENTIALS));

    if(!isPasswordValid(request.password(), user)) {
      throw new AuthException(AuthErrorCode.INVALID_CREDENTIALS);
    }

    if(user.isLocked()) {
      throw new AuthException(AuthErrorCode.LOCKED_ACCOUNT);
    }

    long newSessionVersion = user.increaseSessionVersion();
    String accessToken = jwtProvider.createAccessToken(user.getId(), newSessionVersion);

    return issueRefreshToken(user, accessToken);
  }

  private boolean isPasswordValid(String rawPassword, User user) {
    if(passwordEncoder.matches(rawPassword, user.getPassword())) {
      return true;
    }
    return user.hasValidTemporaryPassword(Instant.now())
        && passwordEncoder.matches(rawPassword, user.getTemporaryPassword());
  }

  @Transactional
  public void resetPassword(ResetPasswordRequest request) {
    User user = userRepository.findByEmail(request.email())
        .orElseThrow(() -> new AuthException(AuthErrorCode.INVALID_CREDENTIALS));

    Instant expiresAt = Instant.now().plus(TEMPORARY_PASSWORD_EXPIRATION_MINUTES, ChronoUnit.MINUTES);
    user.issueTemporaryPassword(passwordEncoder.encode(TEMPORARY_PASSWORD), expiresAt);
    // TODO: 실제 이메일 발송 로직 -> 별도 이슈에서 구현 예정
    //  (SMTP 발신 계정 준비 후 진행)
  }

  @Transactional
  public AuthTokens refresh(String refreshToken) {
    UUID userId;
    try {
      userId = jwtProvider.getUserId(refreshToken);
    } catch (JwtException | IllegalArgumentException e) {
      throw new AuthException(AuthErrorCode.INVALID_TOKEN);
    }

    User user = userRepository.findById(userId)
        .orElseThrow(() -> new AuthException(AuthErrorCode.INVALID_TOKEN));

    if(!user.isRefreshTokenValid(refreshToken, Instant.now())) {
      throw new AuthException(AuthErrorCode.INVALID_TOKEN);
    }

    String newAccessToken = jwtProvider.createAccessToken(user.getId(), user.getSessionVersion());
    return issueRefreshToken(user, newAccessToken);
  }

  private AuthTokens issueRefreshToken(User user, String accessToken) {
    String refreshToken = jwtProvider.createRefreshToken(user.getId());
    Instant refreshExpiresAt = Instant.now().plusMillis(jwtProvider.getRefreshTokenExpiration());
    user.updateRefreshToken(refreshToken, refreshExpiresAt);

    return new AuthTokens(accessToken, refreshToken, refreshExpiresAt, UserDto.from(user));
  }
}
