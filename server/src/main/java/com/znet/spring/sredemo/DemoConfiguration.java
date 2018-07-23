package com.znet.spring.sredemo;

import java.util.Random;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Example Spring configuration to demo creating configuration
 * beans, in this case a shared random number generator.
 */
@Configuration
public class DemoConfiguration {

	/**
	 * Create the shared random number generator to inject
	 * and use when creating random numbers.
	 */
	@Bean
	public Random createSharedRandom() {
		return new Random();
	}
}
