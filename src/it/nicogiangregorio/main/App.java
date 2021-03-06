package it.nicogiangregorio.main;

import it.nicogiangregorio.Cache;

import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

public class App {
	static Logger logger = Logger.getLogger("test");
	
	public static void main(String[] args) {
		Cache<String, String> cache = new Cache<>(new TestComputable());
		
		try {
			System.out.println("Compute A: " + cache.compute("A", 1, TimeUnit.SECONDS));
			System.out.println("Compute B: " + cache.compute("B", 1, TimeUnit.SECONDS));
			System.out.println("Compute A (retrieve cache): " + cache.compute("A", 1, TimeUnit.SECONDS));
			System.out.println("\n=== Clearing cache ====");
			cache.clear();
			System.out.println("Compute A (different value from the previous one): " + cache.compute("A", 1, TimeUnit.SECONDS));
			System.out.println("Compute B (different value from the previous one): " + cache.compute("B", 1, TimeUnit.SECONDS));
			
			// Wait for automatic cache clean up
			System.out.println("\n=== Automatic cache purging ====");
			CountDownLatch latch = new CountDownLatch(1);
			latch.await(2, TimeUnit.SECONDS);
			
			System.out.println("Compute A (different value from the previous one): " + cache.compute("A", 1, TimeUnit.SECONDS));
			System.out.println("Compute B (different value from the previous one): " + cache.compute("B", 1, TimeUnit.SECONDS));
			
			System.out.println("\n=== Get all elements from cache ====");
			for (Map.Entry<String, String> singlevalue : cache.getAll().entrySet()) {
				System.out.println("key: " + singlevalue.getKey() + " - value: " + singlevalue.getValue());
			}
			
			System.out.println("Remove element A from cache\n");
			cache.remove("A");
			
			System.out.println("\n=== Get all elements from cache ====");
			for (Map.Entry<String, String> singlevalue : cache.getAll().entrySet()) {
				System.out.println("key: " + singlevalue.getKey() + " - value: " + singlevalue.getValue());
			}
			
			CountDownLatch latch2 = new CountDownLatch(1);
			latch2.await(2, TimeUnit.SECONDS);
			
			System.out.println("\n=== Waiting for timeout elapsed ====");
			System.out.println("cache size: " + cache.size());
			
			cache.terminate();
			
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}
}
