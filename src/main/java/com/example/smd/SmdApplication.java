package com.example.smd;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.retry.annotation.EnableRetry;

@SpringBootApplication
@EnableRetry
public class SmdApplication {

	public static void main(String[] args) {
		SpringApplication.run(SmdApplication.class, args);
	}

}
