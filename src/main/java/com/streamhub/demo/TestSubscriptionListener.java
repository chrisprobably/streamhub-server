package com.streamhub.demo;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;

import com.streamhub.api.Client;
import com.streamhub.api.Payload;
import com.streamhub.api.PushServer;
import com.streamhub.api.SubscriptionListener;
import com.streamhub.api.SubscriptionManager;

public class TestSubscriptionListener implements SubscriptionListener {
	private static final Logger log = Logger.getLogger(TestSubscriptionListener.class);
	private final PushServer streamingServer;
	private Map<String, Payload> topicToPayload;
	private List<String> unsubscribed = new ArrayList<String>();
	private List<String> subscribed = new ArrayList<String>();

	public TestSubscriptionListener(PushServer streamingServer) {
		this.streamingServer = streamingServer;
		SubscriptionManager subscriptionManager = streamingServer.getSubscriptionManager();
		subscriptionManager.addSubscriptionListener(this);
	}

	public void onSubscribe(final String topic, Client client) {
		subscribed.add(topic);
		Payload payload = topicToPayload.get(topic);
		
		if (payload != null) {
			client.send(topic, payload);
		} else {
			log.warn("No response for topic: '" + topic + "'");
		}
	}
	
	public void onUnSubscribe(String topic, Client client) {
		unsubscribed.add(topic);
	}

	public void setSubscriptionResponses(Map<String, Payload> topicToPayload) {
		this.topicToPayload = topicToPayload;
	}

	public void publish(String topic, Payload payload) {
		streamingServer.publish(topic, payload);
	}

	public boolean hasUnSubscribed(String topic) {
		return unsubscribed.contains(topic);
	}
	
	public boolean hasSubscribed(String topic) {
		return subscribed.contains(topic);
	}

	public void clear() {
		unsubscribed.clear();
		subscribed.clear();
	}
}
