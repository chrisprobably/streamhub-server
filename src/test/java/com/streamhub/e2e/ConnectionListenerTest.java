package com.streamhub.e2e;

import org.apache.commons.lang.StringUtils;
import org.junit.Test;

public class ConnectionListenerTest extends EndToEndTestCase {
	@Test
	public void testConnectionEstablishedListenerOnConnect() throws Exception {
		open("index.html");
		addConnectionListener();
		connect();
		waitForConnection();
		waitForText("ConnectionListener: Connection Established");
	}
	
	@Test
	public void testConnectionLostListenerOnDisconnection() throws Exception {
		open("index.html");
		addConnectionListener();
		connectThenTriggerDisconnection();
		waitForText("ConnectionListener: Connection Lost");
	}
	
	@Test
	public void testConnectionEstablishedListenerOnReconnect() throws Exception {
		open("index.html");
		addConnectionListener();
		connectThenTriggerDisconnection();
		bringConnectionBackUpAndAssertClientReconnects();
		waitForText("ConnectionListener: Connection Established");
	}
	
	@Test
	public void testAddingMultipleConnectionListeners() throws Exception {
		open("index.html");
		addConnectionListener();
		addConnectionListener();
		connectViaProxy();
		waitForConnectionViaProxy();
		waitForText("ConnectionListener: Connection Established");
		assertEquals(2, StringUtils.countMatches(browser.getBodyText(), "ConnectionListener: Connection Established"));
		clearText();
		fakeDisconnection();
		waitForText("ConnectionListener: Connection Lost");
		assertEquals(2, StringUtils.countMatches(browser.getBodyText(), "ConnectionListener: Connection Lost"));
	}
}
