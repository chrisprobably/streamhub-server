package com.streamhub.request;

import java.io.IOException;

import org.apache.log4j.Logger;

import com.streamhub.Connection;
import com.streamhub.DirectHandler;
import com.streamhub.api.Client;
import com.streamhub.api.Payload;
import com.streamhub.api.PublishListener;

class RemotePublishListener implements PublishListener {
	private static final Logger log = Logger.getLogger(RemotePublishListener.class);
	private static final String SEP = DirectHandler.DIRECT_MESSAGE_SEPARATOR;
	private final Connection connection;
	private final ConnectionListener connectionListener;

	public RemotePublishListener(Connection connection, ConnectionListener connectionListener) {
		this.connection = connection;
		this.connectionListener = connectionListener;
	}

	public void onMessageReceived(Client client, String topic, Payload payload) {
		try {
			connection.write(SEP + "onMessageReceived(" + client.getUid() + "," + topic + "," + payload.toString() + ")" + SEP);
		} catch (IOException e) {
			log.error("Error writing to connection", e);
			connection.close();
			connectionListener.connectionLost(this);
		}
	}

}
