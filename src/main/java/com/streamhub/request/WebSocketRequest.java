/**
 * 
 */
package com.streamhub.request;

import org.apache.log4j.Logger;

import com.streamhub.Connection;
import com.streamhub.api.Payload;
import com.streamhub.util.Browser;

public class WebSocketRequest implements Request {
	private static Logger log = Logger.getLogger(WebSocketRequest.class);
	
	public String uid;
	public boolean isResponseConnection;
	public boolean isSubscription;
	public boolean isUnSubscribe;
	public boolean isDisconnection;
	public boolean isPublish;
	public String publishTopic;
	public Payload publishPayload;
	public String[] subscriptionTopics;
	public Connection connection;
	
	public static WebSocketRequest createFrom(String message, String uid) {
		WebSocketRequest request = new WebSocketRequest();
		log.debug("Received message [" + message + "] from Client-" + uid);
		request.uid = uid;

		if (message.startsWith("uid=")) {
			request.uid = extractUid(message);
			request.isResponseConnection = true;
		} else if (message.startsWith("subscribe")) {
			request.isSubscription = true;
			extractTopics(message, request);
		} else if (message.startsWith("unsubscribe")) {
			request.isUnSubscribe = true;
			extractTopics(message, request);
		} else if (message.startsWith("disconnect")) {
			request.isDisconnection = true;
		} else if (message.startsWith("publish")) {
			int firstBracketIndex = message.indexOf('(');
			int firstCommaIndex = message.indexOf(',');
			int endIndex = message.length() - 1;
			String topic = message.substring(firstBracketIndex + 1, firstCommaIndex);
			String jsonString = message.substring(firstCommaIndex + 1, endIndex);
			Payload payload = UrlEncodedJsonPayload.createFrom(jsonString);
			request.isPublish = true;
			request.publishTopic = topic;
			request.publishPayload = payload;
		}

		return request;
	}
	
	public Browser getBrowser() {
		return null;
	}
	public Connection getConnection() {
		return connection;
	}
	public String getContext() {
		return null;
	}
	public String getDomain() {
		return null;
	}
	public Payload getPayload() {
		return publishPayload;
	}
	public String getProcessedUrl() {
		return null;
	}
	public String getPublishTopic() {
		return publishTopic;
	}
	public String[] getSubscriptionTopics() {
		return subscriptionTopics;
	}
	public String getUid() {
		return uid;
	}
	public String getUrl() {
		return null;
	}
	public boolean isCloseResponse() {
		return false;
	}
	public boolean isDisconnection() {
		return isDisconnection;
	}
	public boolean isIframeHtmlRequest() {
		return false;
	}
	public boolean isKeepAliveConnection() {
		return false;
	}
	public boolean isPoll() {
		return false;
	}
	public boolean isPublish() {
		return isPublish;
	}
	public boolean isRequestIFrameConnection() {
		return false;
	}
	public boolean isResponseConnection() {
		return isResponseConnection;
	}
	public boolean isSubscription() {
		return isSubscription;
	}
	public boolean isUnSubscribe() {
		return isUnSubscribe;
	}
	public boolean isWebSocket() {
		return true;
	}
	
	private static String extractUid(String message) {
		return message.split("=")[1];
	}

	private static void extractTopics(String message, WebSocketRequest request) {
		String[] splitByEqual = message.split("=");

		if (splitByEqual.length > 1) {
			String topics = message.split("=")[1];
			request.subscriptionTopics = topics.split(",");
		} else {
			request.isSubscription = false;
			request.isUnSubscribe = false;
		}
	}
}