package com.ssrpro.library.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestTemplate;

import java.util.List;

/**
 * 정보나루(data4library.kr)는 Accept: application/json 요청 시 406을 반환한다.
 * JSON은 format=json 쿼리로 받고, Accept는 와일드카드(전체)로 보내야 한다.
 */
@Configuration
public class RestTemplateConfig {

    @Bean
    public RestTemplate restTemplate() {
        RestTemplate restTemplate = new RestTemplate();
        restTemplate.getInterceptors().add((request, body, execution) -> {
            request.getHeaders().setAccept(List.of(MediaType.ALL));
            return execution.execute(request, body);
        });
        return restTemplate;
    }
}
