package com.banking.OnlineBankingWeb;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
public class OnlineBankingWebApplication {

	public static void main(String[] args) {
		SpringApplication.run(OnlineBankingWebApplication.class, args);
	}

}
