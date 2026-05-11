package com.example.smd;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.retry.annotation.EnableRetry;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableRetry
@EnableAsync
@EnableScheduling  // Kích hoạt @Scheduled - cần thiết để TaskReminderJob hoạt động
public class SmdApplication {

	public static void main(String[] args) {
		SpringApplication.run(SmdApplication.class, args);
	}

}
