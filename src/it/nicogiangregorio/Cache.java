package it.nicogiangregorio;

import java.util.concurrent.Callable;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;

public class Cache<S, T> implements Computable<S, T> {
	private final ConcurrentMap<S, Future<T>> cache = new ConcurrentHashMap<S, Future<T>>();
	private final Computable<S, T> c;

	public Cache(Computable<S, T> c) {
		this.c = c;
	}

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