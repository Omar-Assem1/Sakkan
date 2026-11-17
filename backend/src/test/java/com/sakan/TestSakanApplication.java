package com.sakan;

import org.springframework.boot.SpringApplication;

public class TestSakanApplication {

	public static void main(String[] args) {
		SpringApplication.from(SakanApplication::main).with(TestcontainersConfiguration.class).run(args);
	}

}
