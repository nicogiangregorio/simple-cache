package it.nicogiangregorio.main;

import it.nicogiangregorio.Computable;

public class TestComputable implements Computable<String, String> {

	@Override
	public String compute(String arg) throws InterruptedException {
		return System.nanoTime() + "";
	}
}
