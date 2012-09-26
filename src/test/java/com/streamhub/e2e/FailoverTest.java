package com.streamhub.e2e;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;

import org.junit.Test;

import com.streamhub.util.Sleep;

public class FailoverTest extends EndToEndTestCase {
	private static final String TRYING = "Trying ";
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
	public void testFailingOverToSecondServerOnConnectionLoss() throws Exception {
		connectWithFailover();
		fakeConnectionLoss();
		waitForText(CONNECTING_TO + failoverUrl);
		waitForReconnection();
	}

	@Test
	public void testRandomFailoverAlgorithm() throws Exception {
		connectWithFailover(new String[] {primaryUrl, failoverUrl, thirdFailoverUrl}, "random", Collections.<String,String>emptyMap());
		fakeConnectionLoss();
		waitForText("Trying http");
		assertFalse(browser.getBodyText().contains("Trying undefined"));
	}

	@Test
	public void testSpecifyingInitialReconnectDelay() throws Exception {
		HashMap<String, String> options = new HashMap<String, String>();
		options.put("initialReconnectDelayMillis", "3000");
		connectWithFailover(new String[] {primaryUrl, failoverUrl}, "ordered", options);
		fakeConnectionLoss();
		waitForText("Attempting reconnect in 3000ms");
		Sleep.millis(1500);
		assertFalse(browser.getBodyText().contains("Reconnecting"));
		Sleep.millis(2000);
		assertTrue(browser.getBodyText().contains("Reconnecting"));
	}
	
	@Test
	public void testSpecifyingMaxReconnectAttempts() throws Exception {
		HashMap<String, String> options = new HashMap<String, String>();
		options.put("maxReconnectAttempts", "0");
		connectWithFailover(new String[] {primaryUrl, failoverUrl}, "ordered", options);
		fakeConnectionLoss();
		Sleep.millis(250);
		assertFalse(browser.getBodyText().contains("Reconnecting"));
		assertFalse(browser.getBodyText().contains("Attempting reconnect"));
	}
	
	@Test
	public void testMaxReconnectAttemptsIsResetOnSuccessfulReconnect() throws Exception {
		HashMap<String, String> options = new HashMap<String, String>();
		options.put("maxReconnectAttempts", "1");
		connectWithFailover(new String[] {primaryUrl, failoverUrl}, "ordered", options);
		fakeConnectionLoss();
		waitForText(TRYING + failoverUrl);
		waitForReconnection();
		fakeFailoverUrlConnectionLoss();
		waitForText(TRYING + primaryUrl);
	}
	
	@Test
	public void testSpecifyingExponentialBackOff() throws Exception {
		HashMap<String, String> options = new HashMap<String, String>();
		options.put("initialReconnectDelayMillis", "2000");
		options.put("useExponentialBackOff", "true");
		options.put("backOffMultiplier", "2");
		connectWithFailover(new String[] {primaryUrl}, "ordered", options);
		fakeConnectionLoss();
		Sleep.seconds(3);
		//assertEquals(1, reconnectionAttempts());
		Sleep.millis(1500);
		//assertEquals(1, reconnectionAttempts());
		Sleep.millis(2500);
		//assertEquals(2, reconnectionAttempts());
		Sleep.seconds(5);
		//assertEquals(2, reconnectionAttempts());
		Sleep.seconds(3);
		//assertEquals(3, reconnectionAttempts());
		waitForText("Attempting reconnect in 2000ms");
		waitForText("Attempting reconnect in 4000ms");
		waitForText("Attempting reconnect in 8000ms");
		waitForText("Attempting reconnect in 16000ms");
	}
	
	@Test
	public void testExponentialBackOffResetsAfterSuccessfulReconnection() throws Exception {
		HashMap<String, String> options = new HashMap<String, String>();
		options.put("initialReconnectDelayMillis", "2000");
		options.put("useExponentialBackOff", "true");
		options.put("backOffMultiplier", "2");
		connectWithFailover(new String[] {primaryUrl}, "ordered", options);
		fakeConnectionLoss();
		waitForText("Attempting reconnect in 2000ms");
		waitForText("Attempting reconnect in 4000ms");
		primaryUrlUp();
		waitForReconnection();
		Sleep.millis(250);
		fakeConnectionLoss();
		waitForText("Attempting reconnect in 2000ms");
	}
	
	@Test
	public void testSpecifyingMaxReconnectDelay() throws Exception {
		HashMap<String, String> options = new HashMap<String, String>();
		options.put("initialReconnectDelayMillis", "1000");
		options.put("maxReconnectDelayMillis", "2000");
		options.put("useExponentialBackOff", "true");
		options.put("backOffMultiplier", "3");
		connectWithFailover(new String[] {primaryUrl}, "ordered", options);
		fakeConnectionLoss();
		waitForText("Attempting reconnect in 1000ms");
		waitForText("Attempting reconnect in 2000ms");
	}
	
	@Test
	public void testWrappingRoundServerListOnConnectionLosses() throws Exception {
		connectWithFailover();
		fakeConnectionLoss();
		waitForText(CONNECTING_TO + failoverUrl);
		waitForReconnection();
		primaryUrlUp();
		fakeFailoverUrlConnectionLoss();
		waitForText(CONNECTING_TO + primaryUrl);
		waitForReconnection();
	}
	
	@Test
	public void testPriorityFailoverAlgorithmStartsAtTopOfServerList() throws Exception {
		fakeFailoverUrlConnectionLoss();
		connectWithFailover(new String[] {primaryUrl, failoverUrl, thirdFailoverUrl}, "priority", Collections.<String,String>emptyMap());
		fakeConnectionLoss();
		waitForText(TRYING + primaryUrl);
		clearText();
		waitForText(TRYING + failoverUrl);
		clearText();
		waitForText(TRYING + thirdFailoverUrl);
		waitForText(CONNECTING_TO + thirdFailoverUrl);
		waitForReconnection();
	}
	
	@Test
	public void testWrappingRoundServerListWithPriorityFailoverAlgorithm() throws Exception {
		fakeDisconnectionViaProxyTwo();
		connectWithFailover(new String[] {primaryUrl, failoverUrl}, "priority", Collections.<String,String>emptyMap());
		fakeConnectionLoss();
		waitForText(TRYING + primaryUrl);
		clearText();
		waitForText(TRYING + failoverUrl);
		clearText();
		waitForText(TRYING + primaryUrl);
		primaryUrlUp();
		waitForText(CONNECTING_TO + primaryUrl);
		waitForReconnection();
		Sleep.seconds(4);
	}
	
	private void connectWithFailover() {
		connectWithFailover(new String[] {primaryUrl, failoverUrl}, "ordered", Collections.<String,String>emptyMap());
	}
	
	private void fakeConnectionLoss() {
		clearText();
		fakeDisconnection();
	}

	private void primaryUrlUp() throws IOException {
		fakeConnectionBackUp();
	}
	
	private void fakeFailoverUrlConnectionLoss() {
		clearText();
		fakeDisconnectionViaProxyTwo();
	}
	
	//private int reconnectionAttempts() {
	//	return StringUtils.countMatches(browser.getBodyText(), "Reconnecting");
	//}
}
