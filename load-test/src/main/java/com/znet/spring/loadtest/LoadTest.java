package com.znet.spring.loadtest;

import java.util.Timer;

import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.Response;

/**
 * Load test application class that starts the application.
 * The load test sets up a continuous 5 RPS along with a 
 * burst of traffic every minute to simulate constant peaks.
 *
 * Typically this class would be written in JMeter, but it's
 * written here as a demo of coding.
 */
public class LoadTest {

	public static void main(String... args) throws Exception {

		// wait 10s before starting to allow systems to bootstrap
		System.out.println("Starting...");
		Thread.sleep(10000);

		// schedule bursty traffic
		// wait 15s, then schedule every 60s
		new Timer("scheduler").schedule(new Processor(), 15000, 60000);

		// schedule normal traffic at 5 RPS
		OkHttpClient client = new OkHttpClient();
		while (true) {
			Thread.sleep(200);

			long start = System.nanoTime();
			Request request = new Request.Builder().url("http://sre-demo:8080/foo").build();
			Response response = client.newCall(request).execute();
			long end = System.nanoTime();
			long duration = (int) ((end - start) / 1000000.0);

			System.out.println("SUCCESS (" + duration + " ms): " + response.body().string());
		}
	}
}

