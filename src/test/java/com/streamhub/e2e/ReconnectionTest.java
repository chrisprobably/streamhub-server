package com.streamhub.e2e;

import junit.framework.AssertionFailedError;

import org.apache.commons.lang.StringUtils;
import org.junit.Test;

import com.streamhub.util.Sleep;

public class ReconnectionTest extends EndToEndTestCase {
	private static final int MAX_RECONNECT_TIMEOUT = 2000;

	@Test
	public void testGetUpdatesAfterReconnection() throws Exception {
		open("index.html");
		connectViaProxy();
		waitForConnectionViaProxy();
		subscribe("AAPL");
		subscribe("KO");
		waitForSubscription();
		fakeDisconnection();
		sendPrice("AAPL", "ShouldCauseServerToDisconnectClient");
		sendPrice("AAPL", "ShouldCauseServerToDisconnectClient");
		bringConnectionBackUpAndAssertClientReconnects();
		Sleep.seconds(1);
		clearText();
		sendPrice("AAPL", "3.333");
		sendPrice("KO", "4.444");
		waitForText("3.333");
		waitForText("4.444");
	}	
	
	@Test
	public void testConnectingViaProxy() throws Exception {
		open("index.html");
		connectViaProxy();
		waitForConnectionViaProxy();
	}

	@Test
	public void testDetectingDisconnection() throws Exception {
		open("index.html");
		connectThenTriggerDisconnection();
		waitForText("Lost connection to server");
	}

	@Test
	public void testReconnection() throws Exception {
		open("index.html");
		connectThenTriggerDisconnection();
		bringConnectionBackUpAndAssertClientReconnects();
	}
	
	@Test
	public void testReconnectionDoesNotLoop() throws Exception {
		open("index.html");
		connectThenTriggerDisconnection();
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
		open("index.html");
		connectViaProxy();
		waitForConnectionViaProxy();
		disconnect();
		waitForDisconnection();
		Sleep.millis(MAX_RECONNECT_TIMEOUT + 100);
		assertFalse(browser.isTextPresent("Reconnecting"));
	}
	
	@Test
	public void testMultipleReconnections() throws Exception {
		open("index.html");
		connectThenTriggerDisconnection();
		bringConnectionBackUpAndAssertClientReconnects();
		browser.click("clearText");
		fakeDisconnection();
		bringConnectionBackUpAndAssertClientReconnects();		
	}
	
	@Test
	public void testOnlyGetOneConnectionLostNotificationOnSecondConnectionLoss() throws Exception {
		open("index.html");
		connectThenTriggerDisconnection();
		Sleep.millis(250);
		bringConnectionBackUpAndAssertClientReconnects();
		Sleep.seconds(5);
		fakeDisconnection();
		Sleep.seconds(5);
		bringConnectionBackUpAndAssertClientReconnects();
		Sleep.millis(250);
		browser.click("clearText");
		fakeDisconnection();
		waitForText("Lost connection to server");
		assertEquals(1, StringUtils.countMatches(browser.getBodyText(), "Lost connection to server"));
	}
	
	@Test
	public void testReconnectsAfterLongDisconnectedPeriod() throws Exception {
		open("index.html");
		connectThenTriggerDisconnection();
		Sleep.seconds(5);
		browser.click("clearText");
		fakeConnectionBackUp();
		waitForText("onResponseData via response iFrame : [response OK]");
	}
	
//	@Test
//	public void testReconnectsIfConnectionDownOnInitialConnect() throws Exception {
//		open("index.html");
//		fakeDisconnection();
//		connectViaProxy();
//		Sleep.seconds(5);
//		assertFalse(browser.isTextPresent("Connection response is : uid '1' request OK"));
//		fakeConnectionBackUp();
//		Sleep.seconds(5);
//		waitForConnectionViaProxy();
//	}
//	
//	@Test
//	public void testResubscribesIfSubscriptionFails() throws Exception {
//		open("index.html");
//		connectThenTriggerDisconnection();
//		subscribe("AAPL");
//		subscribe("KO");
//		bringConnectionBackUpAndAssertClientReconnects();
//		waitForSubscription();
//	}
//	
//	@Test
//	public void testReceivesMessagesOnReconnect() throws Exception {
//		open("index.html");
//		connectViaProxy();
//		waitForConnectionViaProxy();
//		subscribe("AAPL");
//		subscribe("KO");
//		waitForSubscription();
//		fakeDisconnection();
//		sendPrice("AAPL", "324.332");
//		bringConnectionBackUpAndAssertClientReconnects();
//		Sleep.seconds(5);
//		waitForText("324.332");
//	}
}
