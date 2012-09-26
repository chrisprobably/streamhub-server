package com.streamhub.e2e;

import java.util.HashMap;

import org.junit.Test;

public class PollingConnectionTest extends EndToEndTestCase {
	String primaryUrl;
	String failoverUrl;
	private String thirdFailoverUrl;

	@Override
	protected void setUp() throws Exception {
		super.setUp();
		primaryUrl = framework.getStreamingServerProxyUrl();
		failoverUrl = framework.getStreamingServerProxyTwoUrl();
		thirdFailoverUrl = framework.getStreamingServerUrl();
	}
	
	@Test
	public void testPollingConnection() throws Exception {
		HashMap<String, String> options = new HashMap<String, String>();
		options.put("connectionType", "POLL");
		connectWithFailover(new String[] {primaryUrl, failoverUrl, thirdFailoverUrl}, "random", options);
		subscribe("AAPL");
		subscribe("KO");
		waitForSubscription();
		sendPrice("AAPL", "1.2345");
		waitForText("1.2345");
		sendPrice("KO", "5.4321");
		waitForText("5.4321");
	}
}
