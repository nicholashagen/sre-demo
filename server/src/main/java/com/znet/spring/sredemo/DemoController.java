package com.znet.spring.sredemo;

import java.util.Random;

import javax.annotation.PostConstruct;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;

/**
 * Example controller that performs REST operations in order to
 * showcase slow latent requests intermixed with fast requests
 * in order to demonstrate the effect of the 95th and below
 * percentiles being less than the mean average.
 */
@Controller
public class DemoController {

	private static final Logger LOG = LoggerFactory.getLogger(DemoController.class);

	@Autowired private Random random;
	@Autowired private MeterRegistry registry;

	private Timer timer;

	@PostConstruct
	public void initialize() {

		// create the timer used to track the application
		// calls as a demo above and beyond the normal MVC
		// controller timers

		this.timer = Timer.builder("application.get")
				.publishPercentiles(0.5, 0.75, 0.90, 0.95, 0.99)
				.publishPercentileHistogram()
				.register(registry);
	}

	/**
	 * Example case that responds randomly between 5 and 10 ms.
	 */
	@RequestMapping("/foo")
	@ResponseBody
	public String foo() {

		doStuff(5, 10);
		return "foo";
	}

	/**
	 * Example case that responds randomly between 10 and 15 ms.
	 */
	@RequestMapping("/bar")
	@ResponseBody
	public String bar() {

		doStuff(10, 15);
		return "bar";
	}

	/**
	 * Example case that purposely responds slowly and randomly between 
	 * 1s and 1.5s.
	 */
	@RequestMapping("/eek")
	@ResponseBody
	public String eek() {

		doStuff(1000, 1500);
		return "eek";
	}

	/**
	 * Implementation method that is tracked time-wise and sleeps for the
	 * timed duration.
	 *
	 * @param start  The min time to sleep and wait
	 * @param end  The max time to sleep and wait
	 */
	private void doStuff(int start, int end) {

		int duration = start + random.nextInt(end - start);

		timer.record(() -> {
			try { Thread.sleep(duration); }
			catch (InterruptedException ie) {
				LOG.warn("interruption while sleeping for test");
			}
		});
	}
}
