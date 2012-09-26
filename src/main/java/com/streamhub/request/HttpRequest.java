package com.streamhub.request;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import com.streamhub.Connection;
import com.streamhub.api.Payload;
import com.streamhub.util.Browser;
import com.streamhub.util.UrlUtils;

public class HttpRequest implements Request {
	private static final String UPGRADE = "Upgrade";
	private static final String WEB_SOCKET = "WebSocket";
	private static final String CONNECTION = "Connection";
	private static final String CLOSE = "close";
	private static final String IFRAME_HTML = "/iframe.html";
	private static final String USER_AGENT = "User-Agent";
	private static final String RESPONSE = "/response/";
	private static final String REQUEST = "/request/";
	private static final String PUBLISH = "/publish/";
	private static final String SUBSCRIBE_REQUEST = "/subscribe/";
	private static final String UN_SUBSCRIBE_REQUEST = "/unsubscribe/";
	private static final String DISCONNECT = "/disconnect/";
	private static final String POLL = "/poll/";
	private static final String CLOSE_RESPONSE = "/closeresponse/";
	private static final String DOMAIN = "domain";
	private static final String UID = "uid";
	private static final String TOPIC = "topic";
	private static final String PAYLOAD = "payload";
	private final String uid;
	private final String domain;
	private final String[] subscriptionTopics;
	private final RequestType type;
	private final Browser browser;
	private final boolean isSubscription;
	private final boolean isDisconnection;
	private final boolean isRequestIFrameConnection;
	private final boolean isResponseConnection;
	private final boolean isPublish;
	private final String publishTopic;
	private final Payload publishPayload;
	private final boolean isUnSubscribe;
	private final boolean isPoll;
	private final boolean isCloseResponse;
	private final String context;
	private final String url;
	private final String processedUrl;
	private Connection connection;
	private boolean isWebSocket;

	private HttpRequest(String uid, String domain, String[] subscriptionTopics, String publishTopic, Payload publishPayload, RequestType type, String url, Browser browser, boolean isSubscription,
			boolean isDisconnection, boolean isRequestIFrameConnection, boolean isResponseConnection, boolean isPublish, boolean isUnSubscribe, boolean isPoll, boolean isCloseResponse, String processedUrl, String context, boolean isWebSocket) {
		this.uid = uid;
		this.domain = domain;
		this.subscriptionTopics = subscriptionTopics;
		this.publishTopic = publishTopic;
		this.publishPayload = publishPayload;
		this.type = type;
		this.context = context;
		this.url = url;
		this.processedUrl = processedUrl;
		this.browser = browser;
		this.isSubscription = isSubscription;
		this.isDisconnection = isDisconnection;
		this.isRequestIFrameConnection = isRequestIFrameConnection;
		this.isResponseConnection = isResponseConnection;
		this.isPublish = isPublish;
		this.isUnSubscribe = isUnSubscribe;
		this.isPoll = isPoll;
		this.isCloseResponse = isCloseResponse;
		this.isWebSocket = isWebSocket;
	}

	public static Request createFrom(Connection connection) throws IOException {
		String httpRequest = new String(connection.readBytes());
		HttpRequest httpRequestObject = createFrom(httpRequest);
		httpRequestObject.connection = connection;
		return httpRequestObject;
	}

