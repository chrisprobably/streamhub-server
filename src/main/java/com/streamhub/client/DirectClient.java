package com.streamhub.client;

import org.apache.log4j.Logger;

import com.streamhub.api.Payload;
import com.streamhub.reader.StreamReader;
import com.streamhub.request.Request;

class DirectClient extends StreamingClient {
	private static final int MESSAGE_INSERTION_POINT = 2;
	private static final String DIRECT_MESSAGE_SEPARATOR_X2 = "@@@@";
	private static final Logger log = Logger.getLogger(DirectClient.class);
	private StreamReader streamReader;
	
	private DirectClient(Request request, ClientConnectionListener clientListener) {
		super(request.getUid(), clientListener);
	}

	public static IStreamingClient createFrom(Request request, ClientConnectionListener clientListener) {
		return new DirectClient(request, clientListener);
	}

	public void send(String topic, Payload payload) {
		payload.timestamp();
		write(new StringBuilder(DIRECT_MESSAGE_SEPARATOR_X2).insert(MESSAGE_INSERTION_POINT, payload.toString()).toString());
	}

	public void onConnect() {
		isConnected = true;
		log.info("Client-" + getUid() + " connected");
		super.onConnect();
	}
	
	@Override
	public synchronized void disconnect() {
		if (streamReader != null) {
			streamReader.close();
		}
		super.disconnect();
	}

	@Override
	public synchronized void lostConnection() {
		if (streamReader != null) {
			streamReader.close();
		}
		super.lostConnection();
	}
	
	public String getQueuedMessages() {
		log.warn("getQueuedMessages should not be called on Direct clients");
		return "";
	}

	public void destroy() {
		
	}
	
	@Override
	public boolean equals(Object o) {
		return o instanceof DirectClient && uid.equals(((DirectClient) o).uid);
	}

	@Override
	public int hashCode() {
		return 31 << ((uid == null) ? 0 : uid.hashCode());
	}
}
