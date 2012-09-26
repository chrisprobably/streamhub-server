package com.streamhub.client;

import com.streamhub.request.DirectRequest;
import com.streamhub.request.HttpRequest;
import com.streamhub.request.Request;

public class ClientFactory {
	private final ClientConnectionListener clientListener;

	public ClientFactory(ClientConnectionListener clientListener) {
		this.clientListener = clientListener;
	}

	public IStreamingClient createFrom(Request request) throws CannotCreateClientException {
		if (request.isWebSocket()) {
			return WebSocketClient.createFrom(request, clientListener);
		} else if (request instanceof HttpRequest) {
			return CometClient.createFrom(request, clientListener);
		} else if (request instanceof DirectRequest) {
			return DirectClient.createFrom(request, clientListener);
		}

		throw new CannotCreateClientException("Do not know how to create a client from request: " + request);
	}
}
