package it.nicogiangregorio;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class Cache<S, T> implements Computable<S, T> {
	private final ConcurrentMap<S, Future<T>> cache = new ConcurrentHashMap<S, Future<T>>();
	private final Computable<S, T> c;

	/**
	 * 
	 * @param c : computable action
	 * @param timeout : timeout of cache, first it will be erased, 0 for infinite
	 */
	public Cache(Computable<S, T> c, long timeout, TimeUnit timeUnit) {
		this.c = c;
		ScheduledExecutorService timeoutHandler = Executors.newSingleThreadScheduledExecutor();
		
		if (timeout == 0) {
			timeUnit = TimeUnit.DAYS;
			timeout = 365000; // I don't think this will be useful anymore :)
		}
		
		timeoutHandler.schedule(new Runnable() {
			@Override
			public void run() {
				cache.clear();
			}
		}, timeout, timeUnit);
	}
	
	/**
	 * Make elaboration or retrieve element from cache 
	 */
	public T compute(final S arg) throws InterruptedException {
		while (true) {
			Future<T> f = cache.get(arg);
			if (f == null) {
				Callable<T> eval = new Callable<T>() {
					public T call() throws InterruptedException {
						return c.compute(arg);
					}
				};
				FutureTask<T> ft = new FutureTask<T>(eval);
				f = cache.putIfAbsent(arg, ft);
				if (f == null) {
					f = ft;
					ft.run();
				}
			}
			try {
				return f.get();
			} catch (CancellationException e) {
				cache.remove(arg, f);
			} catch (ExecutionException e) {
				launderThrowable(e.getCause());
			}
		}
	}
	
	/**
	 * Clear cache
	 */
	public void clear() {
		cache.clear();
	}
	
	/**
	 * Remove element from cache
	 */
	public void remove(final S arg) {
		cache.remove(arg);
	}
	
	/**
	 * Get all elements from cache
	 * @throws InterruptedException 
	 */
	public Map<S, T> getAll() throws InterruptedException {
		Map<S, T> extCache = new HashMap<>();
		
		for (Map.Entry<S, Future<T>> singleKey : cache.entrySet()) {
			try {
				extCache.put(singleKey.getKey(), singleKey.getValue().get());
			} catch (ExecutionException e) {
				launderThrowable(e.getCause());
			}
		}
		return extCache;
	}

	/**
	 * If the Throwable is an Error, throw it; if it is a RuntimeException
	 * return it, otherwise throw IllegalStateException
	 */
	public static RuntimeException launderThrowable(Throwable t) {
		if (t instanceof RuntimeException)
			return (RuntimeException) t;
		else if (t instanceof Error)
			throw (Error) t;
		else
			throw new IllegalStateException("Not unchecked", t);
	}
}