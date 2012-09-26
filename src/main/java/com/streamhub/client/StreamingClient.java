package com.streamhub.client;

import java.io.IOException;
import java.nio.channels.ClosedChannelException;
import java.util.HashSet;
import java.util.Set;

import org.apache.log4j.Logger;

import com.streamhub.Connection;
import com.streamhub.util.SocketUtils;

abstract class StreamingClient implements IStreamingClient {
	private static final Logger log = Logger.getLogger(StreamingClient.class);
	protected ClientConnectionListener clientListener;
	protected String uid;
	protected Connection connection;
	protected boolean isConnected;
	private final Set<String> topics = new HashSet<String>();
	
	protected StreamingClient(String uid, ClientConnectionListener clientListener) {
		this.uid = uid;
		this.clientListener = clientListener;
	}
	
	public String getUid() {
		return uid;
	}

	public synchronized void addSubscription(String topic) {
		topics.add(topic);
	}

	public synchronized void removeSubscription(String topic) {
		topics.remove(topic);
	}

	public synchronized Set<String> getSubscriptions() {
		return topics;
	}

	public synchronized void onConnect() {
		clientListener.clientConnected(this);
	}
	
	public synchronized void disconnect() {
		closeConnection();
		log.info("Client-" + uid + " disconnected");
		clientListener.clientDisconnected(this);
	}
	
	public synchronized void lostConnection() {
		closeConnection();
		log.info("Client-" + uid + " lost connection");
		clientListener.clientLostConnection(this);
	}

	public boolean isConnected() {
		return isConnected;
	}
	
	public synchronized void setConnection(Connection connection) {
		log.debug("Client-" + uid + " setConnection to : " + SocketUtils.toString(connection));		
		this.connection = connection;
	}
	
	void write(String data) {
		log.debug("Sending to Client-" + uid + " data '" + data + "'");
		
		try {
			if (connection != null) {
				connection.write(data);
			}
		} catch (IOException e) {
			if (isConnected()) {
				if (! (e instanceof ClosedChannelException)) {
					log.error("Error sending to Client-" + uid, e);
				}
				lostConnection();
			}
		}
	}
	
	void shutdown() {
		closeConnection();
	}
	
	private void closeConnection() {
		isConnected = false;
		if (connection != null) {
			connection.close();
		}
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((uid == null) ? 0 : uid.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		final StreamingClient other = (StreamingClient) obj;
		if (uid == null) {
			if (other.uid != null)
				return false;
		} else if (!uid.equals(other.uid))
			return false;
		return true;
	}
}
