package com.streamhub.request;

import com.streamhub.Connection;
import com.streamhub.api.Payload;
import com.streamhub.util.Browser;

public interface Request {
	String getUid();
	String getDomain();
	String[] getSubscriptionTopics();
	String getUrl();
	String getProcessedUrl();
	String getContext();
	Browser getBrowser();
	Connection getConnection();
	boolean isSubscription();
	boolean isUnSubscribe();
	boolean isRequestIFrameConnection();
	boolean isResponseConnection();
	boolean isKeepAliveConnection();
	boolean isDisconnection();
	boolean isIframeHtmlRequest();
	boolean isPublish();
	boolean isPoll();
	boolean isCloseResponse();
	boolean isWebSocket();
	Payload getPayload();
	String getPublishTopic();
}