package com.streamhub.e2e;

import org.junit.Test;

import com.streamhub.api.JsonPayload;
import com.streamhub.util.Sleep;
import com.thoughtworks.selenium.condition.Condition;
import com.thoughtworks.selenium.condition.ConditionRunner.Context;

public class UnSubscribeTest extends EndToEndTestCase {
	private static final String COCA_COLA_SECOND_PRICE = "50.00";
	private static final String APPLE_SECOND_PRICE = "114.25";
	private static final String COCA_COLA_INITIAL_PRICE = "44.97";
	private static final String APPLE_INITIAL_PRICE = "115.75";
	private static final String APPLE_THIRD_PRICE = "33.33";

	@Test
	public void testUnSubscribingFromTopic() throws Exception {
		open("stockDemo.html");
		waitForText("Server URL:");
		browser.type("url", EndToEndFramework.getInstance().getStreamingServerUrl());
		browser.click("connectMultipleSubscription");
		waitForConnection();
		waitForSubscription();
		assertEquals(APPLE_INITIAL_PRICE, lastPriceFor("Apple"));
		assertEquals(COCA_COLA_INITIAL_PRICE, lastPriceFor("CocaCola"));
		
		unsubscribe("AAPL");
		sendPriceUpdate("AAPL", APPLE_SECOND_PRICE);
		sendAndWaitForPriceUpdate("KO", COCA_COLA_SECOND_PRICE);
		
		assertEquals(APPLE_INITIAL_PRICE, lastPriceFor("Apple"));
	}

	@Test
	public void testReSubscribing() throws Exception {
		open("stockDemo.html");
		waitForText("Server URL:");
		browser.type("url", EndToEndFramework.getInstance().getStreamingServerUrl());
		browser.click("connectMultipleSubscription");
		waitForConnection();
		waitForSubscription();
		assertEquals(APPLE_INITIAL_PRICE, lastPriceFor("Apple"));
		assertEquals(COCA_COLA_INITIAL_PRICE, lastPriceFor("CocaCola"));
		
		unsubscribe("AAPL");
		sendPriceUpdate("AAPL", APPLE_SECOND_PRICE);
		sendAndWaitForPriceUpdate("KO", COCA_COLA_SECOND_PRICE);
		
		assertEquals(APPLE_INITIAL_PRICE, lastPriceFor("Apple"));
		
		subscribe("AAPL");
		sendAndWaitForPriceUpdate("AAPL", APPLE_THIRD_PRICE);
	}
	
	@Test
	public void testUnSubscribingFromMultipleTopics() throws Exception {
		open("stockDemo.html");
		waitForText("Server URL:");
		browser.type("url", EndToEndFramework.getInstance().getStreamingServerUrl());
		browser.click("connectMultipleSubscription");
		waitForConnection();
		waitForSubscription();
		assertEquals(APPLE_INITIAL_PRICE, lastPriceFor("Apple"));
		assertEquals(COCA_COLA_INITIAL_PRICE, lastPriceFor("CocaCola"));
		
		unsubscribeAll();
		sendPriceUpdate("AAPL", APPLE_SECOND_PRICE);
		sendPriceUpdate("KO", COCA_COLA_SECOND_PRICE);
		
		Sleep.millis(500);
		
		assertEquals(APPLE_INITIAL_PRICE, lastPriceFor("Apple"));
		assertEquals(COCA_COLA_INITIAL_PRICE, lastPriceFor("CocaCola"));
	}

	private void sendAndWaitForPriceUpdate(String topic, final String price) {
		sendPriceUpdate(topic, price);
		waitForText(price);
	}

	private void sendPriceUpdate(String topic, final String price) {
		subscriptionListener.publish(topic, new JsonPayload(topic) {{
			addField("Last", price);
		}});
	}

	private String lastPriceFor(String stock) {
		return browser.getText(stock);
	}

	private void unsubscribeAll() {
		browser.click("unsubscribeAll");
		conditionRunner.waitFor(new Condition() {
			@Override
			public boolean isTrue(Context arg0) {
				return subscriptionListener.hasUnSubscribed("AAPL") && subscriptionListener.hasUnSubscribed("KO");
			}
		});
	}
}

