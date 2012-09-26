package com.streamhub;

@SuppressWarnings("serial")
public class UnrecoverableStartupException extends RuntimeException {
	public UnrecoverableStartupException(String message) {
		super(message);
		System.err.println("StreamHub not started: " + message);
	}

	public UnrecoverableStartupException(String message, Throwable cause) {
		super(message, cause);
		System.err.println("StreamHub not started: " + message);
	}
}
