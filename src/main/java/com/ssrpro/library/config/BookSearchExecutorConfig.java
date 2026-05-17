package com.ssrpro.library.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Configuration
public class BookSearchExecutorConfig {

  @Bean(destroyMethod = "shutdown")
  public ExecutorService bookSearchExecutor() {
    return Executors.newFixedThreadPool(10);
  }
}
