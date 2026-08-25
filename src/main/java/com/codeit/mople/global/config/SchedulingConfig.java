package com.codeit.mople.global.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.EnableScheduling;

@Configuration
@Profile("!probe")
@EnableScheduling
public class SchedulingConfig {

}
