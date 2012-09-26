package com.streamhub.request;

import com.streamhub.Connection;
import com.streamhub.api.Payload;
import com.streamhub.util.Browser;

public class DirectRequest implements Request {
	private String uid;
	private boolean isResponseConnection;
	private boolean isSubscription;
	private boolean isUnSubscribe;
	private String[] subscriptionTopics;
	private String publishTopic;
	private Payload publishPayload;
	private boolean isPublish;
	private boolean isDisconnection;

	public static Request createFrom(String message) {
		DirectRequest directRequest = new DirectRequest();

		if (message.startsWith("uid=")) {
			directRequest.uid = extractUid(message);
			directRequest.isResponseConnection = true;
		} else if (message.startsWith("subscribe")) {
			directRequest.isSubscription = true;
			extractTopics(message, directRequest);
		} else if (message.startsWith("unsubscribe")) {
			directRequest.isUnSubscribe = true;
			extractTopics(message, directRequest);
		} else if (message.startsWith("disconnect")) {
			directRequest.isDisconnection = true;
		} else if (message.startsWith("publish")) {
			int firstBracketIndex = message.indexOf('(');
			int firstCommaIndex = message.indexOf(',');
			int endIndex = message.length() - 1;
			String topic = message.substring(firstBracketIndex + 1 , firstCommaIndex);
			String jsonString = message.substring(firstCommaIndex + 1, endIndex);
			Payload payload = UrlEncodedJsonPayload.createFrom(jsonString);
			directRequest.isPublish = true;
			directRequest.publishTopic = topic;
			directRequest.publishPayload = payload;
		}

		return directRequest;
	}

	public static Request createFrom(String message, String uid) {
		Request directRequest = createFrom(message);
		((DirectRequest) directRequest).uid = uid;
		return directRequest;
	}

	private DirectRequest() {
	}

	public String getUid() {
		return uid;
	}

	public Browser getBrowser() {
		return Browser.UNKNOWN;
	}

	public String getDomain() {
		return null;
	}

	public String getRawRequest() {
		return null;
	}

	public String[] getSubscriptionTopics() {
		return subscriptionTopics;
	}

	public RequestType getType() {
		return null;
	}

	public String getUrl() {
		return null;
	}

	public boolean isDisconnection() {
		return isDisconnection;
	}

	public boolean isKeepAliveConnection() {
		return false;
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

	private static String extractUid(String message) {
		return message.split("=")[1];
	}

	public Connection getConnection() {
		return null;
	}

	public boolean isIframeHtmlRequest() {
		return false;
	}

	public boolean isPublish() {
		return isPublish;
	}

	public Payload getPayload() {
		return publishPayload;
	}

	public String getPublishTopic() {
		return publishTopic;
	}

	public boolean isUnSubscribe() {
		return isUnSubscribe;
	}

	public boolean isPoll() {
		return false;
	}
	
	public boolean isCloseResponse() {
		return false;
	}

	public String getContext() {
		return null;
	}
	
	public String getProcessedUrl() {
		return null;
	}

	public boolean isWebSocket() {
		return false;
	}

	private static void extractTopics(String message, DirectRequest directRequest) {
		String[] splitByEqual = message.split("=");
		
		if (splitByEqual.length > 1) {
			String topics = message.split("=")[1];
			directRequest.subscriptionTopics = topics.split(",");
		} else {
			directRequest.isSubscription = false;
			directRequest.isUnSubscribe = false;
		}
	}
}
