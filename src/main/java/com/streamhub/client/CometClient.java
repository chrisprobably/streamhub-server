package com.streamhub.client;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedChannelException;
import java.util.LinkedList;
import java.util.Queue;

import org.apache.log4j.Logger;

import com.streamhub.api.Payload;
import com.streamhub.request.Request;
import com.streamhub.util.ArrayUtils;

class CometClient extends StreamingClient {
	public static final int QUEUE_SIZE = 1000;
	private static final byte[] COMMA_BYTES = ",".getBytes();
	private static final Logger log = Logger.getLogger(CometClient.class);
	private static final byte[] START_ARRAY_BYTES = "[".getBytes();
	private static final byte[] END_ARRAY_BYTES = "]".getBytes();
	private final Queue<Message> queue = new LinkedList<Message>();

	private CometClient(String uid, ClientConnectionListener clientListener) {
		super(uid, clientListener);
	}

	public static IStreamingClient createFrom(Request request, ClientConnectionListener clientListener) {
		return new CometClient(request.getUid(), clientListener);
	}

	public synchronized void onConnect() {
		log.info("Client-" + getUid() + " connected");
		isConnected = true;
		sendQueuedMessages();
		super.onConnect();
	}

	public synchronized void send(String topic, Payload payload) {
		if (connection == null) {
			queue(topic, payload);
			return;
		}
		
		if (isConnected()) {
			try {
				payload.timestamp();
				connection.write(ByteBuffer.wrap(payload.toCometBytes()));
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

	public String getQueuedMessages() {
		log.debug("Client-" + uid + " sending queued messages: " + queue);
		byte[] allBytes = START_ARRAY_BYTES;
		boolean isFirstMessage = true;
		for (Message message : queue) {
			byte[] cometBytes = message.payload.toString().getBytes();
			if (isFirstMessage) {
				allBytes = ArrayUtils.concat(allBytes, cometBytes);
				isFirstMessage = false;
			} else {
				allBytes = ArrayUtils.concatAll(allBytes, COMMA_BYTES, cometBytes);
			}
		}
		allBytes = ArrayUtils.concat(allBytes, END_ARRAY_BYTES);
		queue.clear();		
		return new String(allBytes);
	}
	
	public void destroy() {
		queue.clear();
		log.info("Client-" + uid + " deleted");
	}
	
	private synchronized void queue(String topic, Payload payload) {
		if (queue.size() == QUEUE_SIZE) {
			log.info("Client-" + uid + " queue is full - disconnecting");
			queue.add(new Message(topic, payload));
			lostConnection();
		} else if (queue.size() < QUEUE_SIZE) {
			if (log.isDebugEnabled()) {
				log.debug("Client-" + uid + " response pipe not connected: no outputStream has been set yet: queueing message");
			}
			queue.add(new Message(topic, payload));
		}
	}

	private synchronized void sendQueuedMessages() {
		log.debug("Client-" + uid + " sending queued messages: " + queue);
		for (Message message : queue) {
			send(message.topic, message.payload);
		}
		queue.clear();
	}
}
