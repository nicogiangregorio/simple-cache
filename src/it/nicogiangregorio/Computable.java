package it.nicogiangregorio;

/**
 * Represent a generic computation to be cached
 * 
 * @author Nico Giangregorio
 *
 * @param <S>
 * @param <T>
 */
public interface Computable<S, T> {
	public T compute(final S arg) throws InterruptedException;
	
}
