package com.codeit.final_project;

import com.codeit.mople.MopleApplication;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(classes = MopleApplication.class)
@ActiveProfiles("test")
class FinalProjectApplicationTests {

  @Test
  void contextLoads() {
  }

}
