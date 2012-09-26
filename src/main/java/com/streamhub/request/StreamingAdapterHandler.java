package com.streamhub.request;

import java.io.IOException;

import org.apache.log4j.Logger;

import com.streamhub.Connection;
import com.streamhub.StreamingSubscriptionManager;
import com.streamhub.api.Payload;
import com.streamhub.api.PublishListener;
import com.streamhub.api.SubscriptionListener;
import com.streamhub.api.SubscriptionManager;
import com.streamhub.client.IStreamingClient;
import com.streamhub.handler.Handler;
import com.streamhub.reader.DirectMessageReader;
import com.streamhub.reader.MessageListener;

public class StreamingAdapterHandler implements Handler, MessageListener {
	private static final String ADD_SUBSCRIPTION_LISTENER = "addSubscriptionListener";
	private static final String REMOVE_SUBSCRIPTION_LISTENER = "removeSubscriptionListener";
	private static final String ADD_PUBLISH_LISTENER = "addPublishListener";
	private static final String REMOVE_PUBLISH_LISTENER = "removePublishListener";
	private static final String PUBLISH = "publish";
	private static final String SEND = "send";
	private static final int PUBLISH_TOPIC_START = 8;
	private static final int SEND_UID_START = 5;
	private static final Logger log = Logger.getLogger(StreamingAdapterHandler.class);
	private final StreamingSubscriptionManager subscriptionManager;
	private final UIDRepository remoteAdapterRepo;
	private Connection connection;
	private String uid;
	
	public StreamingAdapterHandler(SubscriptionManager subscriptionManager) {
		this.subscriptionManager = (StreamingSubscriptionManager) subscriptionManager;
		this.remoteAdapterRepo = new RemoteAdapterRepository(subscriptionManager);
	}

	public synchronized void handle(Connection connection) {
		this.connection = connection;
		this.uid = connection.getAttachment();
		MessageListener listener = this;
		try {
			DirectMessageReader.readDirectMessages(listener, new String(connection.readBytes()));
		} catch (IOException e) {
			log.error("Error reading connection", e);
			connection.close();
		}
	}

	public void onMessage(String message) {
		if (message.startsWith(PUBLISH)) {
			publish(message);
		} else if (message.startsWith(SEND)) {
			send(message);
		} else if (message.startsWith("uid=")) {
			String[] split = message.split("=");
			uid = split[1];
			connection.setAttachment(uid);
			log.info("Connecting StreamingAdapter-" + uid);
		} else if (ADD_SUBSCRIPTION_LISTENER.equals(message)) {
			log.info("Adding StreamingAdapter-" + uid + " as RemoteSubscriptionListener");
			SubscriptionListener subscriptionListener = remoteAdapterRepo.findOrCreateSubscriptionListener(uid, connection);
			subscriptionManager.addSubscriptionListener(subscriptionListener);
		} else if (ADD_PUBLISH_LISTENER.equals(message)) {
			log.info("Adding StreamingAdapter-" + uid + " as RemotePublishListener");
			PublishListener publishListener = remoteAdapterRepo.findOrCreatePublishListener(uid, connection);
			subscriptionManager.addPublishListener(publishListener);
		} else if (REMOVE_SUBSCRIPTION_LISTENER.equals(message)) {
			log.info("Removing StreamingAdapter-" + uid + " as RemoteSubscriptionListener");
			SubscriptionListener subscriptionListener = remoteAdapterRepo.findOrCreateSubscriptionListener(uid, connection);
			subscriptionManager.removeSubscriptionListener(subscriptionListener);
		} else if (REMOVE_PUBLISH_LISTENER.equals(message)) {
			log.info("Removing StreamingAdapter-" + uid + " as RemotePublishListener");
			PublishListener publishListener = remoteAdapterRepo.findOrCreatePublishListener(uid, connection);
			subscriptionManager.removePublishListener(publishListener);
		} else {
			log.warn("Unknown message received '" + message + "'");
		}
	}

	private void send(String message) {
		int firstCommaIndex = message.indexOf(',');
		int secondCommaIndex = message.indexOf(',', firstCommaIndex+1);
		int endIndex = message.length() - 1;
		String clientUid = message.substring(SEND_UID_START, firstCommaIndex);
		String topic = message.substring(firstCommaIndex + 1 , secondCommaIndex);
		String jsonString = message.substring(secondCommaIndex + 1, endIndex);
		log.debug("StreamingAdapter-" + uid + " sending to Client-" + clientUid + " topic '" + topic + "', payload '" + jsonString + "'" );
		Payload payload = ImmutableJsonPayload.createFrom(jsonString);
		IStreamingClient client = subscriptionManager.getClientManager().find(clientUid);
		client.send(topic, payload);
	}

	private void publish(String message) {
		int firstCommaIndex = message.indexOf(',');
		int endIndex = message.length() - 1;
		String topic = message.substring(PUBLISH_TOPIC_START, firstCommaIndex);
		String jsonString = message.substring(firstCommaIndex + 1, endIndex);
		log.debug("StreamingAdapter-" + uid + " publishing topic '" + topic + "', payload '" + jsonString + "'" );
		Payload payload = ImmutableJsonPayload.createFrom(jsonString);
		subscriptionManager.send(topic, payload);
	}
}
