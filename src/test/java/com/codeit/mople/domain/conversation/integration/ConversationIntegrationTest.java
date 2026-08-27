package com.codeit.mople.domain.conversation.integration;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.codeit.mople.domain.auth.security.CustomUserDetails;
import com.codeit.mople.domain.conversation.entity.Conversation;
import com.codeit.mople.domain.conversation.repository.ConversationRepository;
import com.codeit.mople.domain.directmessage.document.DirectMessageDocument;
import com.codeit.mople.domain.directmessage.entity.DirectMessage;
import com.codeit.mople.domain.directmessage.repository.DirectMessageRepository;
import com.codeit.mople.domain.directmessage.repository.DirectMessageSearchRepository;
import com.codeit.mople.domain.user.entity.Role;
import com.codeit.mople.domain.user.entity.User;
import com.codeit.mople.domain.user.repository.UserRepository;
import com.codeit.mople.global.config.SecurityConfig;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

@Import(SecurityConfig.class)
@Transactional
@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc
public class ConversationIntegrationTest {

  @Autowired
  private MockMvc mockMvc;


  @Autowired
  private UserRepository userRepository;

  @Autowired
  private ConversationRepository conversationRepository;

  @Autowired
  private DirectMessageRepository directMessageRepository;

  @Autowired
  private DirectMessageSearchRepository directMessageSearchRepository;

  @Autowired
  private ElasticsearchOperations elasticsearchOperations;

  private CustomUserDetails userDetails;
  private User userA;
  private User userB;
  private Conversation testConversation;

  @BeforeEach
  void setUp() {
    userA = userRepository.save(User.createUser("testA@test.com", "12345678", "userA"));
    userB = userRepository.save(User.createUser("testB@test.com", "12345678", "userB"));

    userDetails = new CustomUserDetails(userA.getId(), Role.USER);

    testConversation = conversationRepository.save(Conversation.createConversation(userA, userB));
  }

  @AfterEach
  void tearDown() {
    directMessageSearchRepository.deleteAll();
    elasticsearchOperations.indexOps(DirectMessageDocument.class).refresh();
  }

  @Nested
  @DisplayName("대화방 목록 및 검색 조회")
  class GetMyConversations {

    @Test
    @DisplayName("성공: 엘라스틱서치를 통과하여 정확한 단어 검색 결과가 반환된다.")
    void search_success() throws Exception {
      // given
      saveMessageToElasticsearch(testConversation, userA, userB, "피자");

      // when & then
      mockMvc.perform(get("/api/conversations")
              .with(user(userDetails))
              .param("keywordLike", "피자")
              .param("limit", "20")
              .param("sortBy", "createdAt")
              .param("sortDirection", "DESCENDING")
              .accept(MediaType.APPLICATION_JSON))
          .andDo(print())
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.da"
              + "ta").isArray())
          .andExpect(jsonPath("$.data.length()").value(1))
          .andExpect(jsonPath("$.data[0].id").value(testConversation.getId().toString()))
          .andExpect(jsonPath("$.hasNext").value(false));
    }

    @Test
    @DisplayName("성공: 검색어가 1글자일 때도 정상적으로 검색된다.")
    void search_1_char_success() throws Exception {
      // given
      saveMessageToElasticsearch(testConversation, userA, userB, "헐");

      // when & then
      mockMvc.perform(get("/api/conversations")
              .with(user(userDetails))
              .param("keywordLike", "헐")
              .param("limit", "20")
              .accept(MediaType.APPLICATION_JSON))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.data.length()").value(1))
          .andExpect(jsonPath("$.data[0].id").value(testConversation.getId().toString()));
    }

    @Test
    @DisplayName("실패: 인증되지 않은 사용자 (401 에러)")
    void search_fail_unauthorized() throws Exception {
      // when & then
      mockMvc.perform(get("/api/conversations")
              .param("keywordLike", "피자")
              .param("limit", "20")
              .accept(MediaType.APPLICATION_JSON))
          .andExpect(status().isUnauthorized());
    }
  }

  // 유틸 메서드: 실제 서비스 흐름과 동일하게 DB 저장 후 -> ES 문서로 변환하여 저장
  private void saveMessageToElasticsearch(Conversation conversation, User sender, User receiver, String content) {
    DirectMessage message = DirectMessage.createMessage(conversation, sender, receiver, content);

    directMessageRepository.save(message);

    DirectMessageDocument document = DirectMessageDocument.from(message);
    directMessageSearchRepository.save(document);

    // 즉시 검색 가능하도록 강제 리프레시
    elasticsearchOperations.indexOps(DirectMessageDocument.class).refresh();
  }
}