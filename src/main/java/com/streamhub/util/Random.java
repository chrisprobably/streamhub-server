package com.streamhub.util;

public class Random {
	public static void sleepBetween(int minMillis, int maxMillis) {
		Sleep.millis(numberBetween(minMillis, maxMillis));
	}
	
	public static long numberBetween(int min, int max) {
		java.util.Random random = new java.util.Random();
		return random.nextInt((int) (max-min)) + min;
	}
}
