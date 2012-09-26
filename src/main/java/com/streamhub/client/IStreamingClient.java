package com.streamhub.client;

import com.streamhub.Connection;
import com.streamhub.api.Client;

public interface IStreamingClient extends Client {
	void addSubscription(String topic);
	void removeSubscription(String topic);
	void onConnect();
	void setConnection(Connection connection);
	String getQueuedMessages();
	void destroy();
}