	public static HttpRequest createFrom(String httpRequest) {
		char[] requestAsCharArray = httpRequest.toCharArray();
		String requestUrl = UrlUtils.getRequestUrl(httpRequest, requestAsCharArray);
		String context = UrlUtils.getContext(requestUrl);
		String processedUrl = UrlUtils.stripContext(context, requestUrl);

		if (processedUrl.startsWith(IFRAME_HTML)) {
			return new HttpRequest(null, null, null, null, null, null, requestUrl, null, false, false, false, false, false, false, false, false, processedUrl, context, false);
		}

		Map<String, String> httpHeaders = UrlUtils.getHttpHeaders(httpRequest, requestAsCharArray);
		Map<String, String> queryParams = UrlUtils.getQueryParams(requestUrl);
		String uid = queryParams.get(UID);
		String domain = queryParams.get(DOMAIN);
		Browser browser = Browser.fromUserAgent(httpHeaders.get(USER_AGENT));

		boolean isWebSocket = WEB_SOCKET.equals(httpHeaders.get(UPGRADE));
		boolean isRequestIFrameConnection = processedUrl.startsWith(REQUEST);
		boolean isResponseIFrameConnection = processedUrl.startsWith(RESPONSE);
		boolean isConnectionClose = CLOSE.equals(httpHeaders.get(CONNECTION));
		RequestType requestType = RequestType.fromHttpRequest(isRequestIFrameConnection, isResponseIFrameConnection, isConnectionClose);

		if (isRequestIFrameConnection || isResponseIFrameConnection) {
			return new HttpRequest(uid, domain, null, null, null, requestType, requestUrl, browser, false, false, isRequestIFrameConnection, isResponseIFrameConnection, false, false, false, false, processedUrl, context, false);
		} else if (processedUrl.startsWith(PUBLISH)) {
			String publishTopic = queryParams.get(TOPIC);
			String payload = queryParams.get(PAYLOAD);
			Payload publishPayload = UrlEncodedJsonPayload.createFrom(payload);
			return new HttpRequest(uid, domain, null, publishTopic, publishPayload, requestType, requestUrl, browser, false, false, isRequestIFrameConnection, isResponseIFrameConnection, true, false,
					false, false, processedUrl, context, false);
		} else if (processedUrl.startsWith(SUBSCRIBE_REQUEST)) {
			String[] subscriptionTopics = UrlUtils.queryValueToArray(queryParams.get(TOPIC));
			return new HttpRequest(uid, domain, subscriptionTopics, null, null, requestType, requestUrl, browser, true, false, isRequestIFrameConnection, isResponseIFrameConnection, false, false,
					false, false, processedUrl, context, false);
		} else if (processedUrl.startsWith(UN_SUBSCRIBE_REQUEST)) {
			String[] subscriptionTopics = UrlUtils.queryValueToArray(queryParams.get(TOPIC));
			return new HttpRequest(uid, domain, subscriptionTopics, null, null, requestType, requestUrl, browser, false, false, isRequestIFrameConnection, isResponseIFrameConnection, false, true,
					false, false, processedUrl, context, false);
		} else if (processedUrl.startsWith(DISCONNECT)) {
			return new HttpRequest(uid, domain, null, null, null, requestType, requestUrl, browser, false, true, isRequestIFrameConnection, isResponseIFrameConnection, false, false, false, false, processedUrl, context, false);
		} else if (processedUrl.startsWith(POLL)) {
			return new HttpRequest(uid, domain, null, null, null, requestType, requestUrl, browser, false, false, isRequestIFrameConnection, isResponseIFrameConnection, false, false, true, false, processedUrl, context, false);
		} else if (processedUrl.startsWith(CLOSE_RESPONSE)) {
			return new HttpRequest(uid, domain, null, null, null, requestType, requestUrl, browser, false, false, isRequestIFrameConnection, isResponseIFrameConnection, false, false, false, true, processedUrl, context, false);
		}

		return new HttpRequest(uid, domain, null, null, null, requestType, requestUrl, browser, false, false, isRequestIFrameConnection, isResponseIFrameConnection, false, false, false, false, processedUrl, context, isWebSocket);
	}

	enum RequestPoint {
		READING_HEADER_VAL, READING_URL, AWAITING_URL, READING_HEADER_PARAM, READING_QUERY_PARAM_KEY, READING_QUERY_PARAM_VAL, READING_HTTP_PART
	}

