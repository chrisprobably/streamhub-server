package com.streamhub;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.streamhub.api.JsonPayload;
import com.streamhub.api.Payload;
import com.streamhub.api.PushServer;
import com.streamhub.demo.TestSubscriptionListener;
import com.streamhub.e2e.EchoMode;
import com.streamhub.e2e.EchoPublishListener;
import com.streamhub.nio.NIOServer;
import com.streamhub.tools.MockDirectClient;
import com.streamhub.util.Sleep;

public class DirectConnectionTest extends StreamingServerTestCase {
	private static final String PUB_CHANNEL = "publish.echo";
	private static final String PRICE_SENT_AFTER_UNSUBSCRIBE = "444.44";
	private PushServer streamingServer;
	private TestSubscriptionListener subscriptionListener; 
	private EchoPublishListener echoPublishListener;
	
	@Before
	public void setUp() throws Exception {
		super.setUp();
		streamingServer = new NIOServer(8888);
		echoPublishListener = new EchoPublishListener(EchoMode.PUBLISH, streamingServer);
		streamingServer.getSubscriptionManager().addPublishListener(echoPublishListener);
		subscriptionListener = new TestSubscriptionListener(streamingServer);
		setUpSubscriptionResponse();
		streamingServer.start();
	}
	
	@After
	public void tearDown() throws IOException {
		streamingServer.stop();
	}
	
	@Test
	public void testConnectingAndDisconnecting() throws Exception {
		MockDirectClient client = new MockDirectClient("1");
		client.connect("localhost", 8888);
		client.waitForConnected();
		client.disconnect();
		client.waitForDisconnected();
	}
	
	@Test
	public void testReceivingStreamingMessagesOnSingleTopic() throws Exception {
		MockDirectClient client = new MockDirectClient("1");
		client.connect("localhost", 8888);
		client.subscribe("STOCK");
		client.waitForMessageContaining("Initial");
		sendPrice("24323.23");
		client.waitForMessageContaining("24323.23");
		sendPrice("3.11");
		client.waitForMessageContaining("3.11");
	}


	@Test
	public void testReceivingStreamingMessagesOnMultipleTopics() throws Exception {
		MockDirectClient client = new MockDirectClient("1");
		client.connect("localhost", 8888);
		client.subscribe("STOCK");
		client.subscribe("TOPIC2");
		client.waitForMessageContaining("Initial");
		client.waitForMessageContaining("Topic2 Initial");
		sendOnTopic2("Some thing interesting");
		sendPrice("24323.23");
		client.waitForMessageContaining("Some thing interesting");
		client.waitForMessageContaining("24323.23");
		sendOnTopic2("hullabulloo  3");
		sendOnTopic2("hullabulloo  4");
		client.waitForMessageContaining("hullabulloo  3");
		client.waitForMessageContaining("hullabulloo  4");
	}
	
	@Test
	public void testUnsubscribingFromSingleTopic() throws Exception {
		MockDirectClient client = new MockDirectClient("1");
		client.connect("localhost", 8888);
		client.subscribe("STOCK");
		client.waitForMessageContaining("Initial");
		client.unsubscribe("STOCK");
		Sleep.millis(300);
		sendPrice(PRICE_SENT_AFTER_UNSUBSCRIBE);
		Sleep.millis(300);
		List<String> messages = client.getMessages();
		for (String message : messages) {
			assertFalse(message.contains(PRICE_SENT_AFTER_UNSUBSCRIBE));
		}
	}
	
	@Test
	public void testUnsubscribingFromOneOfMultipleTopics() throws Exception {
		MockDirectClient client = new MockDirectClient("1");
		client.connect("localhost", 8888);
		client.subscribe("STOCK");
		client.subscribe("TOPIC2");
		client.waitForMessageContaining("Initial");
		client.waitForMessageContaining("Topic2 Initial");
		client.unsubscribe("STOCK");
		Sleep.millis(300);
		sendPrice(PRICE_SENT_AFTER_UNSUBSCRIBE);
		sendOnTopic2("222.221");
		client.waitForMessageContaining("222.221");
		Sleep.millis(300);
		List<String> messages = client.getMessages();
		for (String message : messages) {
			assertFalse(message.contains(PRICE_SENT_AFTER_UNSUBSCRIBE));
		}
	}
	
	@Test
	public void testPublishing() throws Exception {
		MockDirectClient one = new MockDirectClient("1");
		MockDirectClient two = new MockDirectClient("2");
		one.connect("localhost", 8888);
		two.connect("localhost", 8888);
		two.subscribe(PUB_CHANNEL);
		Payload payload = new JsonPayload(PUB_CHANNEL);
		payload.addField("PublishField", "PublishedValue");
		one.publish(PUB_CHANNEL, payload);
		two.waitForMessageContaining("PublishedValue");
	}
	
	@Test
	public void testPublishingWithTimeInBetween() throws Exception {
		MockDirectClient one = new MockDirectClient("1");
		MockDirectClient two = new MockDirectClient("2");
		one.connect("localhost", 8888);
		two.connect("localhost", 8888);
		two.subscribe(PUB_CHANNEL);
		Sleep.seconds(1);
		Payload payload = new JsonPayload(PUB_CHANNEL);
		payload.addField("PublishField", "PublishedValue");
		one.publish(PUB_CHANNEL, payload);
		two.waitForMessageContaining("PublishedValue");
	}
	
	private void sendPrice(final String price) {
		subscriptionListener.publish("STOCK", new JsonPayload("STOCK") {{
			addField("LastPrice", price);
		}});
	}

	private void sendOnTopic2(final String string) {
		subscriptionListener.publish("TOPIC2", new JsonPayload("TOPIC2") {{
			addField("SomeKey", string);
		}});
	}

	@SuppressWarnings("serial")
	private void setUpSubscriptionResponse() {
		subscriptionListener.setSubscriptionResponses(new HashMap<String, Payload>() {{
			put("STOCK", new JsonPayload("STOCK") {{
				addField("LastPrice", "Initial");
			}});
			put("TOPIC2", new JsonPayload("TOPIC2") {{
				addField("SomeKey", "Topic2 Initial");
			}});
		}});
	}	
}
