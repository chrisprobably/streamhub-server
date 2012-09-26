package com.streamhub;

import java.util.HashMap;

import org.junit.After;
import org.junit.Before;

import com.streamhub.api.JsonPayload;
import com.streamhub.api.Payload;
import com.streamhub.api.PushServer;
import com.streamhub.demo.TestSubscriptionListener;
import com.streamhub.nio.NIOServer;
import com.streamhub.reader.StreamReader;
import com.streamhub.tools.ConditionRunner;
import com.streamhub.util.Sleep;
import com.streamhub.util.StaticHttpServer;
import com.thoughtworks.selenium.condition.Condition;
import com.thoughtworks.selenium.condition.ConditionRunner.Context;

public abstract class MockBrowserTestCase extends StreamingServerTestCase {
	protected static final int STREAMING_SERVER_PORT = 8888;
	protected static final int PROXY_PORT = 7654;
	private static final int WAIT_FOR_INTERVAL = 100;
	private static final int WAIT_FOR_TIMEOUT = 2500;
	protected final PushServer streamingServer = new NIOServer(STREAMING_SERVER_PORT);
	protected final TestSubscriptionListener subscriptionListener = new TestSubscriptionListener(streamingServer);
	private final StaticHttpServer httpServer = new StaticHttpServer(8754);
	private ConditionRunner conditionRunner = new ConditionRunner(WAIT_FOR_INTERVAL, WAIT_FOR_TIMEOUT);
	
	@Before
	public void setUp() throws Exception {
		super.setUp();
		
		try {
			streamingServer.start();
		} catch (UnrecoverableStartupException e) {
			// address already in use - wait a bit.
			Sleep.millis(500);
			streamingServer.start();
		}

		httpServer.start();
	}

	@After
	public void tearDown() throws Exception {
		httpServer.stop();
		streamingServer.stop();
		super.tearDown();
	}
	
	protected void assertContains(final String expected, final StreamReader reader) {
		conditionRunner.waitFor(new Condition() {
			public boolean isTrue(Context context) {
				return reader != null && reader.getInputSoFar().contains(expected);
			}
		});
		assertTrue(reader.getLastResponse().contains(expected));
	}

	protected void sendPrice(final String price) {
		subscriptionListener.publish("STOCK", new JsonPayload("STOCK") {{
			addField("LastPrice", price);
		}});
	}

	@SuppressWarnings("serial")
	protected void setUpSubscriptionResponse() {
		subscriptionListener.setSubscriptionResponses(new HashMap<String, Payload>() {{
			put("STOCK", new JsonPayload("STOCK") {{
				addField("LastPrice", "Initial");
			}});
		}});
	}	
}