	public static HttpRequest newCreateFrom(String httpRequest) {
		char[] buf = httpRequest.toCharArray();
		char[] tmpBuff = new char[buf.length];
		int tmpPos = 0;
		int pos = 0;
		String headerKey = "";
		String headerVal = "";
		String queryKey = "";
		String queryVal = "";
		String requestUrl = "";
		Map<String,String> headers = new HashMap<String,String>();
		Map<String,String> queryParams = new HashMap<String,String>();

		RequestPoint currPoint = RequestPoint.AWAITING_URL;

		while (pos < buf.length) {
			if (buf[pos] == '\r' || buf[pos] == '\n') {
				if (currPoint == RequestPoint.READING_HEADER_VAL) {
					headerVal = new String(tmpBuff, 0, tmpPos);
					tmpPos = 0;
					headers.put(headerKey, headerVal);
					currPoint = RequestPoint.READING_HEADER_PARAM;
				} else if (currPoint == RequestPoint.READING_QUERY_PARAM_VAL) {
					queryVal = new String(tmpBuff, 0, tmpPos);
					tmpPos = 0;
					queryParams.put(queryKey, queryVal);
					currPoint = RequestPoint.READING_HEADER_PARAM;
				} else if (currPoint == RequestPoint.READING_URL) {
					requestUrl = new String(tmpBuff, 0, tmpPos);
					tmpPos = 0;
					currPoint = RequestPoint.READING_HEADER_PARAM;
				} else if (currPoint == RequestPoint.READING_HTTP_PART) {
					tmpPos = 0;
					currPoint = RequestPoint.READING_HEADER_PARAM;
				}
			} else if (buf[pos] == ' ') {
				if (currPoint == RequestPoint.AWAITING_URL) {
					currPoint = RequestPoint.READING_URL;
				} else if (currPoint == RequestPoint.READING_QUERY_PARAM_VAL) {
					queryVal = new String(tmpBuff, 0, tmpPos);
					tmpPos = 0;
					queryParams.put(queryKey, queryVal);
					currPoint = RequestPoint.READING_HTTP_PART;
				} else if (currPoint == RequestPoint.READING_URL) {
					requestUrl = new String(tmpBuff, 0, tmpPos);
					tmpPos = 0;
					currPoint = RequestPoint.READING_HTTP_PART;
				} else if (currPoint == RequestPoint.READING_HEADER_VAL) {
					tmpBuff[tmpPos++] = buf[pos];
				}
			} else if (buf[pos] == ':') {
				if (currPoint == RequestPoint.READING_HEADER_PARAM) {
					headerKey = new String(tmpBuff, 0, tmpPos);
					tmpPos = 0;
					currPoint = RequestPoint.READING_HEADER_VAL;
				} else if (currPoint == RequestPoint.READING_HEADER_VAL) {
					tmpBuff[tmpPos++] = buf[pos];
				}
			} else if (buf[pos] == '?') {
				if (currPoint == RequestPoint.READING_URL) {
					requestUrl = new String(tmpBuff, 0, tmpPos);
					tmpPos = 0;
					currPoint = RequestPoint.READING_QUERY_PARAM_KEY;
				} else if (currPoint == RequestPoint.READING_HEADER_VAL) {
					tmpBuff[tmpPos++] = buf[pos];
				}
			} else if (buf[pos] == '=') {
				if (currPoint == RequestPoint.READING_QUERY_PARAM_KEY) {
					queryKey = new String(tmpBuff, 0, tmpPos);
					tmpPos = 0;
					currPoint = RequestPoint.READING_QUERY_PARAM_VAL;
				} else if (currPoint == RequestPoint.READING_HEADER_VAL) {
					tmpBuff[tmpPos++] = buf[pos];
				}
			} else if (buf[pos] == '&') {
				if (currPoint == RequestPoint.READING_QUERY_PARAM_VAL) {
					queryVal = new String(tmpBuff, 0, tmpPos);
					queryParams.put(queryKey, queryVal);
					tmpPos = 0;
					currPoint = RequestPoint.READING_QUERY_PARAM_KEY;
				} else if (currPoint == RequestPoint.READING_HEADER_VAL) {
					tmpBuff[tmpPos++] = buf[pos];
				}
			} else {
				if (currPoint != RequestPoint.AWAITING_URL) {
					tmpBuff[tmpPos++] = buf[pos];
				}
			}

			pos++;
		}

		String uid = queryParams.get(UID);
		String domain = queryParams.get(DOMAIN);
		Browser browser = Browser.fromUserAgent(headers.get(USER_AGENT));
		boolean isRequestIFrameConnection = requestUrl.equals(REQUEST);
		boolean isResponseIFrameConnection = requestUrl.equals(RESPONSE);
		boolean isConnectionClose = CLOSE.equals(headers.get(CONNECTION));
		RequestType requestType = RequestType.fromHttpRequest(isRequestIFrameConnection, isResponseIFrameConnection, isConnectionClose);
		String context = UrlUtils.getContext(requestUrl);
		String processedUrl = UrlUtils.stripContext(context, requestUrl);
		
		if (isRequestIFrameConnection || isResponseIFrameConnection) {
			return new HttpRequest(uid, domain, null, null, null, requestType, requestUrl, browser, false, false, isRequestIFrameConnection, isResponseIFrameConnection, false, false, false, false, processedUrl, context, false);
		} else if (requestUrl.equals(PUBLISH)) {
			String publishTopic = queryParams.get(TOPIC);
			String payload = queryParams.get(PAYLOAD);
			Payload publishPayload = UrlEncodedJsonPayload.createFrom(payload);
			return new HttpRequest(uid, domain, null, publishTopic, publishPayload, requestType, requestUrl, browser, false, false, isRequestIFrameConnection, isResponseIFrameConnection, true, false,
					false, false, processedUrl, context, false);
		} else if (requestUrl.equals(SUBSCRIBE_REQUEST)) {
			String[] subscriptionTopics = UrlUtils.queryValueToArray(queryParams.get(TOPIC));
			return new HttpRequest(uid, domain, subscriptionTopics, null, null, requestType, requestUrl, browser, true, false, isRequestIFrameConnection, isResponseIFrameConnection, false, false,
					false, false, processedUrl, context, false);
		} else if (requestUrl.equals(UN_SUBSCRIBE_REQUEST)) {
			String[] subscriptionTopics = UrlUtils.queryValueToArray(queryParams.get(TOPIC));
			return new HttpRequest(uid, domain, subscriptionTopics, null, null, requestType, requestUrl, browser, false, false, isRequestIFrameConnection, isResponseIFrameConnection, false, true,
					false, false, processedUrl, context, false);
		} else if (requestUrl.equals(DISCONNECT)) {
			return new HttpRequest(uid, domain, null, null, null, requestType, requestUrl, browser, false, true, isRequestIFrameConnection, isResponseIFrameConnection, false, false, false, false, processedUrl, context, false);
		} else if (requestUrl.equals(POLL)) {
			return new HttpRequest(uid, domain, null, null, null, requestType, requestUrl, browser, false, false, isRequestIFrameConnection, isResponseIFrameConnection, false, false, true, false, processedUrl, context, false);
		} else if (requestUrl.equals(CLOSE_RESPONSE)) {
			return new HttpRequest(uid, domain, null, null, null, requestType, requestUrl, browser, false, false, isRequestIFrameConnection, isResponseIFrameConnection, false, false, false, true, processedUrl, context, false);
		}

		return new HttpRequest(uid, domain, null, null, null, requestType, requestUrl, browser, false, false, isRequestIFrameConnection, isResponseIFrameConnection, false, false, false, false, processedUrl, context, false);
	
	}

	public String getUid() {
		return uid;
	}

	public String getDomain() {
		return domain;
	}

	public String[] getSubscriptionTopics() {
		return subscriptionTopics;
	}

	public String getUrl() {
		return url;
	}

	public boolean isSubscription() {
		return isSubscription;
	}

	public boolean isDisconnection() {
		return isDisconnection;
	}

	public boolean isRequestIFrameConnection() {
		return isRequestIFrameConnection;
	}

	public boolean isResponseConnection() {
		return isResponseConnection;
	}

	public boolean isKeepAliveConnection() {
		return type == RequestType.KEEP_ALIVE;
	}

	public Browser getBrowser() {
		return browser;
	}

	public Connection getConnection() {
		return connection;
	}

	public boolean isIframeHtmlRequest() {
		return processedUrl.startsWith(IFRAME_HTML);
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
		return isPoll;
	}

	public boolean isCloseResponse() {
		return isCloseResponse;
	}

	public String getContext() {
		return context;
	}

	public String getProcessedUrl() {
		return processedUrl;
	}

	public boolean isWebSocket() {
		return isWebSocket;
	}
}
