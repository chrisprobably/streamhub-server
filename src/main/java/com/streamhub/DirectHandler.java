package com.streamhub;

import java.io.IOException;

import org.apache.log4j.Logger;

import com.streamhub.api.SubscriptionManager;
import com.streamhub.client.CannotCreateClientException;
import com.streamhub.client.IStreamingClient;
import com.streamhub.handler.Handler;
import com.streamhub.reader.DirectMessageReader;
import com.streamhub.reader.MessageListener;
import com.streamhub.request.DirectRequest;
import com.streamhub.request.Request;

public class DirectHandler implements Handler, MessageListener {
	public static final String DIRECT_MESSAGE_SEPARATOR = "@@";
	public static final String MAGIC_DIRECT_CONNECTION_STRING = "@@DIRECT@@";
	private static final Logger log = Logger.getLogger(DirectHandler.class);
	private final StreamingSubscriptionManager subscriptionManager;
	private Connection connection;
	private String uid;

	public DirectHandler(SubscriptionManager subscriptionManager) {
		this.subscriptionManager = (StreamingSubscriptionManager) subscriptionManager;
	}

	public void onMessage(String message) {
		Request request;
		
		if (uid != null) {
			request = DirectRequest.createFrom(message, uid);
		} else {
			request = DirectRequest.createFrom(message);
		}
		
		if (request.isResponseConnection()) {
			connect(request);
		} else if (request.isDisconnection()) {
			disconnect(request);
		} else if (request.isSubscription()) {
			subscribe(request);
		} else if (request.isUnSubscribe()) {
			unsubscribe(request);
		} else if (request.isPublish()) {
			publish(request);
		} else {
			log.warn("Un-handled direct request. Message: '" + message + "'. Request: " + request);
		}
	}

	public synchronized void handle(Connection connection) {
		this.connection = connection;
		this.uid = connection.getAttachment();
		MessageListener listener = this;
		try {
			String inputSoFar = new String(connection.readBytes());
			DirectMessageReader.readDirectMessages(listener, inputSoFar, 0);
		} catch (IOException e) {
			log.error("Error reading connection", e);
		}
	}

	private void disconnect(Request request) {
		IStreamingClient client;
		
		try {
			client = subscriptionManager.findOrCreateClient(request);
			client.disconnect();
		} catch (CannotCreateClientException e) {
			log.error("Error creating client from request: " + request, e);
		}
		
		if (connection != null) {
			connection.close();
		}
	}

	private void subscribe(Request request) {
		try {
			subscriptionManager.addSubscription(request);
		} catch (CannotCreateClientException e) {
			log.error("Error creating client from request: " + request, e);
		}
	}
	
	private void unsubscribe(Request request) {
		try {
			subscriptionManager.removeSubscription(request);
		} catch (CannotCreateClientException e) {
			log.error("Error creating client from request: " + request, e);
		}
	}
	
	private void publish(Request request) {
		try {
			IStreamingClient client = subscriptionManager.findOrCreateClient(request);
			subscriptionManager.notifyPublishListeners(client, request.getPublishTopic(), request.getPayload());
		} catch (CannotCreateClientException e) {
			log.error("Error creating client from request: " + request, e);
		}
	}

	private void connect(Request request) {
		try {
			IStreamingClient client = subscriptionManager.findOrCreateClient(request);
			if (connection != null) {
				client.setConnection(connection);
				uid = request.getUid();
				connection.setAttachment(uid);
			}
			client.onConnect();
		} catch (Exception e) {
			log.error("Error during connecting client from request: " + request, e);
		}
	}
}
