package com.codeit.mople;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@ConfigurationPropertiesScan
@SpringBootApplication
public class MopleApplication {

  public static void main(String[] args) {
    SpringApplication.run(MopleApplication.class, args);
  }

}
