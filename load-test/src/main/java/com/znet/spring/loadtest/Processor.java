package com.znet.spring.loadtest;

import java.util.TimerTask;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.Response;

/**
 * Processor class used to perform the timer every minute
 * by sending a large burst of traffic of 1,000 requests
 * spread out between 960 fast requests and 40 slow requests
 * in order to drive the 95th percentile as fast and ensure
 * the mean is still weighted negatively above.
 */
public class Processor extends TimerTask {

	OkHttpClient client = new OkHttpClient();

	public void run() {

		// use an executor service to run the 1,000 requests in
		// parallel with 50 clients
		ExecutorService executor = Executors.newFixedThreadPool(50);
		for (int i = 0; i < 1000; i++) {

			// splits the requests between the 3 known endpoints to
			// ensure mostly fast, some slighly slower and the
			// remaining 40 really slow
			int index = i;
			String endpoint = (
				(i < 700) ? "/foo" :
				(i < 960) ? "/bar" :
				"/eek"
			);

			executor.submit(() -> {
				String url = "http://sre-demo:8080" + endpoint + "?index=" + index;

				long start = System.nanoTime();
				try {
					Request request = new Request.Builder().url(url).build();
					Response response = client.newCall(request).execute();
					long end = System.nanoTime();
					long duration = (int) ((end - start) / 1000000.0);

					System.out.println("SUCCESS (" + duration + " ms): " + url + ": " + response.body().string());
				}
				catch (Exception exception) {
					System.out.println("ERROR: " + url);
				}
			});
		}

		// shutdown and wait for all requests to complete
		executor.shutdown();
		try { executor.awaitTermination(10, TimeUnit.SECONDS); }
		catch (Exception exception) {
			System.out.println("FAILED TO TERMINATE");
		}
	}
}
