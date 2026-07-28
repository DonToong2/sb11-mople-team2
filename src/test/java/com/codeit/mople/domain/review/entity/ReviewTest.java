package com.codeit.mople.domain.review.entity;

import static org.assertj.core.api.Assertions.assertThat;

import com.codeit.mople.domain.user.entity.User;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

public class ReviewTest {

  @Test
  @DisplayName("리뷰 생성 성공")
  void create_success() {
    // given
    User user = User.createUser("test@test.com", "12345678", "test");
    Content content = Content.create("test", "test");
    User author = User.createUser("test@test.com", "12345678", "test");

    String text = "리뷰 내용";
    double rating = 5.0;

    // when
    Review review = Review.create(content, author, text, rating);

    // then
    assertThat(review.getContent()).isEqualTo(content);
    assertThat(review.getAuthor()).isEqualTo(author);
    assertThat(review.getText()).isEqualTo(text);
    assertThat(review.getRating()).isEqualTo(rating);

  }

}
