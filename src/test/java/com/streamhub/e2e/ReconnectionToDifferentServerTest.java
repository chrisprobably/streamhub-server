package com.streamhub.e2e;

import java.io.IOException;
import java.util.Collections;

import junit.framework.AssertionFailedError;

import org.apache.commons.lang.StringUtils;
import org.junit.Test;

import com.streamhub.util.Sleep;

public class ReconnectionToDifferentServerTest extends EndToEndTestCase {
	private static final int MAX_RECONNECT_TIMEOUT = 2000;
	private static final String ORDERED_FAILOVER = "ordered";
	private String[] serverList;
	private String primaryUrl;
	private String secondaryUrl;
	
	@Override
	protected void setUp() throws Exception {
		super.setUp();
		primaryUrl = framework.getStreamingServerProxyUrl();
		secondaryUrl = framework.getStreamingServerProxyTwoUrl();
		serverList = new String[] { primaryUrl, secondaryUrl };
	}

	@Test
	public void testGetUpdatesAfterReconnection() throws Exception {
		connectWithFailover(serverList, ORDERED_FAILOVER, Collections.<String,String>emptyMap());
		subscribe("AAPL");
		subscribe("KO");
		waitForSubscription();
		disconnectPrimary();
		sendPrice("AAPL", "ShouldCauseServerToDisconnectClient");
		sendPrice("AAPL", "ShouldCauseServerToDisconnectClient");
		waitForText(CONNECTING_TO + secondaryUrl);
		waitForReconnection();
		Sleep.seconds(1);
		clearText();
		sendPrice("AAPL", "3.333");
		sendPrice("KO", "4.444");		
		waitForText("3.333");
		waitForText("4.444");
	}	

	@Test
	public void testReconnection() throws Exception {
		connectWithFailover(serverList, ORDERED_FAILOVER, Collections.<String,String>emptyMap());
		disconnectPrimary();
		bringConnectionBackUpAndAssertClientReconnects();
	}
	
	@Test
	public void testReconnectionDoesNotLoop() throws Exception {
		connectWithFailover(serverList, ORDERED_FAILOVER, Collections.<String,String>emptyMap());
		disconnectPrimary();
		bringConnectionBackUpAndAssertClientReconnects();
		Sleep.millis(500);
		browser.click("clearText");
		Sleep.millis(MAX_RECONNECT_TIMEOUT + 200);
		try {
			assertFalse(browser.isTextPresent("Lost connection to server"));
		} catch (Throwable t) {
			throw new AssertionFailedError("Failed expected text NOT to be present: Lost connection to server. Entire text [" + browser.getBodyText() + "]");
		}
		assertFalse(browser.isTextPresent("Reconnecting"));
	}

	@Test
	public void testDeliberateDisconnectionDoesNotTriggerReconnection() throws Exception {
		connectWithFailover(serverList, ORDERED_FAILOVER, Collections.<String,String>emptyMap());
		disconnect();
		waitForDisconnection();
		Sleep.millis(MAX_RECONNECT_TIMEOUT + 100);
		assertFalse(browser.isTextPresent("Reconnecting"));
	}
	
	@Test
	public void testMultipleReconnections() throws Exception {
		connectWithFailover(serverList, ORDERED_FAILOVER, Collections.<String,String>emptyMap());
		clearText();
		disconnectPrimary();
		waitForText(CONNECTING_TO + secondaryUrl);
		waitForReconnection();
		clearText();
		bringPrimaryUp();
		disconnectSecondary();
		waitForText(CONNECTING_TO + primaryUrl);
		waitForReconnection();	
	}
	
	@Test
	public void testOnlyGetOneConnectionLostNotificationOnSecondConnectionLoss() throws Exception {
		connectWithFailover(serverList, ORDERED_FAILOVER, Collections.<String,String>emptyMap());
		clearText();
		disconnectPrimary();
		waitForText(CONNECTING_TO + secondaryUrl);
		waitForReconnection();
		clearText();
		bringPrimaryUp();
		disconnectSecondary();
		waitForText(CONNECTING_TO + primaryUrl);
		waitForReconnection();
		clearText();
		disconnectPrimary();
		waitForText("Lost connection to server");
		assertEquals(1, StringUtils.countMatches(browser.getBodyText(), "Lost connection to server"));
	}
	
	private void bringPrimaryUp() throws IOException {
		fakeConnectionBackUp();
	}

	private void disconnectSecondary() {
		fakeDisconnectionViaProxyTwo();
	}

	private void disconnectPrimary() {
		fakeDisconnection();
	}
}
