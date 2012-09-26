package com.streamhub;

import java.util.Collections;
import java.util.Set;

import com.streamhub.api.Payload;
import com.streamhub.client.IStreamingClient;

class NullClient implements IStreamingClient {
	public void addSubscription(String topic) {}

	public void disconnect() {}

	public Set<String> getSubscriptions() {
		return Collections.emptySet();
	}

	public String getUid() {
		return "null";
	}

	public boolean isConnected() {
		return false;
	}

	public void onConnect() {}

	public void send(String topic, Payload payload) {}

	public void setConnection(Connection connection) {}

	public void startNoOps(long intervalMillis) {}

	public void removeSubscription(String topic) {}

	public String getQueuedMessages() {
		return "";
	}

	public void destroy() {}
}
