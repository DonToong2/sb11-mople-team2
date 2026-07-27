package com.codeit.mople.domain.user.admin.service;

import com.codeit.mople.domain.user.entity.Role;
import com.codeit.mople.domain.user.entity.User;
import com.codeit.mople.domain.user.exception.UserErrorCode;
import com.codeit.mople.domain.user.repository.UserRepository;
import com.codeit.mople.global.error.CustomException;
import com.codeit.mople.global.event.UserForceLogoutEvent;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class AdminServiceTest {

  @InjectMocks
  private AdminService adminService;

  @Mock
  private UserRepository userRepository;

  @Mock
  private ApplicationEventPublisher eventPublisher;

  @Test
  void 권한_변경_성공_및_강제로그아웃_이벤트_발행() {
    UUID userId = UUID.randomUUID();
    User user = User.createUser("test@test.com", "encoded", "테스터");
    given(userRepository.findById(userId)).willReturn(Optional.of(user));

    adminService.changeUserRole(userId, "ADMIN");

    assertThat(user.getRole()).isEqualTo(Role.ADMIN);
    ArgumentCaptor<UserForceLogoutEvent> captor = ArgumentCaptor.forClass(UserForceLogoutEvent.class);
    verify(eventPublisher).publishEvent(captor.capture());
    assertThat(captor.getValue().userId()).isEqualTo(userId);
  }

  @Test
  void 존재하지_않는_사용자_권한_변경_시_예외() {
    UUID userId = UUID.randomUUID();
    given(userRepository.findById(userId)).willReturn(Optional.empty());

    assertThatThrownBy(() -> adminService.changeUserRole(userId, "ADMIN"))
        .isInstanceOf(CustomException.class)
        .satisfies(e -> assertThat(((CustomException) e).getErrorCode())
            .isEqualTo(UserErrorCode.USER_NOT_FOUND));
  }

  @Test
  void 어드민을_일반유저로_강등할_수_있다() {
    UUID userId = UUID.randomUUID();
    User admin = User.createAdmin("admin@test.com", "encoded", "어드민");
    given(userRepository.findById(userId)).willReturn(Optional.of(admin));

    adminService.changeUserRole(userId, "USER");

    assertThat(admin.getRole()).isEqualTo(Role.USER);
  }
}
