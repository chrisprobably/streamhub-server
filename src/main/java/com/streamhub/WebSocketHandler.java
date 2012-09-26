package com.streamhub;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Map;

import com.streamhub.api.SubscriptionManager;
import com.streamhub.handler.Handler;
import com.streamhub.handler.WebSocketConnection;
import com.streamhub.util.UrlUtils;
import com.streamhub.util.WebSocketUtils;

public class WebSocketHandler implements Handler {
	private static final String SEC_KEY1 = "Sec-WebSocket-Key1";
	private static final String SEC_KEY2 = "Sec-WebSocket-Key2";
	private static final String HANDSHAKE_START = "HTTP/1.1 101 Web Socket Protocol Handshake\r\n" +
					"Upgrade: WebSocket\r\n" +
					"Connection: Upgrade";
	private static final String CRLFx2 = "\r\n\r\n";
	private static final String SECURE_ORIGIN_HEADER =	"\r\nSec-WebSocket-Origin: ";
	private static final String ORIGIN_HEADER =	"\r\nWebSocket-Origin: ";
	private static final String SECURE_PROTOCOL = "\r\nSec-WebSocket-Protocol: StreamHubWS";
	private static final String HANDSHAKE_END = "\r\nWebSocket-Protocol: StreamHubWS\r\nServer: StreamHub\r\n\r\n";
	private static final String SERVER = "\r\nServer: StreamHub";
	private static final String SECURE_LOCATION_HEADER = "\r\nSec-WebSocket-Location: ws://";
	private static final String LOCATION_HEADER = "\r\nWebSocket-Location: ws://";
	private final StreamingSubscriptionManager subscriptionManager;
	private WebSocketConnection wsConnection;

	public WebSocketHandler(SubscriptionManager subscriptionManager, Handler httpHandler) {
		this.subscriptionManager = (StreamingSubscriptionManager) subscriptionManager;
	}

	public void handle(Connection connection) throws Exception {
		wsConnection = new WebSocketConnection(connection, new WebSocketMessageHandler(subscriptionManager));
		wsConnection.setReadableEventInterceptor(wsConnection);
		wsConnection.write(handshakeResponse(connection));
	}

	private ByteBuffer handshakeResponse(Connection connection) throws IOException {
		byte[] input = connection.readBytes();
		String string = new String(input);
		char[] inputAsCharArray = string.toCharArray();
		Map<String, String> headers = UrlUtils.getHttpHeaders(string, inputAsCharArray);
		String requestUrl = UrlUtils.getRequestUrl(string, inputAsCharArray);
		String origin = headers.get("Origin");
		String host = headers.get("Host");

		if (headers.keySet().contains(SEC_KEY1)) {
			return secureHandshakeResponse(requestUrl, origin, host, input, headers);
		} 
		
		return handshakeResponse(requestUrl, origin, host);
	}

	private ByteBuffer handshakeResponse(String requestUrl, String origin, String host) {
		return ByteBuffer.wrap(new StringBuilder(HANDSHAKE_START)
							.append(ORIGIN_HEADER)
							.append(origin)
							.append(LOCATION_HEADER)
							.append(host).append(requestUrl)
							.append(HANDSHAKE_END)
							.toString().getBytes());
	}

	private ByteBuffer secureHandshakeResponse(String requestUrl, String origin, String host, byte[] input, Map<String, String> headers) {
		byte[] challengeBytes = WebSocketUtils.getChallengeBytes(input);
		String key1 = headers.get(SEC_KEY1);
		String key2 = headers.get(SEC_KEY2);
		byte[] challengeResponse = WebSocketUtils.getChallengeResponse(key1, key2, challengeBytes);
		String responseHeaders =  new StringBuilder(HANDSHAKE_START)
							.append(SERVER)
							.append(SECURE_ORIGIN_HEADER)
							.append(origin)
							.append(SECURE_LOCATION_HEADER)
							.append(host).append(requestUrl)
							.append(SECURE_PROTOCOL)
							.append(CRLFx2).toString();
		
		ByteBuffer response = ByteBuffer.allocate(responseHeaders.length() + challengeResponse.length);
		response.put(responseHeaders.getBytes());
		response.put(challengeResponse);
		response.flip();
		return response;
	}
}
