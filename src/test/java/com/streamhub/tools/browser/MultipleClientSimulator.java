package com.streamhub.tools.browser;

import java.io.IOException;
import java.net.URL;
import java.util.HashMap;

import com.streamhub.api.Client;
import com.streamhub.api.JsonPayload;
import com.streamhub.api.Payload;
import com.streamhub.api.PushServer;
import com.streamhub.api.SubscriptionListener;
import com.streamhub.demo.TestSubscriptionListener;
import com.streamhub.nio.NIOServer;

public class MultipleClientSimulator extends MultipleClientSimulatorBase implements SubscriptionListener {
	private final PushServer streamingServer = new NIOServer(8888);
	private final TestSubscriptionListener subscriptionListener = new TestSubscriptionListener(streamingServer);
	
	public MultipleClientSimulator(URL streamingServerUrl, int numberOfClients, Strategy strategy) {
		this(streamingServerUrl, numberOfClients, strategy, 10, 200);
	}

	public MultipleClientSimulator(URL streamingServerUrl, int numberOfClients, Strategy strategy, int browserBatchSize, long browserBatchInterval) {
		super(streamingServerUrl, numberOfClients, strategy, browserBatchSize, browserBatchInterval);
		streamingServer.getSubscriptionManager().addSubscriptionListener(this);
	}
	
	public void start() {
		streamingServer.start();
		setupSubscriptionListener();
		super.start();
	}
	
	public void stop() throws IOException {
		super.stop();
		streamingServer.stop();
	}

	public void sendPrice(final String price) {
		subscriptionListener.publish("STOCK", new JsonPayload("STOCK") {{
			addField("LastPrice", price);
		}});
	}
	
	public void sendCurrency(final String currencyPrice) {
		subscriptionListener.publish("CURRENCY", new JsonPayload("CURRENCY") {{
			addField("Bid", currencyPrice);
		}});
	}
	
	public void onSubscribe(String topic, Client client) {
		if ("STOCK".equals(topic)) {
			subscribedToStockUids.add(client.getUid());
		} else if ("CURRENCY".equals(topic)) {
			subscribedToCurrencyUids.add(client.getUid());
		}
	}

	public void onUnSubscribe(String topic, Client client) {
		// TODO Auto-generated method stub
	}
	
	@SuppressWarnings("serial") void setupSubscriptionListener() {
		subscriptionListener.setSubscriptionResponses(new HashMap<String, Payload>() {{
			put("STOCK", new JsonPayload("STOCK") {{
				addField("LastPrice", "Initial");
			}});
			put("CURRENCY", new JsonPayload("CURRENCY") {{
				addField("Bid", "CcyBid");
			}});
		}});
	}
}


