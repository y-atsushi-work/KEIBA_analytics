package com.keiba.analytics;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling; // ★追加

@SpringBootApplication
@EnableScheduling // ★このアノテーションを追加してタイマー機能を有効化します
public class KeibaApplication {

    public static void main(String[] args) {
        SpringApplication.run(KeibaApplication.class, args);
    }
}