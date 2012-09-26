package com.streamhub.client;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedChannelException;

import org.apache.log4j.Logger;

import com.streamhub.api.Payload;
import com.streamhub.request.Request;
import com.streamhub.util.WebSocketUtils;

public class WebSocketClient extends StreamingClient {
	private static final Logger log = Logger.getLogger(WebSocketClient.class);
	
	private WebSocketClient(String uid, ClientConnectionListener clientListener) {
		super(uid, clientListener);
	}

	@Override
	public synchronized void onConnect() {
		isConnected = true;
		log.info("Client-" + uid + " connected");
		super.onConnect();
	}

	public void destroy() {
		log.info("Client-" + uid + " deleted");
	}

	public String getQueuedMessages() {
		log.warn("getQueuedMessages should not be called on WebSocket clients");
		return "";
	}

	public synchronized void send(String topic, Payload payload) {
		sendWebSocketMessage(payload.toString());
	}

	private void sendWebSocketMessage(String message) {
		if (isConnected()) {
			ByteBuffer buffer = ByteBuffer.wrap(WebSocketUtils.createMessage(message));
			
			try {
				connection.write(buffer);
			} catch (IOException e) {
				if (isConnected()) {
					if (! (e instanceof ClosedChannelException)) {
						log.error("Error sending to Client-" + uid, e);
					}
					lostConnection();
				}
			}
		}
	}

	public static IStreamingClient createFrom(Request request, ClientConnectionListener clientListener) {
		return new WebSocketClient(request.getUid(), clientListener);
	}
}
