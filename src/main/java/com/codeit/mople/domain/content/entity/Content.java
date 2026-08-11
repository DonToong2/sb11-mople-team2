package com.codeit.mople.domain.content.entity;


import com.codeit.mople.global.entity.BaseTimeEntity;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.util.ArrayList;
import java.util.List;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "contents",
    uniqueConstraints = @UniqueConstraint(
    name = "uk_contents_type_external_id",
    columnNames = {"type", "external_id"}))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Content extends BaseTimeEntity {

  @Enumerated(EnumType.STRING)
  @Column(name = "type", nullable = false)
  private ContentType type;

  @Column(name = "title", nullable = false)
  private String title;

  //긴 텍스트를 저장하기 위해 columnDefinition을 TEXT로 명시
  @Column(name = "description", columnDefinition = "TEXT")
  private String description;

  @Column(name = "thumbnailUrl")
  private String thumbnailUrl;

  @ElementCollection(fetch = FetchType.LAZY)
  @CollectionTable(name = "content_tags", joinColumns = @JoinColumn(name = "content_id"))
  @Column(name = "tags")
  private List<String> tags;

  // 동시성 문제를 개선하기 위해 총 별점 필드를 추가
  // 기존 리뷰 평균 별점은 서비스 로직에서 계산하여 응답으로 보냄
  // 단순 평균 별점으로 원자적 Update시 반올림으로 인한 정확한 계산이 어렵기 때문에 총 별점으로 필드 대체
  @Column(name = "rating_sum", nullable = false)
  private double ratingSum = 0.0;

  @Column(name = "review_count", nullable = false)
  private int reviewCount = 0; //리뷰 개수

  @Column(name = "watcher_count", nullable = false)
  private long watcherCount = 0L; //실시간 사용자 수

  @Column(name = "external_id")
  private String externalId;

  public Content(ContentType type, String title, String description, String thumbnailUrl, List<String> tags) {
    this.type = type;
    this.title = title;
    this.description = description;
    this.thumbnailUrl = thumbnailUrl;

    //생성 시 복사본(new ArrayList)을 사용하여 불변 리스트 참조 방지
    this.tags = tags != null ? new ArrayList<>(tags) : new ArrayList<>();
  }

  //외부 데이터 전용 생성자 오버로딩
  public Content(ContentType type, String title, String description, String thumbnailUrl, List<String> tags, String externalId) {
    this.type = type;
    this.title = title;
    this.description = description;
    this.thumbnailUrl = thumbnailUrl;
    this.tags = tags != null ? new ArrayList<>(tags) : new ArrayList<>();
    this.externalId = externalId;
  }

  //관리자가 콘텐츠의 기본 정보를 수정할 때 사용하는 메서드
  public void updateContentInfo(String title, String description, String thumbnailUrl, List<String> tags) {
    if (title != null) {
      this.title = title;
    }

    if (description != null) {
      this.description = description;
    }

    if (thumbnailUrl != null) {
      this.thumbnailUrl = thumbnailUrl;
    }

    //Hibernate 컬렉션 래퍼 유지를 위한 clear/addAll 적용
    if (tags != null) {
      this.tags.clear();
      this.tags.addAll(tags);
    }
  }

  //실시간 시청자 수 동기화용 메서드
  public void updateWatcherCount(long watcherCount) {
    this.watcherCount = watcherCount;
  }

  // 평균 평점을 구하는 로직
  public double calculateAverageRating() {
    if (reviewCount == 0) {
      return 0.0;
    }
    return ratingSum / reviewCount;
  }

}
