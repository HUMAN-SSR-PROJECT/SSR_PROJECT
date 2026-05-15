package com.ssrpro.library.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.storage.Bucket;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.cloud.StorageClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;

import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.io.InputStream;

@Configuration
public class FirebaseConfig {

    @Value("${firebase.config.path}")
    private String configPath;

    @Value("${firebase.storage.bucket}")
    private String storageBucket;

    @PostConstruct  // 초기화 로직(프로그램 켜지고 한 번 실행)
    public void init() {
        try {
            String fileName = configPath.contains("/") ?
                    configPath.substring(configPath.lastIndexOf("/") + 1) : configPath;

            ClassPathResource resource = new ClassPathResource(fileName);
            InputStream serviceAccount = resource.getInputStream();  // resources에 넣어둔 JSON 키 파일 열어 내용 읽음

            FirebaseOptions options = FirebaseOptions.builder()
                    .setCredentials(GoogleCredentials.fromStream(serviceAccount))
                    .setStorageBucket(storageBucket) // 저장 주소가 storageBucket
                    .build();

            if (FirebaseApp.getApps().isEmpty()) {  // 중복 연결 막아줌
                FirebaseApp.initializeApp(options);
            }
        } catch (IOException e) {
            System.err.println("Firebase 초기화 실패: " + e.getMessage());
        }
    }

    @Bean  // 저장소
    public Bucket storageBucket() {  // 접속 허가 나면 bucket 빌려옴
        return StorageClient.getInstance(FirebaseApp.getInstance()).bucket(storageBucket);
    }
}