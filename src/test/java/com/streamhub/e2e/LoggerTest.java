package com.streamhub.e2e;

import org.junit.After;
import org.junit.Test;

public class LoggerTest extends EndToEndTestCase {
	@After
	public void tearDown() throws Exception {
		browser.click("turnLoggingOn");
		super.tearDown();
	}

	@Test
	public void testTurningOffLoggingAgain() throws Exception {
		open("index.html");
		browser.click("turnLoggingOff");
		connect();
		assertEquals("", browser.getText("logMessages"));
	}

	@Test
	public void testSettingCustomLogger() throws Exception {
		open("index.html");
		browser.click("switchToCountLogger");
		connect();
		waitForText("messages logged");
	}
}
