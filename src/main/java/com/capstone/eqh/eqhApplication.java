package com.capstone.eqh;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@EnableJpaAuditing
@SpringBootApplication
public class eqhApplication {

	public static void main(String[] args) {
		SpringApplication.run(eqhApplication.class, args);
	}

}