package com.codeit.mople.domain.user.init;

import com.codeit.mople.domain.user.entity.Role;
import com.codeit.mople.domain.user.entity.User;
import com.codeit.mople.domain.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.ApplicationArguments;
import org.springframework.security.crypto.password.PasswordEncoder;

import org.springframework.dao.DataIntegrityViolationException;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AdminInitializerTest {

  @InjectMocks
  private AdminInitializer adminInitializer;

  @Mock
  private UserRepository userRepository;

  @Mock
  private PasswordEncoder passwordEncoder;

  @Mock
  private ApplicationArguments applicationArguments;

  @BeforeEach
  void setUp() {
    AdminProperties adminProperties = new AdminProperties("admin@mople.com", "Admin1234!", "관리자");
    org.springframework.test.util.ReflectionTestUtils.setField(
        adminInitializer, "adminProperties", adminProperties);
  }

  @Test
  void 어드민_계정이_없으면_생성한다() throws Exception {
    given(userRepository.existsByRole(Role.ADMIN)).willReturn(false);
    given(passwordEncoder.encode(anyString())).willReturn("encoded-password");

    adminInitializer.run(applicationArguments);

    verify(userRepository, times(1)).saveAndFlush(any(User.class));
  }

  @Test
  void 어드민_계정이_이미_있으면_생성하지_않는다() throws Exception {
    given(userRepository.existsByRole(Role.ADMIN)).willReturn(true);

    adminInitializer.run(applicationArguments);

    verify(userRepository, never()).save(any(User.class));
  }

  @Test
  void 동시_초기화_시_중복_예외가_발생해도_서버가_정상_시작된다() throws Exception {
    given(userRepository.existsByRole(Role.ADMIN)).willReturn(false);
    given(userRepository.existsByEmail("admin@mople.com")).willReturn(false);
    given(passwordEncoder.encode(anyString())).willReturn("encoded-password");
    given(userRepository.saveAndFlush(any(User.class))).willThrow(new DataIntegrityViolationException("duplicate key"));

    adminInitializer.run(applicationArguments);

    verify(userRepository, times(1)).saveAndFlush(any(User.class));
  }

  @Test
  void 어드민_이메일을_일반유저가_사용중이면_생성하지_않는다() throws Exception {
    given(userRepository.existsByRole(Role.ADMIN)).willReturn(false);
    given(userRepository.existsByEmail("admin@mople.com")).willReturn(true);

    adminInitializer.run(applicationArguments);

    verify(userRepository, never()).save(any(User.class));
  }
}
