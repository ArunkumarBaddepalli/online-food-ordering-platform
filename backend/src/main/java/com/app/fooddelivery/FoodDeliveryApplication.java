package com.app.fooddelivery;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
@EnableAsync
@org.springframework.boot.autoconfigure.domain.EntityScan("com.app.fooddelivery.model")
@org.springframework.data.jpa.repository.config.EnableJpaRepositories("com.app.fooddelivery.repository")
public class FoodDeliveryApplication {

	public static void main(String[] args) {
		SpringApplication.run(FoodDeliveryApplication.class, args);
	}

}
