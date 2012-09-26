package com.streamhub;

import java.io.InputStream;
import java.net.URL;
import java.util.List;

import org.junit.Before;
import org.junit.Test;

import com.streamhub.tools.browser.ConnectionFailedException;
import com.streamhub.tools.browser.MeasuringMockBrowser;
import com.streamhub.tools.browser.MockBrowser;
import com.streamhub.util.StreamUtils;

public class MockBrowserTest extends MockBrowserTestCase {
	private MockBrowser browser;
	
	@Before
	public void setUp() throws Exception {
		super.setUp();
		browser = new MockBrowser();
		browser.unsafeSetRequestOk("request OK");
		browser.unsafeSetSubscriptionOk("subscription OK");
	}

	@Test
	public void testGettingStaticDocument() throws Exception {
		InputStream inputStream = browser.get(new URL("http://localhost:8754/test/index.html"));
		String page = StreamUtils.toString(inputStream);
		assertTrue(page.contains("<title>Test Page</title>"));
	}
	
	@Test
	public void testThrowsExceptionWhenConnectionFails() throws Exception {
		browser.unsafeSetRequestOk("nada");
		try {
			browser.connectToStreamingServer();
			fail("Expected ConnectionFailedException");
		} catch (ConnectionFailedException expected) {
		}
	}
	
	@Test
	public void testHasConnectedOkIsFalseWhenConnectionFails() {
		browser.unsafeSetRequestOk("nada");
		try {
			browser.connectToStreamingServer();
		} catch (Exception e) {
		}
		assertFalse(browser.hasConnectedOk());
	}
	
	@Test
	public void testHasConnectedOkIsTrueWhenConnectionSucceeds() throws Exception {
		browser.connectToStreamingServer();
		assertTrue(browser.hasConnectedOk());
	}
	
	@Test
	public void testHasSubscribedOkIsTrueWhenSubscriptionSucceeds() throws Exception {
		browser.connectToStreamingServer();
		setUpSubscriptionResponse();
		browser.subscribe("STOCK");
		assertTrue(browser.hasSubscribedOk());
	}
	
	@Test
	public void testHasSubscribedOkIsFalseWhenSubscriptionFails() throws Exception {
		browser.connectToStreamingServer();
		setUpSubscriptionResponse();
		browser.unsafeSetSubscriptionOk("nada");
		browser.subscribe("STOCK");
		assertFalse(browser.hasSubscribedOk());
	}
	
	@Test
	public void testGettingNumberOfMessagesReceived() throws Exception {
		browser.connectToStreamingServer();
		setUpSubscriptionResponse();
		browser.subscribe("STOCK");
		browser.waitForMessages(2);
		assertEquals(2, browser.numberOfMessagesReceived());
	}
	
	@Test
	public void testGettingMessagesReceived() throws Exception {
		browser.connectToStreamingServer();
		setUpSubscriptionResponse();
		browser.subscribe("STOCK");
		browser.waitForMessages(2);
		List<String> messagesReceived = browser.getMessagesReceived();
		assertTrue(messagesReceived.get(0).contains("response OK") || messagesReceived.get(0).contains("Initial"));
		assertTrue(messagesReceived.get(1).contains("Initial") || messagesReceived.get(1).contains("response OK"));
	}
	
	@Test
	public void testHasReceivedMessage() throws Exception {
		browser.connectToStreamingServer();
		setUpSubscriptionResponse();
		browser.subscribe("STOCK");
		browser.waitForMessages(2);
		assertTrue(browser.hasReceived("Initial"));
	}
	
	@Test
	public void testGettingBytesPerSecond() throws Exception {
		browser = new MeasuringMockBrowser();
		browser.connectToStreamingServer();
		setUpSubscriptionResponse();
		browser.subscribe("STOCK");
		browser.waitForMessages(2);
		double bytesPerSecond = ((MeasuringMockBrowser) browser).getBytesPerSecond();
		assertTrue(bytesPerSecond > 0);
	}
	
	@Test
	public void testGettingUpdatesPerSecond() throws Exception {
		browser = new MeasuringMockBrowser();
		browser.connectToStreamingServer();
		setUpSubscriptionResponse();
		browser.subscribe("STOCK");
		browser.waitForMessages(2);
		double updatesPerSecond = ((MeasuringMockBrowser) browser).getUpdatesPerSecond();
		assertTrue(updatesPerSecond > 0);
	}
}
