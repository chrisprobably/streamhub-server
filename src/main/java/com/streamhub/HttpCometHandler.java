package com.streamhub;

import org.apache.log4j.Logger;

import com.streamhub.api.Payload;
import com.streamhub.api.SubscriptionManager;
import com.streamhub.client.IStreamingClient;
import com.streamhub.handler.Handler;
import com.streamhub.request.Request;

public class HttpCometHandler implements Handler {
	private static final Logger log = Logger.getLogger(HttpCometHandler.class);
	private static final String CONNECTING_CLIENT = "Connecting Client-";
	private final StreamingSubscriptionManager subscriptionManager;
	private final Handler httpHandler;
	
	public HttpCometHandler(SubscriptionManager subscriptionManager, Handler httpHandler) {
		this.httpHandler = httpHandler;
		this.subscriptionManager = (StreamingSubscriptionManager) subscriptionManager;
	}
	
	public void handle(Connection connection) throws Exception {
		Request request = connection.getRequest();
		handle(connection, request);
	}
	
	public void handle(Connection connection, Request request) throws Exception {
		if (request.isIframeHtmlRequest()) {
			String response = SpecialPages.iframeHttpResponse();
			connection.write(response);
		} else if (request.isDisconnection()) {
			IStreamingClient client = subscriptionManager.findOrCreateClient(request);
			client.disconnect();
			String response = ResponseFactory.disconnectionResponse(request.getDomain(), request.getBrowser());
			connection.write(response);
		} else if (request.isSubscription()) {
			subscriptionManager.addSubscription(request);
			String response = ResponseFactory.subscriptionResponse(request.getDomain(), request.getBrowser());
			connection.write(response);
		} else if (request.isPublish()) {
			String response = ResponseFactory.publishResponse(request.getDomain(), request.getBrowser());
			connection.write(response);
			IStreamingClient client = subscriptionManager.findOrCreateClient(request);
			Payload payload = request.getPayload();
			String topic = request.getPublishTopic();
			subscriptionManager.notifyPublishListeners(client, topic, payload);
		} else if (request.isResponseConnection()) {
			IStreamingClient client = subscriptionManager.findOrCreateClient(request);
			log.info(new StringBuilder(CONNECTING_CLIENT).append(client.getUid()).toString());
			String response = ResponseFactory.foreverFramePageHeader(request.getDomain(), request.getBrowser());
			connection.write(response);
			client.onConnect();
		} else if (request.isRequestIFrameConnection()) {
			connection.write(ResponseFactory.requestResponse(request.getDomain(), request.getBrowser()));
		} else if (request.isUnSubscribe()) {
			subscriptionManager.removeSubscription(request);
			String response = ResponseFactory.unSubscribeResponse(request.getDomain(), request.getBrowser());
			connection.write(response);
		} else if (request.isPoll()) {
			IStreamingClient client = subscriptionManager.findOrCreateClient(request);
			log.debug("Poll request from Client-" + client.getUid());
			connection.write(ResponseFactory.pollResponse(request.getDomain(), request.getBrowser(), client.getQueuedMessages()));
		} else if (request.isCloseResponse()) {
			IStreamingClient client = subscriptionManager.findOrCreateClient(request);
			log.debug("Close response channel from Client-" + client.getUid());
			client.disconnect();
			connection.write(ResponseFactory.closeResponse(request.getDomain(), request.getBrowser()));
		} else {
			httpHandler.handle(connection);
		}
	}

}
