package com.znet.spring.sredemo;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

/**
 * Main application used to bootstrap and configure the application.
 */
@SpringBootApplication
@ComponentScan("com.znet.spring.sredemo")
public class SreDemoApplication {

	public static void main(String[] args) {
		SpringApplication.run(SreDemoApplication.class, args);
	}
}
