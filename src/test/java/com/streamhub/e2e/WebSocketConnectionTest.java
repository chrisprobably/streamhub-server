package com.streamhub.e2e;

import org.junit.Test;

import com.streamhub.util.Sleep;

public class WebSocketConnectionTest extends WebSocketEndToEndTestCase {
	@Test
	public void testConnectingToStreamingServer() throws Exception {
		open("index.html");
		connect();
		waitForWebSocketConnection();
	}

	private void waitForWebSocketConnection() {
		Sleep.seconds(30);
	}
}

