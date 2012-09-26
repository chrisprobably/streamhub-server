package com.streamhub.e2e;

import org.junit.After;
import org.junit.Test;

import com.streamhub.api.JsonPayload;

public class UniqueIdStockDemoTest extends EndToEndTestCase {
	@After @Override
	public void tearDown() throws Exception {
		disconnect();
		waitForText("disconnection OK");
		super.tearDown();
	}
	
	@Test
	public void testSubscribingToTopic() throws Exception {
		open("stockDemo.html");
		connectUniqueId();
		waitForText("Connecting request iFrame to ");
		waitForText("Connecting response iFrame to ");
		waitForText("Connection response is : request OK");
		waitForText("onResponseData via response iFrame : [response OK");
		waitForText("Subscription response is : subscription OK");
		waitForText("priceChangeListener response for topic 'AAPL' is Name: 'Apple'");
		waitForText("priceChangeListener response for topic 'KO' is Name: 'Coca-Cola'");
		
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

	private void connectUniqueId() {
		waitForText("Server URL:");
		browser.type("url", EndToEndFramework.getInstance().getStreamingServerUrl());
		browser.click("connectUniqueId");
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

