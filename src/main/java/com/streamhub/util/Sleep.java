package com.streamhub.util;

public class Sleep {
	public static void millis(long millis) {
		try {
			Thread.sleep(millis);
		} catch (InterruptedException ignored) {}
	}
	
	public static void seconds(int seconds) {
		try {
			Thread.sleep(seconds * 1000);
		} catch (InterruptedException ignored) {}
	}
}
