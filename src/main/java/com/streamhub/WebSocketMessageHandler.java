package com.streamhub;

import org.apache.log4j.Logger;

import com.streamhub.api.SubscriptionManager;
import com.streamhub.client.CannotCreateClientException;
import com.streamhub.client.IStreamingClient;
import com.streamhub.request.Request;
import com.streamhub.request.WebSocketRequest;

public class WebSocketMessageHandler {
	private final StreamingSubscriptionManager subscriptionManager;
	private Connection connection;
	private static Logger log = Logger.getLogger(WebSocketMessageHandler.class);
	
	public WebSocketMessageHandler(SubscriptionManager subscriptionManager) {
		this.subscriptionManager = (StreamingSubscriptionManager) subscriptionManager;
	}
	
	public synchronized void handleMessage(String message, String uid, Connection connection) {
		this.connection = connection;
		WebSocketRequest request = WebSocketRequest.createFrom(message, uid);
		request.connection = connection;
		
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
				String uid = request.getUid();
				connection.setAttachment(uid);
			}
			client.onConnect();
		} catch (Exception e) {
			log.error("Error during connecting client from request: " + request, e);
		}
	}
}
