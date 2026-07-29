package com.codeit.mople.domain.user.admin.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.codeit.mople.domain.user.entity.Role;
import com.codeit.mople.domain.user.entity.User;
import com.codeit.mople.domain.user.exception.UserErrorCode;
import com.codeit.mople.domain.user.repository.UserRepository;
import com.codeit.mople.global.error.CustomException;
import com.codeit.mople.global.event.UserForceLogoutEvent;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

@ExtendWith(MockitoExtension.class)
class AdminServiceTest {

  @InjectMocks
  private AdminService adminService;

  @Mock
  private UserRepository userRepository;

  @Mock
  private ApplicationEventPublisher eventPublisher;

  @Captor
  private ArgumentCaptor<UserForceLogoutEvent> eventCaptor;

  UUID userId;
  User user;

  @BeforeEach
  void setUp() {
    userId = UUID.randomUUID();
    user = User.createUser("test@test.com", "encoded", "테스터");
  }

  @Nested
  @DisplayName("사용자 권한 변경")
  class ChangeUserRole {

    @Test
    @DisplayName("USER을 ADMIN으로 권한 변경 할 수 있고 강제 로그아웃 이벤트를 발행한다")
    void USER을 ADMIN으로 권한 변경 할 수 있고 강제 로그아웃 이벤트를 발행한다() {
      // given
      given(userRepository.findById(userId)).willReturn(Optional.of(user));

      // when
      adminService.changeUserRole(userId, "ADMIN");

      // then
      assertThat(user.getRole()).isEqualTo(Role.ADMIN);
      verify(eventPublisher).publishEvent(eventCaptor.capture());
      assertThat(eventCaptor.getValue().userId()).isEqualTo(userId);
    }

    @Test
    @DisplayName("ADMIN을 USER로 강등할 수 있고 강제 로그아웃 이벤트를 발행한다")
    void ADMIN을_USER로_강등할_수_있고_강제_로그아웃_이벤트를_발행한다() {
      // given
      User admin = User.createAdmin("admin@test.com", "encoded", "어드민");
      given(userRepository.findById(userId)).willReturn(Optional.of(admin));

      // when
      adminService.changeUserRole(userId, "USER");

      // then
      assertThat(admin.getRole()).isEqualTo(Role.USER);
      verify(eventPublisher).publishEvent(eventCaptor.capture());
      assertThat(eventCaptor.getValue().userId()).isEqualTo(userId);
    }

    @Test
    @DisplayName("존재하지 않는 사용자 id로 요청하면 USER_NOT_FOUND 예외가 발생한다")
    void 존재하지_않는_사용자_id로_요청하면_USER_NOT_FOUND_예외가_발생한다() {
      // given
      given(userRepository.findById(userId)).willReturn(Optional.empty());

      // when & then
      assertThatExceptionOfType(CustomException.class)
          .isThrownBy(() -> adminService.changeUserRole(userId, "ADMIN"))
          .extracting(CustomException::getErrorCode)
          .isEqualTo(UserErrorCode.USER_NOT_FOUND);

      verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    @DisplayName("유효하지 않은 role 값이면 IllegalArgumentException이 발생한다")
    void 유효하지_않은_role_값이면_IllegalArgumentException이_발생한다() {
      // Role.valueOf()가 DB 조회 전에 먼저 실행되므로 별도 스텁 불필요
      assertThatExceptionOfType(IllegalArgumentException.class)
          .isThrownBy(() -> adminService.changeUserRole(userId, "INVALID_ROLE"));

      verify(eventPublisher, never()).publishEvent(any());
    }
  }
}
