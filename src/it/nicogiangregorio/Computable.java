package it.nicogiangregorio;

public interface Computable<S, T> {
	public T compute(final S arg) throws InterruptedException;
}
