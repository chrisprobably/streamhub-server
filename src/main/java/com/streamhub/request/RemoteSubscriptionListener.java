package com.streamhub.request;

import java.io.IOException;

import org.apache.log4j.Logger;

import com.streamhub.Connection;
import com.streamhub.DirectHandler;
import com.streamhub.api.Client;
import com.streamhub.api.SubscriptionListener;

class RemoteSubscriptionListener implements SubscriptionListener {
	private static final Logger log = Logger.getLogger(RemoteSubscriptionListener.class);
	private static final String ON_UN_SUBSCRIBE = "onUnSubscribe";
	private static final String ON_SUBSCRIBE = "onSubscribe";
	private static final String SEP = DirectHandler.DIRECT_MESSAGE_SEPARATOR;
	private final Connection connection;
	private final ConnectionListener connectionListener;

	public RemoteSubscriptionListener(Connection connection, ConnectionListener connectionListener) {
		this.connection = connection;
		this.connectionListener = connectionListener;
	}

	public void onSubscribe(String topic, Client client) {
		sendEvent(topic, client, ON_SUBSCRIBE);
	}

	public void onUnSubscribe(String topic, Client client) {
		sendEvent(topic, client, ON_UN_SUBSCRIBE);
	}

	private void sendEvent(String topic, Client client, String event) {
		try {
			connection.write(SEP + event + "(" + topic + "," + client.getUid() + ")" + SEP);
		} catch (IOException e) {
			log.error("Error writing to connection - closing", e);
			connection.close();
			connectionListener.connectionLost(this);
		}
	}
}
