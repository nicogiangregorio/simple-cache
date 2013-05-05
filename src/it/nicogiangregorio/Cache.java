package it.nicogiangregorio;

import it.nicogiangregorio.utils.ExpirationHandler;

import java.util.HashMap;
import java.util.Map;
import java.util.Observable;
import java.util.Observer;
import java.util.concurrent.Callable;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;

/**
 * 
 * Simple cache with optional expiration time on each element inserted 
 * 
 * @author Nico giangregorio
 *
 * @param <S> : element to be cached 
 * @param <T> : Task to be executed
 */
public class Cache<S, T> implements Observer{
	private final ConcurrentMap<S, Future<T>> cache = new ConcurrentHashMap<S, Future<T>>();
	private final Computable<S, T> c;
	private final ExpirationHandler<S> timeoutHandler;
	/**
	 * 
	 * @param c : computable action
	 * @param timeout : timeout of cache, first it will be erased, 0 for infinite
	 */
	public Cache(Computable<S, T> c) {
		this.c = c;
		this.timeoutHandler = new ExpirationHandler<>();
		this.timeoutHandler.addObserver(this);
	}
	
	/**
	 * Make elaboration or retrieve element from cache 
	 */
	public T compute(final S arg, long timeout, TimeUnit unit) throws InterruptedException {
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
				T result = f.get();
				timeoutHandler.register(arg, timeout, unit);
				return result;
			} catch (CancellationException e) {
				System.out.println("Computation not ready");
				cache.remove(arg, f);
			} catch (ExecutionException e) {
				System.out.println("Computation not ready");
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
		timeoutHandler.unregister(arg);
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
	 * Rerurn size of cache
	 * @return
	 */
	public int size() {
		return cache.size();
	}
	
	/**
	 * Terminate cache 
	 */
	public void terminate() {
		timeoutHandler.stop();
		cache.clear();
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

	/**
	 * Handle timeout of single element in cache, removing it when deadline is reached
	 */
	@Override
	public void update(Observable timeoutHandler, Object obj) {
		cache.remove(obj);
	}
	
}