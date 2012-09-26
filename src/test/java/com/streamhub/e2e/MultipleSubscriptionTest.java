package com.streamhub.e2e;

import org.junit.Test;

import com.streamhub.api.JsonPayload;

public class MultipleSubscriptionTest extends EndToEndTestCase {
	@Test
	public void testSubscribingToTopic() throws Exception {
		open("stockDemo.html");
		waitForText("Server URL:");
		browser.type("url", EndToEndFramework.getInstance().getStreamingServerUrl());
		browser.click("connectMultipleSubscription");
		waitForConnection();
		waitForSubscription();
		
		assertEquals("115.75", lastPriceFor("Apple"));
		assertEquals("44.97", lastPriceFor("CocaCola"));
		
		sendAndWaitForPriceUpdate("AAPL", "114.25");
		assertEquals("114.25", lastPriceFor("Apple"));
		
		sendAndWaitForPriceUpdate("KO", "50.00");
		assertEquals("50.00", lastPriceFor("CocaCola"));
		
		sendAndWaitForPriceUpdate("KO", "50.25");
		sendAndWaitForPriceUpdate("KO", "50.55");
		assertEquals("50.55", lastPriceFor("CocaCola"));
	}

	private void sendAndWaitForPriceUpdate(String topic, final String price) {
		subscriptionListener.publish(topic, new JsonPayload(topic) {{
			addField("Last", price);
		}});
		
		waitForText(price);
	}

	private String lastPriceFor(String stock) {
		return browser.getText(stock);
	}
}

