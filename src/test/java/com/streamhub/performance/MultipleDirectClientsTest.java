package com.streamhub.performance;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.streamhub.StreamingServerTestCase;
import com.streamhub.api.JsonPayload;
import com.streamhub.api.Payload;
import com.streamhub.api.PushServer;
import com.streamhub.demo.TestSubscriptionListener;
import com.streamhub.e2e.EchoMode;
import com.streamhub.e2e.EchoPublishListener;
import com.streamhub.nio.NIOServer;
import com.streamhub.tools.MockDirectClient;
import com.streamhub.util.Random;
import com.streamhub.util.Sleep;

@SuppressWarnings("serial")
public class MultipleDirectClientsTest extends StreamingServerTestCase {
	private static final String INITIAL_RESPONSE = "Initial";
	private static final String TOPIC_2_INITIAL_RESPONSE = "Topic 2 Initial";
	private static final int PORT = 8888;
	private static final String HOST = "localhost";
	private static final String TOPIC = "topic";
	protected static final String TOPIC_2 = "topic-2";
	private final ExecutorService threadPool = Executors.newFixedThreadPool(50);
	private List<MockDirectClient> clients;
	private PushServer streamingServer;
	private TestSubscriptionListener subscriptionListener;
	private EchoPublishListener echoPublishListener;

	@Before
	public void setUp() throws Exception {
		super.setUp();
		streamingServer = new NIOServer(PORT);
		echoPublishListener = new EchoPublishListener(EchoMode.PUBLISH, streamingServer);
		streamingServer.getSubscriptionManager().addPublishListener(echoPublishListener);
		subscriptionListener = new TestSubscriptionListener(streamingServer);
		subscriptionListener.setSubscriptionResponses(new HashMap<String, Payload>() {{
				put(TOPIC, new JsonPayload(TOPIC) {{
					addField("Key", INITIAL_RESPONSE);
				}});
				put(TOPIC_2, new JsonPayload(TOPIC_2) {{
					addField("Key", TOPIC_2_INITIAL_RESPONSE);
				}});
			}
		});
		clients = new ArrayList<MockDirectClient>();
		streamingServer.start();
	}

	@After
	public void tearDown() throws IOException {
		streamingServer.stop();
	}

	@Test
	public void testFiftyClientsGetInitialResponse() throws Exception {
		createAndSubscribeClients(50, TOPIC);
		
		for (final MockDirectClient client : clients) {
			client.waitForMessageContaining(INITIAL_RESPONSE);
			assertEquals(1, client.getMessages().size());
		}
	}
	
	@Test
	public void testFiftyClientsGetInitialResponseForMultipleTopics() throws Exception {
		createAndSubscribeClients(50, TOPIC, TOPIC_2);
		
		for (final MockDirectClient client : clients) {
			client.waitForMessageContaining(INITIAL_RESPONSE);
			client.waitForMessageContaining(TOPIC_2_INITIAL_RESPONSE);
			assertEquals(2, client.getMessages().size());
		}
	}

	@Test
	public void testFiftyClientsGetMultipleResponses() throws Exception {
		createAndSubscribeClients(50, TOPIC);
		
		for (final MockDirectClient client : clients) {
			client.waitForMessageContaining(INITIAL_RESPONSE);
		}
		
		int numMessages = 10;
		for (int i = 0; i < numMessages; i++) {
			Payload payload = new JsonPayload(TOPIC);
			payload.addField("MainField", "Response " + i);
			subscriptionListener.publish(TOPIC, payload);
		}
		
		for (final MockDirectClient client : clients) {
			for (int i = 0; i < numMessages; i++) {
				client.waitForMessageContaining("Response " + i);
			}
			assertEquals(numMessages + 1, client.getMessages().size());
		}
	}
	
	@Test
	public void testFiftyClientsGetMultipleResponsesOnMultipleTopics() throws Exception {
		createAndSubscribeClients(50, TOPIC, TOPIC_2);
		
		for (final MockDirectClient client : clients) {
			client.waitForMessageContaining(INITIAL_RESPONSE);
			client.waitForMessageContaining(TOPIC_2_INITIAL_RESPONSE);
		}
		
		int numMessages = 10;
		for (int i = 0; i < numMessages; i++) {
			Payload topicOnePayload = new JsonPayload(TOPIC);
			topicOnePayload.addField("MainField", "Response " + i);
			subscriptionListener.publish(TOPIC, topicOnePayload);
			Payload topicTwoPayload = new JsonPayload(TOPIC_2);
			topicTwoPayload.addField("MainField", "Topic2Response " + i);
			subscriptionListener.publish(TOPIC_2, topicTwoPayload);
		}
		
		for (final MockDirectClient client : clients) {
			for (int i = 0; i < numMessages; i++) {
				client.waitForMessageContaining("Response " + i);
				client.waitForMessageContaining("Topic2Response " + i);
			}
			assertEquals((numMessages + 1) * 2, client.getMessages().size());
		}
	}
	
	@Test
	public void testTwoHundredClientsGetMultipleResponsesOnMultipleTopics() throws Exception {
		createAndSubscribeClients(200, TOPIC, TOPIC_2);
		
		for (final MockDirectClient client : clients) {
			client.waitForMessageContaining(INITIAL_RESPONSE);
			client.waitForMessageContaining(TOPIC_2_INITIAL_RESPONSE);
		}
		
		int numMessages = 10;
		for (int i = 0; i < numMessages; i++) {
			Payload topicOnePayload = new JsonPayload(TOPIC);
			topicOnePayload.addField("MainField", "Response " + i);
			subscriptionListener.publish(TOPIC, topicOnePayload);
			Payload topicTwoPayload = new JsonPayload(TOPIC_2);
			topicTwoPayload.addField("MainField", "Topic2Response " + i);
			subscriptionListener.publish(TOPIC_2, topicTwoPayload);
		}
		
		for (final MockDirectClient client : clients) {
			for (int i = 0; i < numMessages; i++) {
				client.waitForMessageContaining("Response " + i);
				client.waitForMessageContaining("Topic2Response " + i);
			}
			assertEquals((numMessages + 1) * 2, client.getMessages().size());
		}
	}

	private void createAndSubscribeClients(int numClients, final String... topics) {
		for (int i = 0; i < numClients; i++) {
			clients.add(new MockDirectClient(i + ""));
		}
		
		for (final MockDirectClient client : clients) {
			threadPool.execute(new Runnable() {
				public void run() {
					try {
						randomSleepToIncreaseInterleaving();
						client.connect(HOST, PORT);
						for (String topic : topics) {
							randomSleepToIncreaseInterleaving();
							client.subscribe(topic);
						}
					} catch (Exception e) {
						e.printStackTrace();
					}
				}

				private void randomSleepToIncreaseInterleaving() {
					Sleep.millis(Random.numberBetween(10, 150));
				}
			});
		}
	}
}
