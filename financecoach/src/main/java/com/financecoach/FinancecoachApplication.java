package com.financecoach;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
@EnableScheduling
@SpringBootApplication
public class FinancecoachApplication {

	public static void main(String[] args) {
		SpringApplication.run(FinancecoachApplication.class, args);
	}

}
