package com.streamhub;

import java.net.URL;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.streamhub.tools.browser.MockBrowser;
import com.streamhub.tools.proxy.Proxy;
import com.streamhub.util.Sleep;

public class BrowserAndProxyIntegrationTest extends MockBrowserTestCase {
	private Proxy proxy;
	private MockBrowser browser;
	
	@Before @Override
	public void setUp() throws Exception {
		super.setUp();
		proxy = new Proxy(PROXY_PORT, new URL("http://localhost:" + STREAMING_SERVER_PORT));
		browser = new MockBrowser(new URL("http://" + proxy.getHost() + ":" + proxy.getPort() + "/streamhub/"), 1);
		proxy.start();
	}
	
	@After @Override
	public void tearDown() throws Exception {
		proxy.stop();
		super.tearDown();
	}
	
	@Test
	public void testConnectingBrowserThroughProxy() throws Exception {
		browser.connectToStreamingServer();
		browser.waitForConnected();
	}
	
	@Test
	public void testConnectingBrowserThroughProxyTwo() throws Exception {
		browser.connectToStreamingServer();
		browser.waitForConnected();
	}
	
	@Test
	public void testConnectingBrowserThroughProxyThree() throws Exception {
		browser.connectToStreamingServer();
		browser.waitForConnected();
	}
	
	@Test
	public void testSubscribingBrowserThroughProxy() throws Exception {
		browser.connectToStreamingServer();
		browser.waitForConnected();
		setUpSubscriptionResponse();
		browser.subscribe("STOCK");
		browser.waitForSubscribed();
	}
		
	@Test
	public void testDisconnectingBrowserUsingProxy() throws Exception {
		browser.connectToStreamingServer();
		setUpSubscriptionResponse();
		browser.subscribe("STOCK");
		browser.waitForSubscribed();
		proxy.stop();
		sendPrice("78.886");
		Sleep.seconds(1);
		assertFalse(browser.hasReceived("78.886"));
	}
	
	@Test
	public void testBringingProxyUpAgain() throws Exception {
		browser.connectToStreamingServer();
		browser.waitForConnected();
		assertTrue(browser.hasConnectedOk());
		proxy.stop();
		proxy.start();
		browser = new MockBrowser(2);
		browser.connectToStreamingServer();
		browser.waitForConnected();
		assertTrue(browser.hasConnectedOk());
	}
}
