package com.streamhub.e2e;

import org.junit.Test;

public class ConnectionTest extends EndToEndTestCase {
	@Test
	public void testConnectingToStreamingServer() throws Exception {
		open("index.html");
		connect();
		waitForConnection();
	}
}

