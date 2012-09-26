package com.streamhub.tools.browser;


@SuppressWarnings("serial")
public class ConnectionFailedException extends Exception {
	private final String connectResponse;

	public ConnectionFailedException(String connectResponse) {
		super(connectResponse);
		this.connectResponse = connectResponse;
	}

	@Override
	public String getMessage() {
		return "Connect response: " + connectResponse;
	}
}
