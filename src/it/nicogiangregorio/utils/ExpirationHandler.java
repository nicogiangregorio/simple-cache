package it.nicogiangregorio.utils;

import java.util.Map;
import java.util.Map.Entry;
import java.util.Observable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;


/**
 * Handler for expiration time of cache elements
 * Be aware of the fact that every instance run a new thread. 
 *
 * USAGE:
 * 
 * final ExpirationHandler<String> test = new ExpirationHandler<>();
 *
 * test.register("test", 3, TimeUnit.SECONDS);
 * test.register("test2", 3, TimeUnit.SECONDS);
 *
 * test.stop();
 * 
 * IMPORTANT: you MUST use stop() method on handler termination, in order to avoid zombie threads!!!
 * 
 * @author Nico Giangregorio
 *
 * @param <K>
 */
public class ExpirationHandler<K> extends Observable {
	
	private boolean EXECUTION = true;
	private Map<K, Long> elements = new ConcurrentHashMap<>();
	private final ExecutorService executorService = Executors.newSingleThreadExecutor();
	
	public ExpirationHandler() {
		executorService.execute(new Runnable() {
		    public void run() {
		        execute();
		    }
		});
	}
	
	
	
	/**
	 * Register a new Element with an expiration time expressed with given time unit
	 * 
	 * @param toRegister : element to register
	 * @param delay : expiration time
	 * @param unit : time unit
	 */
	public void register(K toRegister, long delay, TimeUnit unit) {
		
		// Start event loop if not started yet, on first element insertion 
		if(!EXECUTION) EXECUTION = true;
		
		// if delay is == 0, then set 1000 years
		if (delay == 0L) {
			unit = TimeUnit.DAYS;
			delay = 365000;
		}
		
		long requiredDelay = TimeUnit.MILLISECONDS.convert(delay, unit) + System.currentTimeMillis();
		Long deadline = new Long(requiredDelay);
		elements.put(toRegister,deadline);
	}
	
	public void unregister(K registered) {
		elements.remove(registered);
		this.hasChanged();
		this.notifyObservers(registered);
	}

	/**
	 * Event loop 
	 * every cycle check if an element is eligible for removal from map
	 */
	protected void execute() {
		while (EXECUTION){
			for (Entry<K, Long> entry : elements.entrySet()) {
				if (System.currentTimeMillis() > entry.getValue()) {
					elements.remove(entry.getKey());
					this.setChanged();
					this.notifyObservers(entry.getKey());
					System.out.println("Timeout elapsed, removing: " + entry.getKey());
				}
			}
		}
	}
	
	/**
	 * stop the event loop and terminate executor service
	 */
	public void stop() {
		EXECUTION = false;
		executorService.shutdownNow();
		System.out.println("exiting with registered size: " + elements.size());
	}
}
